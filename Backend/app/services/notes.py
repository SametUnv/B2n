from __future__ import annotations

from dataclasses import dataclass

from app.services.ocr import OcrResult


@dataclass
class FormattedNote:
    title: str
    body: str


class RuleBasedNoteFormatter:
    def format(self, ocr: OcrResult) -> FormattedNote:
        lines = []
        for raw_line in ocr.text.replace("\r", "\n").splitlines():
            line = " ".join(raw_line.strip().split())
            if line and (not lines or lines[-1].lower() != line.lower()):
                lines.append(line)
        if not lines:
            return FormattedNote(
                title="B2Note Notu",
                body="OCR sonucu bos. Gorseli daha net cekmeyi veya threshold degerini degistirmeyi deneyin.",
            )
        title = lines[0].rstrip(".:;,-")[:70] or "B2Note Notu"
        return FormattedNote(title=title, body="\n".join(lines))
