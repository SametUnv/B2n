from __future__ import annotations

import re
import time
from dataclasses import dataclass, field

import requests

from app.config import Settings

# "Anlat" amacli, kaliteli Turkce ogretmen/calisma asistani system prompt'u.
SYSTEM_PROMPT = """Sen Board2Notes uygulamasinin yapay zeka calisma asistanisin. Gorevin, bir
ogrencinin tahta/ders notlarindan OCR ile cikarilmis ham metni alip, konuyu
ogrenciye ogretmen edasiyla, acik ve akici Turkce ile ANLATMAK.

Girdi, bir veya birden cok gorselden cikarilmis olabilir; parcalar '---' ile
ayrilmistir. Metinde OCR kaynakli kucuk hatalar, eksik harfler veya bozuk
semboller olabilir; baglamdan en olasi dogruyu cikar ve gerekiyorsa nazikce
duzelt. Asla "metin bozuk" diye sikayet etme; elindeki bilgiyle en faydali
anlatimi uret.

Cikti kurallari:
- Tamamen TURKCE yaz.
- Cikti mobil uygulama ekraninda dogrudan gosterilecek temiz metin olmalidir.
- Markdown, HTML, LaTeX, dollar isareti, backtick, kod blogu, **kalin** isareti,
  \\text{}, \\frac{}, \\( \\), \\[ \\] gibi teknik yazim bicimlerini KULLANMA.
- Matematik ifadelerini okunabilir sembollerle ve duz metinle yaz:
  "e = 0.5(tahmin - gercek)^2", "E_T", "dE/dW", "kismi turev" gibi.
- Su yapiyi izle:
  Ozet  -> Konunun 2-3 cumlelik genel ozeti.
  Anahtar Kavramlar  -> Onemli terim/tanimlarin madde madde aciklamasi.
  Adim Adim Anlatim  -> Konuyu mantik sirasiyla, sade ve ornekli anlat.
  Ornekler / Formuller  -> Varsa formulleri yorumla, kisa ornek coz (yoksa bu basligi atla).
  Aklinda Kalsin  -> Sinavda ise yarayacak ipuclari/kisa hatirlatmalar.
- Bilgiyi uydurma; notlarda olmayan bir konuyu zorla genisletme, ama mevcut
  konuyu anlamayi kolaylastiracak baglami ekleyebilirsin.
- Acik, ogretici ve motive edici bir ton kullan.

COK ONEMLI CIKTI KURALI: Cevabina DOGRUDAN 'Ozet' basligi ile basla. Selamlama,
giris cumlesi, dusunme adimi, taslak, Ingilizce ceviri, kontrol listesi, persona
aciklamasi veya talimat tekrari KESINLIKLE YAZMA. Ciktin yalnizca su basliklardan
olussun (sirayla):
Ozet
Anahtar Kavramlar
Adim Adim Anlatim
Ornekler / Formuller   (notlarla ilgili degilse bu basligi atla)
Aklinda Kalsin

KESINLIKLE YAZMA:
- "AI Study Assistant", "Language:", "Format:", "NO:", "Math:", "Structure:",
  "Tone:", "Constraint:" gibi prompt/talimat satirlari.
- "Weights and Values", "Error Calculation", "Total Error", "Check:" gibi
  kendi analiz, plan veya kontrol listesi satirlari.
- Final cevaptan once dusunme, taslak, dogrulama veya meta aciklama."""


@dataclass
class ExplainText:
    text: str
    warnings: list[str] = field(default_factory=list)
    elapsed_ms: float = 0.0


class GeminiExplainService:
    """Google Generative Language API (Gemma) uzerinden notlari aciklayan servis.
    Hata durumunda (ozellikle gecersiz model -> 404) net, yonlendirici bir
    RuntimeError firlatir; cagiran rota bunu kullaniciya iletir."""

    def __init__(self, settings: Settings):
        self.settings = settings

    def metadata(self) -> dict[str, object]:
        return {
            "id": "gemma_explain",
            "engine": "Google Generative Language API",
            "base_url": self.settings.gemini_base_url,
            "model": self.settings.gemini_model,
        }

    def explain(self, combined_text: str, note_title: str | None = None) -> ExplainText:
        started = time.perf_counter()
        warnings: list[str] = []
        text = (combined_text or "").strip()
        if not text:
            raise RuntimeError("Aciklanacak metin bos.")

        model = self.settings.gemini_model
        notes = text if not note_title else f"Not basligi: {note_title}\n\nNot icerigi:\n{text}"
        # Talimati ve notlari tek bir kullanici
        # mesajinda birlestiriyoruz (daha kararli ve temiz cikti).
        prompt = (
            f"{SYSTEM_PROMPT}\n\n"
            f"=== OGRENCININ NOT ICERIGI (OCR) ===\n{notes}\n\n"
            f"Simdi anlatimi dogrudan 'Ozet' basligi ile baslatarak temiz duz metin olarak uret:"
        )
        payload = {
            "contents": [{"role": "user", "parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.3,
                "maxOutputTokens": self.settings.gemini_max_output_tokens,
            },
        }
        url = f"{self.settings.gemini_base_url}/models/{model}:generateContent"
        headers = {
            "Content-Type": "application/json",
            "X-goog-api-key": self.settings.gemini_api_key,
        }
        try:
            response = requests.post(url, json=payload, headers=headers, timeout=self.settings.gemini_timeout_s)
        except requests.exceptions.ConnectionError as exc:
            raise RuntimeError(f"Google API'ye baglanilamadi. Internet baglantisini kontrol edin. Detay: {exc}") from exc

        if response.status_code == 404:
            raise RuntimeError(
                f"Gemma modeli '{model}' bulunamadi (HTTP 404). Model kimligini "
                f"B2N_GEMINI_MODEL ortam degiskeni ile gecerli bir kimlige (or. 'gemma-4-31b-it') degistirin."
            )
        if response.status_code in (401, 403):
            raise RuntimeError(
                f"Google API yetki hatasi (HTTP {response.status_code}). API anahtarini "
                f"B2N_GEMINI_API_KEY ile guncelleyin."
            )
        if response.status_code != 200:
            raise RuntimeError(f"Gemma istegi basarisiz (HTTP {response.status_code}): {response.text[:400]}")

        data = response.json()
        explanation, finish_warning = _extract_text(data)
        explanation = _clean_output(explanation)
        explanation = _clean_display_text(explanation)
        if finish_warning:
            warnings.append(finish_warning)
        if not explanation.strip():
            raise RuntimeError("Gemma bos yanit dondurdu.")
        return ExplainText(text=explanation.strip(), warnings=warnings, elapsed_ms=(time.perf_counter() - started) * 1000.0)


# Gercek anlatimin ilk basligi: 'Ozet' / 'Özet' (Markdown isareti olsa bile).
# Satir basina sabitlemeyiz: model bazen basligi onceki metne (kontrol listesi) yapistirir.
_SUMMARY_RE = re.compile(
    r"(?:^|[\n\r]|[.!?]\s*|#\s*)(?:#{0,3}[ \t]*)?(?:[*_ \t]*)?(?P<title>Özet|Ozet)\s*:?",
    re.IGNORECASE,
)
_ANSWER_SECTION_RE = re.compile(
    r"\b(?:Anahtar Kavramlar|Adim Adim Anlatim|Adım Adım Anlatım|Aklinda Kalsin|Aklında Kalsın)\b",
    re.IGNORECASE,
)


def _clean_output(text: str) -> str:
    """Gemma, nihai cevaptan once persona/talimat tekrarini veya 'dusunme' taslagini
    sizdirabilir; bu taslak '## Ozet' ifadesini (cogu kez backtick/girintili) en uste
    yerlestirir. Gercek anlatim, '## Ozet' / '## Özet' basliginin SON geçisinden itibaren
    gelir. Hicbiri bulunamazsa ham metni dondururuz."""
    cleaned = (text or "").strip()
    matches = list(_SUMMARY_RE.finditer(cleaned))
    for match in reversed(matches):
        candidate = cleaned[match.start("title"):].strip()
        if _looks_like_final_answer(candidate):
            return candidate
    if matches:
        return cleaned[matches[-1].start("title"):].strip()
    return cleaned


def _looks_like_final_answer(text: str) -> bool:
    return bool(_ANSWER_SECTION_RE.search(text[:1800]))


def _extract_text(data: dict) -> tuple[str, str | None]:
    """generateContent yanitindan ilk adayin metnini birlestirir."""
    candidates = data.get("candidates")
    if not isinstance(candidates, list) or not candidates:
        # promptFeedback bloklama bilgisi tasiyabilir.
        feedback = data.get("promptFeedback", {})
        block = feedback.get("blockReason") if isinstance(feedback, dict) else None
        if block:
            raise RuntimeError(f"Istek guvenlik filtresine takildi (blockReason={block}).")
        return "", None
    candidate = candidates[0]
    parts = (candidate.get("content") or {}).get("parts") or []
    text = "".join(
        part.get("text", "")
        for part in parts
        if isinstance(part, dict) and part.get("thought") is not True
    )
    finish_reason = candidate.get("finishReason")
    warning = None
    if finish_reason and finish_reason not in ("STOP", "MAX_TOKENS"):
        warning = f"Yanit beklenmedik sekilde sonlandi (finishReason={finish_reason})."
    elif finish_reason == "MAX_TOKENS":
        warning = "Yanit token sinirina ulasti, kisaltilmis olabilir."
    return text, warning


def _clean_math_formulas(txt: str) -> str:
    """LaTeX formüllerini daha temiz ve okunabilir düz metne dönüştürür."""
    # 1. Superscripts: ^{2} -> ², ^{T} -> ᵀ, etc.
    superscripts = {
        '0': '⁰', '1': '¹', '2': '²', '3': '³', '4': '⁴',
        '5': '⁵', '6': '⁶', '7': '⁷', '8': '⁸', '9': '⁹',
        '+': '⁺', '-': '⁻', '=': '⁼', '(': '⁽', ')': '⁾',
        'n': 'ⁿ', 'i': 'ⁱ', 'j': 'ʲ', 'T': 'ᵀ'
    }
    
    def replace_super(m):
        val = m.group(1)
        return "".join(superscripts.get(c, c) for c in val)
        
    txt = re.sub(r'\^\{([^}]+)\}', replace_super, txt)
    txt = re.sub(r'\^([0-9T])', lambda m: superscripts.get(m.group(1), m.group(1)), txt)
    
    # 2. Subscripts: _{14} -> ₁₄, etc.
    subscripts = {
        '0': '₀', '1': '₁', '2': '₂', '3': '₃', '4': '₄',
        '5': '₅', '6': '₆', '7': '₇', '8': '₈', '9': '₉',
        '+': '₊', '-': '₋', '=': '₌', '(': '₍', ')': '₎',
        'a': 'ₐ', 'e': 'ₑ', 'o': 'ₒ', 'x': 'ₓ', 'h': 'ₕ',
        'k': 'ₖ', 'l': 'ₗ', 'm': 'ₘ', 'n': 'ₙ', 'p': 'ₚ',
        's': 'ₛ', 't': 'ₜ', 'i': 'ᵢ', 'j': 'ⱼ', 'g': 'g'
    }
    
    def replace_sub(m):
        val = m.group(1)
        return "".join(subscripts.get(c, c) for c in val)
        
    txt = re.sub(r'_\{([^}]+)\}', replace_sub, txt)
    txt = re.sub(r'_([0-9aeoxhknpstigj])', lambda m: subscripts.get(m.group(1), m.group(1)), txt)

    # 3. LaTeX environments
    txt = re.sub(r'\\begin\{equation\}', '', txt)
    txt = re.sub(r'\\end\{equation\}', '', txt)
    txt = re.sub(r'\\begin\{array\}\{[a-zA-Z0-9| ]+\}', '', txt)
    txt = re.sub(r'\\end\{array\}', '', txt)
    txt = re.sub(r'&\s*', '', txt)
    
    # Parentheses & Brackets
    txt = re.sub(r'\\left\(', '(', txt)
    txt = re.sub(r'\\right\)', ')', txt)
    txt = re.sub(r'\\left\[', '[', txt)
    txt = re.sub(r'\\right\]', ']', txt)
    
    # Greek/Math symbols
    txt = re.sub(r'\\partial', '∂', txt)
    txt = re.sub(r'\\approx', '≈', txt)
    txt = re.sub(r'\\infty', '∞', txt)
    txt = re.sub(r'\\cos', 'cos', txt)
    txt = re.sub(r'\\sin', 'sin', txt)
    txt = re.sub(r'\\cdot', ' · ', txt)
    txt = re.sub(r'\\Delta', 'Δ', txt)
    txt = re.sub(r'\\times', ' × ', txt)

    # 4. Fractions: \frac{a}{b} -> a / b
    while True:
        match = re.search(r'\\frac\{([^{}]+)\}\{([^{}]+)\}', txt)
        if not match:
            break
        a, b = match.group(1), match.group(2)
        if a.strip() == '1' and b.strip() == '2':
            replacement = '0.5'
        else:
            replacement = f'{a.strip()} / {b.strip()}'
        txt = txt[:match.start()] + replacement + txt[match.end():]
    
    # 5. Remove block math $$ and inline math $
    txt = re.sub(r'\$\$(.*?)\$\$', r'\1', txt, flags=re.DOTALL)
    txt = re.sub(r'\$([^$]+)\$', r'\1', txt)
    
    # 6. Replace LaTeX newlines \\ with actual newlines
    txt = txt.replace(r'\\\\', '\n')
    txt = txt.replace(r'\\', '\n')
    
    # General cleanup (removing consecutive empty lines)
    lines = [line.strip() for line in txt.splitlines()]
    cleaned_lines = []
    for line in lines:
        if line or (cleaned_lines and cleaned_lines[-1]):
            cleaned_lines.append(line)
    return "\n".join(cleaned_lines)


def _clean_display_text(text: str) -> str:
    """Gemma cevabini frontend'in direkt gosterebilecegi sade metne cevirir.

    Model bazen prompt'a ragmen Markdown veya LaTeX dondurebilir. Android tarafinda
    Markdown/LaTeX renderer olmadigi icin burada baslik, vurgu ve formulleri okunur
    duz metne indiriyoruz.
    """
    txt = (text or "").strip()
    if not txt:
        return ""

    txt = re.sub(r"```[a-zA-Z0-9_-]*\s*", "", txt)
    txt = txt.replace("```", "")
    txt = re.sub(r"^\s{0,3}#{1,6}\s*", "", txt, flags=re.MULTILINE)
    txt = re.sub(r"\*\*([^*\n]+)\*\*", r"\1", txt)
    txt = re.sub(r"__([^_\n]+)__", r"\1", txt)
    txt = re.sub(r"`([^`]*)`", r"\1", txt)

    txt = _replace_latex_wrappers(txt)
    txt = _replace_latex_fractions(txt)
    txt = _replace_latex_scripts(txt)
    txt = _replace_latex_symbols(txt)

    txt = txt.replace(r"\(", "").replace(r"\)", "")
    txt = txt.replace(r"\[", "").replace(r"\]", "")
    txt = txt.replace("$$", "").replace("$", "")
    txt = txt.replace("{", "").replace("}", "")
    txt = txt.replace("\\", "")
    txt = _drop_prompt_leak_lines(txt)

    return _normalize_display_whitespace(txt)


def _drop_prompt_leak_lines(text: str) -> str:
    blocked = re.compile(
        r"^(?:[-*]\s*)?(?:"
        r"AI Study Assistant|Convert raw OCR|OCR text containing|Language:|Format:|NO:|Math:|"
        r"Structure:|Tone:|Constraint:|Weights and Values:|Error Calculation:|Total Error:|"
        r"Weight Updates/Gradients:|Chain Rule:|Repetitive blocks:|Specific terms:|Check:|"
        r"Did I use|Did I start|Is it in Turkish"
        r")",
        re.IGNORECASE,
    )
    kept: list[str] = []
    for line in text.splitlines():
        normalized = line.strip().strip("*_ ")
        if blocked.search(normalized):
            continue
        kept.append(line)
    return "\n".join(kept)


def _replace_latex_wrappers(text: str) -> str:
    txt = text
    wrapper_pattern = re.compile(r"\\(?:text|mathrm|mathbf|mathit|operatorname)\{([^{}]*)\}")
    while True:
        updated = wrapper_pattern.sub(r"\1", txt)
        if updated == txt:
            break
        txt = updated
    txt = re.sub(r"\\begin\{[^{}]+\}(?:\{[^{}]*\})?", "", txt)
    txt = re.sub(r"\\end\{[^{}]+\}", "", txt)
    txt = txt.replace(r"\left", "").replace(r"\right", "")
    return txt


def _replace_latex_fractions(text: str) -> str:
    txt = text
    frac_pattern = re.compile(r"\\(?:frac|dfrac|tfrac)\{([^{}]+)\}\{([^{}]+)\}")
    while True:
        match = frac_pattern.search(txt)
        if not match:
            break
        numerator = match.group(1).strip()
        denominator = match.group(2).strip()
        if numerator == "1" and denominator == "2":
            replacement = "0.5"
        else:
            replacement = f"({numerator}) / ({denominator})"
        txt = txt[: match.start()] + replacement + txt[match.end() :]
    txt = re.sub(r"\\sqrt\{([^{}]+)\}", r"sqrt(\1)", txt)
    return txt


def _replace_latex_scripts(text: str) -> str:
    superscripts = {
        "0": "\u2070", "1": "\u00b9", "2": "\u00b2", "3": "\u00b3", "4": "\u2074",
        "5": "\u2075", "6": "\u2076", "7": "\u2077", "8": "\u2078", "9": "\u2079",
        "+": "\u207a", "-": "\u207b", "=": "\u207c", "(": "\u207d", ")": "\u207e",
        "n": "\u207f", "i": "\u2071",
    }
    subscripts = {
        "0": "\u2080", "1": "\u2081", "2": "\u2082", "3": "\u2083", "4": "\u2084",
        "5": "\u2085", "6": "\u2086", "7": "\u2087", "8": "\u2088", "9": "\u2089",
        "+": "\u208a", "-": "\u208b", "=": "\u208c", "(": "\u208d", ")": "\u208e",
        "a": "\u2090", "e": "\u2091", "h": "\u2095", "i": "\u1d62", "j": "\u2c7c",
        "k": "\u2096", "l": "\u2097", "m": "\u2098", "n": "\u2099", "o": "\u2092",
        "p": "\u209a", "s": "\u209b", "t": "\u209c", "x": "\u2093",
        "T": "\u209c",
    }

    def script_replace(match: re.Match[str], table: dict[str, str]) -> str:
        return "".join(table.get(char, char) for char in match.group(1))

    txt = re.sub(r"\^\{([^{}]+)\}", lambda m: script_replace(m, superscripts), text)
    txt = re.sub(r"_\{([^{}]+)\}", lambda m: script_replace(m, subscripts), txt)
    txt = re.sub(r"\^([0-9+\-=()ni])", lambda m: superscripts.get(m.group(1), m.group(1)), txt)
    txt = re.sub(r"_([0-9aehijklmnopsxT])", lambda m: subscripts.get(m.group(1), m.group(1)), txt)
    return txt


def _replace_latex_symbols(text: str) -> str:
    replacements = {
        r"\partial": "\u2202",
        r"\nabla": "\u2207",
        r"\Delta": "\u0394",
        r"\alpha": "\u03b1",
        r"\beta": "\u03b2",
        r"\gamma": "\u03b3",
        r"\lambda": "\u03bb",
        r"\mu": "\u03bc",
        r"\sigma": "\u03c3",
        r"\theta": "\u03b8",
        r"\approx": "\u2248",
        r"\neq": "\u2260",
        r"\leq": "\u2264",
        r"\geq": "\u2265",
        r"\le": "\u2264",
        r"\ge": "\u2265",
        r"\times": "\u00d7",
        r"\cdot": "\u00b7",
        r"\rightarrow": "\u2192",
        r"\to": "\u2192",
        r"\leftarrow": "\u2190",
        r"\infty": "\u221e",
        r"\sum": "\u2211",
    }
    txt = text
    for latex, symbol in replacements.items():
        txt = txt.replace(latex, symbol)
    txt = re.sub(r"\\([A-Za-z]+)", r"\1", txt)
    return txt


def _normalize_display_whitespace(text: str) -> str:
    lines: list[str] = []
    previous_blank = False
    for raw_line in text.splitlines():
        line = re.sub(r"[ \t]+", " ", raw_line).strip()
        line = re.sub(r"\s+([,.;:!?])", r"\1", line)
        line = re.sub(r"([(\[])\s+", r"\1", line)
        line = re.sub(r"\s+([)\]])", r"\1", line)
        if not line:
            if not previous_blank and lines:
                lines.append("")
            previous_blank = True
            continue
        lines.append(line)
        previous_blank = False
    return "\n".join(lines).strip()
