from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class Point(BaseModel):
    x: float
    y: float


class Quad(BaseModel):
    top_left: Point
    top_right: Point
    bottom_right: Point
    bottom_left: Point


class BBox(BaseModel):
    left: int
    top: int
    right: int
    bottom: int


class OcrLine(BaseModel):
    text: str
    confidence: float | None = None
    bbox: BBox | None = None


class Note(BaseModel):
    title: str
    body: str


class Model1Response(BaseModel):
    job_id: str
    bbox: BBox
    quad: Quad
    content_quad: Quad
    confidence: float
    strategy: str
    warnings: list[str] = Field(default_factory=list)
    timings_ms: dict[str, float]
    artifacts: dict[str, str]


class Model2Response(BaseModel):
    job_id: str
    warnings: list[str] = Field(default_factory=list)
    timings_ms: dict[str, float]
    artifacts: dict[str, str]


class PipelineResponse(BaseModel):
    job_id: str
    model1: Model1Response
    model2: Model2Response
    ocr_text: str
    ocr_lines: list[OcrLine]
    note: Note
    warnings: list[str] = Field(default_factory=list)
    timings_ms: dict[str, float]
    artifacts: dict[str, str]
    metadata: dict[str, Any] = Field(default_factory=dict)


class OcrTextResponse(BaseModel):
    text: str
    warnings: list[str] = Field(default_factory=list)
    elapsed_ms: float = 0.0
    job_id: str | None = None
    artifacts: dict[str, str] = Field(default_factory=dict)


class ExplainRequest(BaseModel):
    text: str
    note_title: str | None = None
    source_job_id: str | None = None


class ExplainResponse(BaseModel):
    explanation: str
    warnings: list[str] = Field(default_factory=list)
    elapsed_ms: float = 0.0
    job_id: str | None = None
    artifacts: dict[str, str] = Field(default_factory=dict)
