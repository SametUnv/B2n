from __future__ import annotations

import base64
import io
import re
import time
from dataclasses import dataclass, field

import requests
from PIL import Image

from app.config import Settings

# Qwen 2.5 VL'a verilen OCR talimati: detayli, eksiksiz cikarim.
OCR_PROMPT = (
    "Extract all text, mathematical formulas, and written contents from this image verbatim. "
    "Maintain the layout structure and formatting as much as possible. "
    "If there are equations, preserve them using LaTeX format. "
    "Transcribe the content exactly as it is shown in the image (including non-English/Turkish text). "
    "Do not add any explanations, introductory text, or markdown code blocks. Output only the transcribed text."
)


@dataclass
class OcrText:
    text: str
    warnings: list[str] = field(default_factory=list)
    elapsed_ms: float = 0.0


class QwenVisionOcrService:
    """LM Studio uzerindeki Qwen 2.5 VL modelini OpenAI uyumlu /chat/completions ucuyla
    cagiran OCR servisi. Gorsel mumkun oldugunca yuksek cozunurlukte gonderilir; model
    baglam penceresini asarsa (HTTP 400) otomatik olarak daha kucuk cozunurlukle yeniden
    denenir. Hata durumunda bos metin + uyari dondurur (akisi bozmaz)."""

    def __init__(self, settings: Settings):
        self.settings = settings

    def metadata(self) -> dict[str, object]:
        return {
            "id": "qwen_ocr",
            "engine": "LM Studio (OpenAI uyumlu)",
            "base_url": self.settings.lmstudio_base_url,
            "model": self.settings.lmstudio_ocr_model,
        }

    def recognize_bytes(self, image_bytes: bytes, mime: str = "image/jpeg") -> OcrText:
        started = time.perf_counter()
        warnings: list[str] = []
        if not image_bytes:
            return OcrText(text="", warnings=["Bos goruntu."], elapsed_ms=0.0)

        url = f"{self.settings.lmstudio_base_url}/chat/completions"
        sides = _resize_attempts(self.settings.lmstudio_image_max_side)
        try:
            for index, side in enumerate(sides):
                b64, send_mime = _prepare_image(image_bytes, mime, side)
                payload = {
                    "model": self.settings.lmstudio_ocr_model,
                    "messages": [
                        {
                            "role": "user",
                            "content": [
                                {"type": "text", "text": OCR_PROMPT},
                                {"type": "image_url", "image_url": {"url": f"data:{send_mime};base64,{b64}"}},
                            ],
                        }
                    ],
                    "temperature": 0.1,
                    "max_tokens": self.settings.lmstudio_max_tokens,
                    "stream": False,
                }
                response = requests.post(url, json=payload, timeout=self.settings.lmstudio_timeout_s)

                if response.status_code == 200:
                    data = response.json()
                    text = _strip_code_fences(_extract_message_content(data).strip())
                    text = _sanitize_ocr_text(text)
                    if _finish_reason(data) == "length":
                        warnings.append(
                            "Cikti token sinirina ulasti ve eksik olabilir. LM Studio'da modelin baglam "
                            "boyutunu (n_ctx) artirip B2N_LMSTUDIO_MAX_TOKENS'i yukseltmeyi deneyin."
                        )
                    if not text:
                        warnings.append("OCR okunabilir metin bulamadi.")
                    return OcrText(text=text, warnings=warnings, elapsed_ms=(time.perf_counter() - started) * 1000.0)

                body = response.text[:300]
                overflow = response.status_code == 400 and any(
                    token in body.lower() for token in ("context", "exceed", "n_ctx", "too large")
                )
                if overflow and index < len(sides) - 1:
                    # Gorsel baglama sigmadi; bir sonraki (daha kucuk) cozunurlukle tekrar dene.
                    continue
                warnings.append(
                    f"LM Studio OCR HTTP {response.status_code}: {body}. "
                    f"LM Studio calisiyor mu ve '{self.settings.lmstudio_ocr_model}' modeli yuklu mu?"
                )
                return OcrText(text="", warnings=warnings, elapsed_ms=(time.perf_counter() - started) * 1000.0)

            return OcrText(text="", warnings=["OCR denemeleri basarisiz oldu."], elapsed_ms=(time.perf_counter() - started) * 1000.0)
        except requests.exceptions.ConnectionError as exc:
            warnings.append(
                f"LM Studio'ya baglanilamadi ({self.settings.lmstudio_base_url}). "
                f"LM Studio sunucusu acik mi? Detay: {exc}"
            )
            return OcrText(text="", warnings=warnings, elapsed_ms=(time.perf_counter() - started) * 1000.0)
        except Exception as exc:  # noqa: BLE001 - dayanikli davranis
            warnings.append(f"Qwen OCR basarisiz: {exc}")
            return OcrText(text="", warnings=warnings, elapsed_ms=(time.perf_counter() - started) * 1000.0)


def _resize_attempts(primary_side: int) -> list[int]:
    """Once istenen (yuksek) cozunurluk, baglam tasarsa kademeli daha kucuk cozunurlukler.
    0/negatif 'kuculme yok' anlamina gelir."""
    candidates = [int(primary_side) if primary_side and primary_side > 0 else 0, 1280, 1024, 768]
    result: list[int] = []
    for side in candidates:
        if side not in result:
            result.append(side)
    # Birincil 0 (kuculme yok) ise yine de tasma fallback'leri kalsin.
    return result


def _prepare_image(image_bytes: bytes, mime: str, max_side: int) -> tuple[str, str]:
    """En uzun kenar max_side'i asiyorsa gorseli orantili kuculup JPEG olarak yeniden kodlar;
    aksi halde orijinali KAYIPSIZ (oldugu gibi) gonderir. Hata olursa orijinali dondurur."""
    try:
        with Image.open(io.BytesIO(image_bytes)) as img:
            longest = max(img.width, img.height)
            if max_side <= 0 or longest <= max_side:
                return base64.b64encode(image_bytes).decode("ascii"), (mime or "image/jpeg")
            scale = max_side / float(longest)
            new_size = (max(1, round(img.width * scale)), max(1, round(img.height * scale)))
            resized = img.convert("RGB").resize(new_size, Image.LANCZOS)
            buffer = io.BytesIO()
            resized.save(buffer, format="JPEG", quality=95)
            return base64.b64encode(buffer.getvalue()).decode("ascii"), "image/jpeg"
    except Exception:
        return base64.b64encode(image_bytes).decode("ascii"), (mime or "image/jpeg")


# Modelin takilip urettigi 8+ ayni karakterlik dizileri (or. '????????...') tek karaktere indir.
_REPEAT_RUN_RE = re.compile(r"(.)\1{7,}", re.DOTALL)


def _sanitize_ocr_text(text: str) -> str:
    cleaned = _REPEAT_RUN_RE.sub(r"\1", text).strip()
    # Hic harf/rakam kalmadiysa (sadece tekrarli noktalama vb.) anlamli metin yok say.
    if cleaned and not any(ch.isalnum() for ch in cleaned):
        return ""
    return cleaned


def _finish_reason(data: dict):
    try:
        return data["choices"][0].get("finish_reason")
    except (KeyError, IndexError, TypeError):
        return None


def _extract_message_content(data: dict) -> str:
    try:
        message = data["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError):
        return ""
    if isinstance(message, str):
        return message
    if isinstance(message, list):
        parts = []
        for item in message:
            if isinstance(item, dict) and isinstance(item.get("text"), str):
                parts.append(item["text"])
            elif isinstance(item, str):
                parts.append(item)
        return "".join(parts)
    return str(message) if message is not None else ""


def _strip_code_fences(text: str) -> str:
    stripped = text.strip()
    if stripped.startswith("```"):
        lines = stripped.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip().startswith("```"):
            lines = lines[:-1]
        return "\n".join(lines).strip()
    return text
