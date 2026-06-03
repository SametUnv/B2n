from __future__ import annotations

import asyncio
import json
import time
from typing import Any

import cv2
import numpy as np
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

from app.config import load_settings, torch_runtime_info
from app.schemas import (
    BBox,
    ExplainRequest,
    ExplainResponse,
    Model1Response,
    Model2Response,
    Note,
    OcrLine,
    OcrTextResponse,
    PipelineResponse,
    Point,
    Quad,
)
from app.services.artifacts import ArtifactStore
from app.services.image_io import read_upload_rgb
from app.services.geometry import draw_quad, inset_quad, order_points, warp_perspective
from app.services.gemini_explain import GeminiExplainService
from app.services.model1_segmentation import Model1Result, Model1SegmentationService
from app.services.model2_enhancement import Model2EnhancementService, Model2Result
from app.services.notes import FormattedNote, RuleBasedNoteFormatter
from app.services.ocr import OcrResult as InternalOcrResult
from app.services.ocr import PaddleOcrService
from app.services.qwen_ocr import QwenVisionOcrService

settings = load_settings()
artifacts = ArtifactStore(settings.outputs_dir)
model1_service = Model1SegmentationService(settings)
model2_service = Model2EnhancementService(settings)
ocr_service = PaddleOcrService(settings)
note_formatter = RuleBasedNoteFormatter()
qwen_ocr_service = QwenVisionOcrService(settings)
gemini_service = GeminiExplainService(settings)

app = FastAPI(title="Board2Notes Backend", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "device": settings.device,
        "outputs_dir": str(settings.outputs_dir),
        "model1_exists": settings.model1_path.exists(),
        "model2_exists": settings.model2_path.exists(),
        "model1_path": str(settings.model1_path),
        "model2_path": str(settings.model2_path),
        "require_cuda": settings.require_cuda,
        "use_amp": settings.use_amp,
        "board_content_margin_ratio": settings.board_content_margin_ratio,
        "runtime": torch_runtime_info(settings.device),
    }


@app.get("/api/v1/models")
def models() -> dict[str, Any]:
    return {
        "device": settings.device,
        "use_amp": settings.use_amp,
        "models": [
            model1_service.metadata(),
            model2_service.metadata(),
            {"id": "ocr", "engine": "PaddleOCR", "lang": settings.ocr_lang, "lazy_loaded": True},
            qwen_ocr_service.metadata(),
            gemini_service.metadata(),
        ],
        "postprocess": {
            "board_content_margin_ratio": settings.board_content_margin_ratio,
            "perspective_crop": "content_quad",
            "outer_perspective_crop": "debug_artifact",
        },
    }


@app.post("/api/v1/warmup")
def warmup() -> dict[str, Any]:
    timings = {}
    timings.update(model1_service.warmup())
    timings.update(model2_service.warmup())
    rng = np.random.default_rng(20260530)
    for index in range(2):
        started = time.perf_counter()
        dummy = rng.integers(180, 256, size=(720, 960, 3), dtype=np.uint8)
        dummy[90:630, 100:860] = rng.integers(225, 256, size=(540, 760, 3), dtype=np.uint8)
        m1 = model1_service.detect(dummy, threshold=0.5)
        direct_crop = rng.integers(180, 256, size=(620, 920, 3), dtype=np.uint8)
        m2 = model2_service.enhance(direct_crop)
        timings[f"pipeline_path_warmup_pass_{index + 1}_ms"] = (time.perf_counter() - started) * 1000.0
        timings[f"pipeline_path_pass_{index + 1}_model1_inference_ms"] = m1.timings_ms.get("inference_ms", 0.0)
        timings[f"pipeline_path_pass_{index + 1}_model2_inference_ms"] = m2.timings_ms.get("inference_ms", 0.0)
    return {
        "status": "ok",
        "device": settings.device,
        "use_amp": settings.use_amp,
        "timings_ms": timings,
        "runtime": torch_runtime_info(settings.device),
    }


@app.post("/api/v1/model1/detect", response_model=Model1Response)
async def detect_model1(image: UploadFile = File(...), threshold: float = Form(0.5)) -> Model1Response:
    try:
        image_rgb = await read_upload_rgb(image)
        job_id, job_dir = artifacts.create_job()
        result = model1_service.detect(image_rgb, threshold)
        artifact_urls = save_model1_artifacts(job_id, job_dir, result)
        artifacts.save_json(job_id, job_dir, "model1_response", {"job_id": job_id, "artifacts": artifact_urls})
        return model1_response(job_id, result, artifact_urls)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/v1/model2/enhance", response_model=Model2Response)
async def enhance_model2(image: UploadFile = File(...)) -> Model2Response:
    try:
        image_rgb = await read_upload_rgb(image)
        job_id, job_dir = artifacts.create_job()
        result = model2_service.enhance(image_rgb)
        artifact_urls = save_model2_artifacts(job_id, job_dir, result)
        artifacts.save_json(job_id, job_dir, "model2_response", {"job_id": job_id, "artifacts": artifact_urls})
        return model2_response(job_id, result, artifact_urls)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/v1/pipeline/from-quad", response_model=PipelineResponse)
async def pipeline_from_quad(
    image: UploadFile = File(...),
    quad: str | None = Form(None),
    top_left_x: float | None = Form(None),
    top_left_y: float | None = Form(None),
    top_right_x: float | None = Form(None),
    top_right_y: float | None = Form(None),
    bottom_right_x: float | None = Form(None),
    bottom_right_y: float | None = Form(None),
    bottom_left_x: float | None = Form(None),
    bottom_left_y: float | None = Form(None),
    run_ocr: bool = Form(True),
) -> PipelineResponse:
    started = time.perf_counter()
    try:
        image_rgb = await read_upload_rgb(image)
        approved_quad = parse_quad_form(
            quad,
            image_rgb.shape[1],
            image_rgb.shape[0],
            field_points=[
                top_left_x,
                top_left_y,
                top_right_x,
                top_right_y,
                bottom_right_x,
                bottom_right_y,
                bottom_left_x,
                bottom_left_y,
            ],
        )
        content_quad = inset_quad(approved_quad, settings.board_content_margin_ratio)

        job_id, job_dir = artifacts.create_job()
        original_url = artifacts.save_image(job_id, job_dir, "original", image_rgb)

        outer_perspective_crop = warp_perspective(
            image_rgb,
            approved_quad,
            max_output_side=settings.perspective_max_output_side,
        )
        perspective_crop = warp_perspective(
            image_rgb,
            content_quad,
            max_output_side=settings.perspective_max_output_side,
        )
        overlay = draw_quad(image_rgb, approved_quad)
        m1_artifacts = {
            "overlay": artifacts.save_image(job_id, job_dir, "model1_overlay", overlay),
            "outer_perspective_crop": artifacts.save_image(
                job_id,
                job_dir,
                "model1_outer_perspective_crop",
                outer_perspective_crop,
            ),
            "perspective_crop": artifacts.save_image(job_id, job_dir, "model1_perspective_crop", perspective_crop),
        }

        m2 = model2_service.enhance(perspective_crop)
        m2_artifacts = save_model2_artifacts(job_id, job_dir, m2)

        if run_ocr:
            ocr = ocr_service.recognize(m2.ocr_enhanced)
            note = note_formatter.format(ocr)
        else:
            ocr = InternalOcrResult(text="", lines=[], warnings=[], elapsed_ms=0.0)
            note = FormattedNote(
                title="OCR kapali",
                body="OCR bu calistirmada kapaliydi. Model ciktilari ve not canvas uretildi.",
            )
        ocr_text_url = artifacts.save_text(job_id, job_dir, "ocr_text", ocr.text)
        note_url = artifacts.save_text(job_id, job_dir, "note", f"{note.title}\n\n{note.body}")

        warnings = m2.warnings + ocr.warnings
        timings = {f"model2_{k}": v for k, v in m2.timings_ms.items()}
        timings["ocr_ms"] = ocr.elapsed_ms
        timings["total_ms"] = (time.perf_counter() - started) * 1000.0

        model1 = Model1Response(
            job_id=job_id,
            bbox=bbox_model(bbox_from_quad(approved_quad)),
            quad=quad_model(approved_quad),
            content_quad=quad_model(content_quad),
            confidence=1.0,
            strategy="user_approved_quad",
            warnings=[],
            timings_ms={"manual_quad_ms": 0.0},
            artifacts=m1_artifacts,
        )
        all_artifacts = {"original": original_url, **m1_artifacts, **m2_artifacts, "ocr_text": ocr_text_url, "note": note_url}
        response = PipelineResponse(
            job_id=job_id,
            model1=model1,
            model2=model2_response(job_id, m2, m2_artifacts),
            ocr_text=ocr.text,
            ocr_lines=[
                OcrLine(text=line.text, confidence=line.confidence, bbox=bbox_model(line.bbox))
                for line in ocr.lines
            ],
            note=Note(title=note.title, body=note.body),
            warnings=warnings,
            timings_ms=timings,
            artifacts=all_artifacts,
            metadata={
                "device": settings.device,
                "run_ocr": run_ocr,
                "board_content_margin_ratio": settings.board_content_margin_ratio,
                "quad_source": "frontend_user_approved",
            },
        )
        artifacts.save_json(job_id, job_dir, "pipeline_from_quad_response", response.model_dump())
        return response
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/v1/pipeline", response_model=PipelineResponse)
async def pipeline(
    image: UploadFile = File(...),
    threshold: float = Form(0.5),
    run_ocr: bool = Form(True),
) -> PipelineResponse:
    started = time.perf_counter()
    try:
        image_rgb = await read_upload_rgb(image)
        job_id, job_dir = artifacts.create_job()
        original_url = artifacts.save_image(job_id, job_dir, "original", image_rgb)

        m1 = model1_service.detect(image_rgb, threshold)
        m1_artifacts = save_model1_artifacts(job_id, job_dir, m1)

        m2 = model2_service.enhance(m1.perspective_crop)
        m2_artifacts = save_model2_artifacts(job_id, job_dir, m2)

        if run_ocr:
            ocr = ocr_service.recognize(m2.ocr_enhanced)
            note = note_formatter.format(ocr)
        else:
            ocr = InternalOcrResult(text="", lines=[], warnings=[], elapsed_ms=0.0)
            note = FormattedNote(
                title="OCR kapali",
                body="OCR bu calistirmada kapaliydi. Model ciktilari ve not canvas uretildi.",
            )
        ocr_text_url = artifacts.save_text(job_id, job_dir, "ocr_text", ocr.text)
        note_url = artifacts.save_text(job_id, job_dir, "note", f"{note.title}\n\n{note.body}")

        warnings = m1.warnings + m2.warnings + ocr.warnings
        timings = dict(m1.timings_ms)
        timings.update({f"model2_{k}": v for k, v in m2.timings_ms.items()})
        timings["ocr_ms"] = ocr.elapsed_ms
        timings["total_ms"] = (time.perf_counter() - started) * 1000.0

        all_artifacts = {"original": original_url, **m1_artifacts, **m2_artifacts, "ocr_text": ocr_text_url, "note": note_url}
        response = PipelineResponse(
            job_id=job_id,
            model1=model1_response(job_id, m1, m1_artifacts),
            model2=model2_response(job_id, m2, m2_artifacts),
            ocr_text=ocr.text,
            ocr_lines=[
                OcrLine(text=line.text, confidence=line.confidence, bbox=bbox_model(line.bbox))
                for line in ocr.lines
            ],
            note=Note(title=note.title, body=note.body),
            warnings=warnings,
            timings_ms=timings,
            artifacts=all_artifacts,
            metadata={
                "threshold": threshold,
                "device": settings.device,
                "run_ocr": run_ocr,
                "board_content_margin_ratio": settings.board_content_margin_ratio,
            },
        )
        artifacts.save_json(job_id, job_dir, "pipeline_response", response.model_dump())
        return response
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/v1/artifacts/{job_id}/{artifact_name}")
def get_artifact(job_id: str, artifact_name: str) -> FileResponse:
    try:
        path, media_type = artifacts.resolve(job_id, artifact_name)
    except FileNotFoundError as exc:
        raise HTTPException(status_code=404, detail="Artifact not found.") from exc
    return FileResponse(path, media_type=media_type, filename=path.name)


@app.post("/api/v1/ocr/qwen", response_model=OcrTextResponse)
async def ocr_qwen(image: UploadFile = File(...), job_id: str | None = Form(None)) -> OcrTextResponse:
    data = await image.read()
    mime = image.content_type or "image/jpeg"
    resolved_job_id, job_dir = artifact_job(job_id)
    artifact_urls: dict[str, str] = {}
    image_rgb = decode_image_bytes_rgb(data)
    artifact_urls["qwen_input"] = artifacts.save_image(resolved_job_id, job_dir, "qwen_input", image_rgb)
    result = await asyncio.to_thread(qwen_ocr_service.recognize_bytes, data, mime)
    artifact_urls["qwen_ocr_text"] = artifacts.save_text(resolved_job_id, job_dir, "qwen_ocr_text", result.text)
    artifact_urls["qwen_ocr_response"] = artifacts.save_json(
        resolved_job_id,
        job_dir,
        "qwen_ocr_response",
        {
            "job_id": resolved_job_id,
            "source": {
                "filename": image.filename,
                "mime": mime,
                "bytes": len(data),
                "linked_pipeline_job": bool(job_id),
            },
            "service": qwen_ocr_service.metadata(),
            "text": result.text,
            "warnings": result.warnings,
            "elapsed_ms": result.elapsed_ms,
            "artifacts": artifact_urls,
        },
    )
    return OcrTextResponse(
        text=result.text,
        warnings=result.warnings,
        elapsed_ms=result.elapsed_ms,
        job_id=resolved_job_id,
        artifacts=artifact_urls,
    )


@app.post("/api/v1/explain", response_model=ExplainResponse)
async def explain(req: ExplainRequest) -> ExplainResponse:
    if not req.text.strip():
        raise HTTPException(status_code=400, detail="Aciklanacak metin bos.")
    resolved_job_id, job_dir = artifact_job(req.source_job_id)
    artifact_urls: dict[str, str] = {
        "gemini_input_text": artifacts.save_text(resolved_job_id, job_dir, "gemini_input_text", req.text),
    }
    try:
        result = await asyncio.to_thread(gemini_service.explain, req.text, req.note_title)
    except Exception as exc:
        artifact_urls["gemini_error"] = artifacts.save_json(
            resolved_job_id,
            job_dir,
            "gemini_error",
            {
                "job_id": resolved_job_id,
                "source_job_id": req.source_job_id,
                "note_title": req.note_title,
                "service": gemini_service.metadata(),
                "input_text": req.text,
                "error": str(exc),
                "artifacts": artifact_urls,
            },
        )
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    artifact_urls["gemini_explanation"] = artifacts.save_text(
        resolved_job_id,
        job_dir,
        "gemini_explanation",
        result.text,
    )
    artifact_urls["gemini_response"] = artifacts.save_json(
        resolved_job_id,
        job_dir,
        "gemini_response",
        {
            "job_id": resolved_job_id,
            "source_job_id": req.source_job_id,
            "note_title": req.note_title,
            "service": gemini_service.metadata(),
            "input_text": req.text,
            "explanation": result.text,
            "warnings": result.warnings,
            "elapsed_ms": result.elapsed_ms,
            "artifacts": artifact_urls,
        },
    )
    return ExplainResponse(
        explanation=result.text,
        warnings=result.warnings,
        elapsed_ms=result.elapsed_ms,
        job_id=resolved_job_id,
        artifacts=artifact_urls,
    )


def artifact_job(job_id: str | None) -> tuple[str, Any]:
    cleaned = (job_id or "").strip()
    if not cleaned:
        return artifacts.create_job()
    try:
        return artifacts.get_or_create_job(cleaned)
    except FileNotFoundError as exc:
        raise HTTPException(status_code=400, detail="Gecersiz artifact job_id.") from exc


def decode_image_bytes_rgb(data: bytes) -> np.ndarray:
    if not data:
        raise HTTPException(status_code=400, detail="Yuklenen gorsel bos.")
    buffer = np.frombuffer(data, dtype=np.uint8)
    bgr = cv2.imdecode(buffer, cv2.IMREAD_COLOR)
    if bgr is None:
        raise HTTPException(status_code=400, detail="Yuklenen dosya gorsel olarak okunamadi.")
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def save_model1_artifacts(job_id: str, job_dir, result: Model1Result) -> dict[str, str]:
    return {
        "mask": artifacts.save_image(job_id, job_dir, "model1_mask", result.mask_image),
        "mask_probability": artifacts.save_image(job_id, job_dir, "model1_mask_probability", result.mask_probability * 255.0),
        "overlay": artifacts.save_image(job_id, job_dir, "model1_overlay", result.overlay),
        "bbox_crop": artifacts.save_image(job_id, job_dir, "model1_bbox_crop", result.bbox_crop),
        "outer_perspective_crop": artifacts.save_image(
            job_id,
            job_dir,
            "model1_outer_perspective_crop",
            result.outer_perspective_crop,
        ),
        "perspective_crop": artifacts.save_image(job_id, job_dir, "model1_perspective_crop", result.perspective_crop),
    }


def save_model2_artifacts(job_id: str, job_dir, result: Model2Result) -> dict[str, str]:
    return {
        "model2_input": artifacts.save_image(job_id, job_dir, "model2_input", result.model_input),
        "restored": artifacts.save_image(job_id, job_dir, "model2_restored", result.restored),
        "ocr_enhanced": artifacts.save_image(job_id, job_dir, "model2_ocr_enhanced", result.ocr_enhanced),
        "text_mask": artifacts.save_image(job_id, job_dir, "model2_text_mask", result.text_mask_image),
        "white_canvas": artifacts.save_image(job_id, job_dir, "model2_white_canvas", result.white_canvas),
    }


def model1_response(job_id: str, result: Model1Result, artifact_urls: dict[str, str]) -> Model1Response:
    return Model1Response(
        job_id=job_id,
        bbox=bbox_model(result.bbox),
        quad=quad_model(result.quad),
        content_quad=quad_model(result.content_quad),
        confidence=result.confidence,
        strategy=result.strategy,
        warnings=result.warnings,
        timings_ms=result.timings_ms,
        artifacts=artifact_urls,
    )


def model2_response(job_id: str, result: Model2Result, artifact_urls: dict[str, str]) -> Model2Response:
    return Model2Response(
        job_id=job_id,
        warnings=result.warnings,
        timings_ms=result.timings_ms,
        artifacts=artifact_urls,
    )


def bbox_model(bbox: tuple[int, int, int, int] | None) -> BBox | None:
    if bbox is None:
        return None
    left, top, right, bottom = bbox
    return BBox(left=left, top=top, right=right, bottom=bottom)


def parse_quad_form(
    raw: str | None,
    image_width: int,
    image_height: int,
    field_points: list[float | None] | None = None,
) -> np.ndarray:
    if field_points is not None and all(value is not None for value in field_points):
        values = [float(value) for value in field_points]
        points = [
            [values[0], values[1]],
            [values[2], values[3]],
            [values[4], values[5]],
            [values[6], values[7]],
        ]
    else:
        if raw is None or not raw.strip():
            raise ValueError("Quad form field is empty.")
        raw = raw.strip()
        payload = None
        candidates = [
            raw,
            raw.strip('"'),
            raw.replace('\\"', '"'),
            raw.replace("\\\\\"", '"'),
        ]
        try:
            candidates.append(raw.encode("utf-8").decode("unicode_escape"))
        except Exception:
            pass
        last_error: Exception | None = None
        for candidate in candidates:
            try:
                payload = json.loads(candidate)
                break
            except json.JSONDecodeError as exc:
                last_error = exc
        if payload is None:
            raise ValueError(f"Invalid quad JSON: {last_error}")
        if isinstance(payload, str):
            payload = json.loads(payload)
        if isinstance(payload, dict):
            keys = ("top_left", "top_right", "bottom_right", "bottom_left")
            points = [[payload[key]["x"], payload[key]["y"]] for key in keys]
        else:
            points = payload
    quad = np.asarray(points, dtype=np.float32).reshape(4, 2)
    quad[:, 0] = np.clip(quad[:, 0], 0, max(1, image_width - 1))
    quad[:, 1] = np.clip(quad[:, 1], 0, max(1, image_height - 1))
    return order_points(quad)


def bbox_from_quad(quad: np.ndarray) -> tuple[int, int, int, int]:
    left = int(np.floor(float(np.min(quad[:, 0]))))
    top = int(np.floor(float(np.min(quad[:, 1]))))
    right = int(np.ceil(float(np.max(quad[:, 0]))))
    bottom = int(np.ceil(float(np.max(quad[:, 1]))))
    return left, top, right, bottom


def quad_model(quad) -> Quad:
    return Quad(
        top_left=Point(x=float(quad[0][0]), y=float(quad[0][1])),
        top_right=Point(x=float(quad[1][0]), y=float(quad[1][1])),
        bottom_right=Point(x=float(quad[2][0]), y=float(quad[2][1])),
        bottom_left=Point(x=float(quad[3][0]), y=float(quad[3][1])),
    )
