package com.board2notes.app.data.mock

/**
 * Centralized fake data. When the real API arrives, this file simply
 * stops being referenced — nothing else needs to know it existed.
 */
object MockData {

    const val OCR_TEXT: String =
        "Bu derste türev kavramı, limit ilişkisi ve temel türev alma kuralları anlatılmıştır.\n\n" +
            "1) Türev, bir fonksiyonun anlık değişim hızını ifade eder.\n" +
            "2) f'(x) = lim (h→0) [f(x+h) - f(x)] / h\n" +
            "3) Sabit kuralı:  (c)' = 0\n" +
            "4) Kuvvet kuralı:  (x^n)' = n·x^(n-1)\n" +
            "5) Toplam kuralı:  (f + g)' = f' + g'\n\n" +
            "Not: Limit tanımı türevin temelidir; süreklilik türevlenebilirliğin ön koşuludur."

    const val VISUAL_NOTE_TITLE: String = "Türev — Ders Notu"

    val VISUAL_NOTE_LINES: List<String> = listOf(
        "Konu: Türev Kavramı",
        "• Türev = anlık değişim hızı",
        "• Limit ile tanımlanır:  f'(x) = lim(h→0) Δf/h",
        "• Sabit kuralı:  (c)' = 0",
        "• Kuvvet kuralı:  (xⁿ)' = n·xⁿ⁻¹",
        "• Toplam kuralı:  (f+g)' = f' + g'",
        "Hatırlatma: Süreklilik, türevlenebilirliğin ön koşuludur."
    )
}
