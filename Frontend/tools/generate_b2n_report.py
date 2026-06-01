from __future__ import annotations

import shutil
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor


ROOT = Path(r"C:\Users\Samet\Project-b2n\b2n")
TEMPLATE = next(Path(r"C:\Users\Samet\Downloads").glob("*Proje Raporu.docx"))
OUT = ROOT / "Board2Note_0.9_Proje_Raporu.docx"


def clear_document_body(doc: Document) -> None:
    body = doc._body._element
    for child in list(body):
        if child.tag == qn("w:sectPr"):
            continue
        body.remove(child)


def set_cell_shading(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_border(cell, color: str = "B7C3D0", size: str = "8") -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    borders = tc_pr.first_child_found_in("w:tcBorders")
    if borders is None:
        borders = OxmlElement("w:tcBorders")
        tc_pr.append(borders)
    for edge in ("top", "left", "bottom", "right"):
        tag = "w:" + edge
        element = borders.find(qn(tag))
        if element is None:
            element = OxmlElement(tag)
            borders.append(element)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), size)
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), color)


def set_cell_margins(cell, top=90, start=120, bottom=90, end=120) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    values = {"top": top, "start": start, "bottom": bottom, "end": end}
    for m, v in values.items():
        node = tc_mar.find(qn("w:" + m))
        if node is None:
            node = OxmlElement("w:" + m)
            tc_mar.append(node)
        node.set(qn("w:w"), str(v))
        node.set(qn("w:type"), "dxa")


def set_table_widths(table, widths_cm: list[float]) -> None:
    table.autofit = False
    for row in table.rows:
        for i, width in enumerate(widths_cm):
            if i >= len(row.cells):
                break
            row.cells[i].width = Cm(width)
            set_cell_margins(row.cells[i])


def format_table(table, header_fill="D9EAF7") -> None:
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for r_i, row in enumerate(table.rows):
        for cell in row.cells:
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_border(cell)
            set_cell_margins(cell)
            for p in cell.paragraphs:
                p.paragraph_format.space_after = Pt(2)
                p.paragraph_format.line_spacing = 1.05
                for run in p.runs:
                    run.font.name = "Times New Roman"
                    run.font.size = Pt(10)
            if r_i == 0:
                set_cell_shading(cell, header_fill)
                for p in cell.paragraphs:
                    for run in p.runs:
                        run.bold = True


def add_paragraph(doc, text: str = "", style: str | None = None, align=None, bold=False):
    p = doc.add_paragraph(style=style)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.line_spacing = 1.08
    if align is not None:
        p.alignment = align
    run = p.add_run(text)
    run.font.name = "Times New Roman"
    run.font.size = Pt(12)
    run.bold = bold
    return p


def add_body(doc, text: str):
    p = add_paragraph(doc, text)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    return p


def add_bullet(doc, text: str):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.05
    run = p.add_run(text)
    run.font.name = "Times New Roman"
    run.font.size = Pt(12)
    return p


def add_heading(doc, text: str, level: int):
    p = doc.add_heading(text, level=level)
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    for run in p.runs:
        run.font.name = "Times New Roman"
        run.font.color.rgb = RGBColor(0, 0, 0)
        run.bold = True
    return p


def add_caption(doc, text: str):
    p = add_paragraph(doc, text)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    for run in p.runs:
        run.italic = True
        run.font.size = Pt(10)
    return p


def add_placeholder(doc, title: str, description: str, height_lines: int = 7):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    set_cell_border(cell, color="94A3B8", size="10")
    set_cell_shading(cell, "F3F6FA")
    set_cell_margins(cell, top=180, start=220, bottom=180, end=220)
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(title)
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(11)
    p2 = cell.add_paragraph()
    p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r2 = p2.add_run(description)
    r2.font.name = "Times New Roman"
    r2.font.size = Pt(10)
    r2.italic = True
    for _ in range(max(0, height_lines - 2)):
        cell.add_paragraph(" ")
    add_caption(doc, title.replace("[", "").replace("]", ""))
    return table


def add_table(doc, headers: list[str], rows: list[list[str]], widths_cm: list[float] | None = None):
    table = doc.add_table(rows=1, cols=len(headers))
    for i, h in enumerate(headers):
        table.rows[0].cells[i].text = h
    for row in rows:
        cells = table.add_row().cells
        for i, val in enumerate(row):
            cells[i].text = str(val)
    if widths_cm:
        set_table_widths(table, widths_cm)
    format_table(table)
    return table


def add_cover(doc):
    for _ in range(2):
        add_paragraph(doc, "")
    for line in ["TRAKYA ÜNİVERSİTESİ", "MÜHENDİSLİK FAKÜLTESİ", "BİLGİSAYAR MÜHENDİSLİĞİ BÖLÜMÜ"]:
        p = add_paragraph(doc, line, align=WD_ALIGN_PARAGRAPH.CENTER, bold=True)
        p.runs[0].font.size = Pt(14)
    for _ in range(3):
        add_paragraph(doc, "")
    p = add_paragraph(doc, "ÖRÜNTÜ TANIMA", align=WD_ALIGN_PARAGRAPH.CENTER, bold=True)
    p.runs[0].font.size = Pt(16)
    p = add_paragraph(doc, "PROJE RAPORU", align=WD_ALIGN_PARAGRAPH.CENTER, bold=True)
    p.runs[0].font.size = Pt(16)
    add_paragraph(doc, "")
    add_paragraph(doc, "PROJE ADI:", align=WD_ALIGN_PARAGRAPH.CENTER, bold=True)
    p = add_paragraph(doc, "Board2Note: Tahta Görüntülerinden Yapay Zeka Destekli Dijital Not Üretimi", align=WD_ALIGN_PARAGRAPH.CENTER)
    p.runs[0].font.size = Pt(13)
    add_paragraph(doc, "")
    add_paragraph(doc, "PROJE EKİBİ:", align=WD_ALIGN_PARAGRAPH.CENTER, bold=True)
    add_paragraph(doc, "1221602035 - Samet Ünver", align=WD_ALIGN_PARAGRAPH.CENTER)
    add_paragraph(doc, "[Ekip üyesi bilgisi eklenecek]", align=WD_ALIGN_PARAGRAPH.CENTER)
    add_paragraph(doc, "[Ekip üyesi bilgisi eklenecek]", align=WD_ALIGN_PARAGRAPH.CENTER)
    add_paragraph(doc, "")
    add_paragraph(doc, "PROJE DANIŞMANI:", align=WD_ALIGN_PARAGRAPH.CENTER, bold=True)
    add_paragraph(doc, "Doç. Dr. Emir ÖZTÜRK", align=WD_ALIGN_PARAGRAPH.CENTER)
    for _ in range(4):
        add_paragraph(doc, "")
    add_paragraph(doc, "Edirne - 2026", align=WD_ALIGN_PARAGRAPH.CENTER)
    doc.add_page_break()


TOC = [
    ("GİRİŞ & PROJENİN TANITIMI", 1),
    ("Projenin Konusu ve Amacı", 2),
    ("Projenin Kapsamı", 2),
    ("Benzer Çalışmalar", 2),
    ("GEREKSİNİM ANALİZİ", 1),
    ("Hedef Platformlar", 2),
    ("Hedef Kitle ve Kullanıcı Rolleri", 2),
    ("Kullanılan Araç ve Teknolojiler", 2),
    ("Proje Bağımlılıkları", 2),
    ("Model Eğitimi, Veri ve Donanım Gereksinimleri", 2),
    ("Proje Kısıtları ve Stratejileri", 2),
    ("TASARIM", 1),
    ("Sistem Mimarisi ve Bileşenleri", 2),
    ("Kullanıcı Arayüzü ve Not Deneyimi", 2),
    ("Backend Pipeline Tasarımı", 2),
    ("Yapay Zeka Katmanı", 2),
    ("Veri Akışı ve Artifact Sözleşmesi", 2),
    ("Ekranlar", 2),
    ("GELİŞTİRME", 1),
    ("TTM-V2 Tahta Tespit Modelinin Geliştirilmesi", 2),
    ("YGM V4 Yazı Güçlendirme Modelinin Geliştirilmesi", 2),
    ("Model Eğitim Stratejileri ve Veri Setleri", 2),
    ("Backend API ve Artifact Yönetimi", 2),
    ("Kotlin Frontend ve Not Sistemi", 2),
    ("Projenin Çalıştırılması İçin Gereksinimler ve Kurulum Aşamaları", 2),
    ("TEST ve DOĞRULAMA", 1),
    ("Model Testleri ve Başarı Değerlendirmesi", 2),
    ("Backend ve Mobil Uygulama Testleri", 2),
    ("Manuel Testler ve İyileştirme Kararları", 2),
    ("Genel Değerlendirme", 2),
    ("SONUÇ", 1),
    ("Genel Sistem Değerlendirmesi", 2),
    ("Klasik OCR ile Farkı", 2),
    ("Genel Kazanımlar ve Geliştirme Alanları", 2),
    ("KAYNAKLAR", 1),
]


def add_toc(doc):
    p = add_paragraph(doc, "İÇİNDEKİLER", align=WD_ALIGN_PARAGRAPH.CENTER, bold=True)
    p.runs[0].font.size = Pt(14)
    add_paragraph(doc, "")
    for title, level in TOC:
        p = add_paragraph(doc, "")
        p.paragraph_format.left_indent = Cm(0.7 if level == 2 else 0)
        run = p.add_run(title)
        run.font.name = "Times New Roman"
        run.font.size = Pt(12)
        run.bold = level == 1
    doc.add_page_break()


def add_intro(doc):
    add_heading(doc, "GİRİŞ & PROJENİN TANITIMI", 1)
    add_heading(doc, "Projenin Konusu ve Amacı", 2)
    for text in [
        "Board2Note projesinin temel konusu, sınıf, amfi veya çalışma ortamında çekilen tahta görüntülerini yalnızca fotoğraf olarak saklamak yerine, yapay zeka destekli bir işlem hattı ile dijital nota dönüştürmektir. Proje; tahta alanı tespiti, perspektif düzeltme, yazı güçlendirme, yazı maskesi üretimi, OCR ve mobil not editörü katmanlarını tek ürün akışı içinde birleştirmektedir.",
        "Bu çalışmanın amacı, öğrencinin veya akademik kullanıcının tahtadaki bilgiyi hızlı biçimde yakalamasını, temizlenmiş bir görsel veya düzenlenebilir metin olarak notuna eklemesini sağlamaktır. Sistem iki farklı not türünü destekler: Yazı Notu ve Canvas Notu. Yazı notunda OCR sonucu metne eklenirken, canvas notunda Model 2 tarafından üretilen beyaz sayfa/canvas çıktısı not yüzeyine yerleştirilmektedir.",
        "Board2Note, klasik tarama uygulamalarından farklı olarak doğrudan tahta senaryosuna odaklanır. Fotoğrafın tamamı yerine tahta/projeksiyon bölgesini bulur, fiziksel kenarları ve perspektif bozulmasını azaltır, yazıları güçlendirir ve özellikle Model 2'nin üçüncü çıktısı olan text mask yapısını kullanarak yazı katmanını beyaz bir not yüzeyine taşımaya uygun hale getirir.",
    ]:
        add_body(doc, text)
    add_placeholder(
        doc,
        "[Şekil 1. Board2Note genel ürün akışı]",
        "Buraya uygulamanın ana akışını gösteren ekran görüntüsü veya şema eklenecek: fotoğraf seçimi, backend işlem hattı, text/canvas not çıktısı.",
    )
    add_heading(doc, "Projenin Kapsamı", 2)
    for text in [
        "Proje kapsamı uçtan uca bir mobil not alma sistemidir. Android/Kotlin tarafında modern bir not uygulaması, FastAPI backend tarafında PyTorch tabanlı model servisleri, model geliştirme tarafında TTM-V2 ve YGM V4 eğitim çalışmaları bulunmaktadır. Uygulama mimarisi artık API-first yaklaşıma taşınmıştır; büyük model dosyaları APK içinde tutulmak yerine backend üzerinde çalıştırılmaktadır.",
        "Sistemin ana işlem hattı; görsel yükleme, TTM-V2 ile tahta segmentasyonu, OpenCV ile kırpma ve perspektif düzeltme, YGM V4 ile restorasyon/OCR güçlendirme/text mask üretimi, PaddleOCR ile metin okuma ve mobil not editörüne sonuç aktarma adımlarından oluşur. Backend her çalıştırma için debug artifact dosyaları üretir ve frontend bu artifact URL'lerini indirerek not içeriğine dönüştürür.",
        "Kapsam ayrıca notların kalıcı saklanmasını, ders bazlı ayrıştırılmasını, arşiv/favori/çöp mantığını, ayarlanabilir backend bağlantı profillerini, Samsung Notes benzeri tam ekran text/canvas editörlerini ve taşınabilir canvas araç çubuğunu içerir.",
    ]:
        add_body(doc, text)
    add_heading(doc, "Benzer Çalışmalar", 2)
    for text in [
        "Microsoft Lens, Adobe Scan, Google Drive Scan ve Samsung Notes gibi uygulamalar görüntü yakalama, kırpma, OCR veya not düzenleme tarafında güçlü ürün deneyimleri sunmaktadır. Ancak bu uygulamaların çoğu, tahta fotoğrafından iki farklı not çıktısı üretme, yazı maskesini çıkararak beyaz canvas yüzeyine taşıma ve tahta tespit/model çıktılarıyla not editörünü birleştirme konusunda özel bir uçtan uca pipeline sunmaz.",
        "Board2Note bu noktada tarama uygulaması ile profesyonel not uygulaması arasında konumlanmaktadır. Proje, Samsung Notes benzeri canvas deneyimini korurken, ek olarak 'Tahtadan Ekle' özelliği ile yapay zeka pipeline'ını doğrudan not editörüne bağlamaktadır.",
    ]:
        add_body(doc, text)


def add_requirements(doc):
    add_heading(doc, "GEREKSİNİM ANALİZİ", 1)
    add_heading(doc, "Hedef Platformlar", 2)
    for text in [
        "Board2Note'un birincil hedef platformu Android tablet ve telefonlardır. Özellikle kalem destekli tabletlerde canvas not alma deneyimi ön plana çıkmaktadır. Backend ise Windows veya Linux üzerinde GPU destekli Python/FastAPI servisi olarak çalışacak biçimde tasarlanmıştır.",
        "Mobil uygulama Kotlin, Jetpack Compose ve Material 3 ile geliştirilmiştir. Backend tarafında PyTorch checkpoint dosyaları doğrudan servis edilir; böylece APK boyutu düşürülmüş, model güncelleme ve GPU hızlandırma süreçleri backend'e taşınmıştır.",
    ]:
        add_body(doc, text)
    add_heading(doc, "Hedef Kitle ve Kullanıcı Rolleri", 2)
    for text in [
        "Hedef kullanıcı kitlesi öğrenciler, akademisyenler, ders anlatıcıları ve sınıf ortamında tahtadaki bilgiyi hızlıca dijital nota dönüştürmek isteyen kullanıcılardır. Kullanıcı rolü temel olarak son kullanıcıdır; kullanıcı ders seçer, yazı veya canvas notu oluşturur, tahtadan görüntü ekler ve sistemin ürettiği çıktıyı notu içinde düzenler.",
        "Kullanıcının teknik bilgiye ihtiyaç duymaması tasarım gereksinimlerinden biridir. Backend bağlantısı ayarlardan seçilebilir profillerle yönetilir; ADB tüneli, Wi-Fi IP ve emulator adresleri kullanıcının bulunduğu ortama göre seçilebilir.",
    ]:
        add_body(doc, text)
    add_heading(doc, "Kullanılan Araç ve Teknolojiler", 2)
    add_heading(doc, "Kullanılan Teknolojiler ve Tercih Sebepleri", 3)
    tech_rows = [
        ["Kotlin / Jetpack Compose", "Android not uygulaması arayüzü, tam ekran editörler, drawer/navigation, canvas yüzeyi ve ayarlar ekranı."],
        ["FastAPI", "Model servislerini HTTP API üzerinden sunmak, multipart görüntü almak ve artifact URL'leri üretmek."],
        ["PyTorch / Torchvision", "TTM-V2 segmentasyon modeli ve YGM V4 multi-head enhancement modelinin çalıştırılması."],
        ["OpenCV", "Letterbox, mask post-process, contour/quad tespiti, perspektif düzeltme ve görsel artifact üretimi."],
        ["PaddleOCR", "YGM V4 OCR-enhanced çıktısı üzerinden metin okuma."],
        ["DataStore / JSON dosya saklama", "Kullanıcı ayarları, backend profilleri ve not verilerinin kalıcılığı."],
        ["ML Kit Text Recognition", "Mobil tarafta debug/fallback OCR katmanı için korunan yerel OCR desteği."],
    ]
    add_table(doc, ["Teknoloji / Araç", "Kullanım Amacı"], tech_rows, [5.0, 11.0])
    add_heading(doc, "Projede Kullanılan Yapay Zeka Geliştirme Araçları", 3)
    for text in [
        "Model geliştirme sürecinde TTM kod adı Tahta Tespit Modeli, YGM kod adı Yazı Güçlendirme Modeli olarak kullanılmıştır. Mevcut ürün akışında TTM-V2 ve YGM V4 modelleri aktif mimariyi oluşturmaktadır.",
        "TTM-V2, MobileNetV3-Large + LR-ASPP mimarisiyle tek sınıflı tahta/projeksiyon alanı segmentasyonu yapar. YGM V4 ise NAFNet-Base tabanlı multi-head yapı ile restored, OCR-enhanced ve text mask çıktıları üretir.",
    ]:
        add_body(doc, text)
    add_heading(doc, "Proje Bağımlılıkları", 2)
    dep_rows = [
        ["Backend", "fastapi, uvicorn, python-multipart, torch, torchvision, numpy, opencv-python, pillow, pydantic, paddleocr"],
        ["Android", "Compose BOM 2026.05.00, Kotlin 2.2.21, AGP 8.13.2, Navigation Compose, DataStore, ML Kit Text Recognition"],
        ["Model eğitim", "Google Colab A100, PyTorch, Torchvision, OpenCV, pycocotools, Roboflow/COCO formatı, bfloat16 AMP"],
        ["Model dosyaları", "Models/TTM-V2/stage3_polish_final_best.pt ve Models/YGM 4. Model B2N Nafnet/final_model.pt"],
    ]
    add_table(doc, ["Katman", "Bağımlılık / Açıklama"], dep_rows, [3.5, 12.5])
    add_heading(doc, "Proje Kısıtları ve Stratejileri", 2)
    for text in [
        "Tahta görüntüleri çoğunlukla eğik açı, düşük kontrast, silik yazı, parlama, gölge ve fiziksel tahta kenarı içerir. Bu nedenle tek aşamalı OCR yeterli değildir. Sistem önce tahtayı tespit eder, ardından içeriği perspektif düzeltmeyle normalize eder ve Model 2 ile yazı katmanını belirginleştirir.",
        "APK boyutu ve mobil performans da temel kısıtlardır. İlk mimaride mobil ONNX modelleri değerlendirildikten sonra ürün mimarisi API-first yapıya taşınmıştır. Bu sayede model dosyaları APK'dan çıkarılmış, GPU destekli backend üzerinde daha güçlü PyTorch checkpoint'leri kullanılabilir hale gelmiştir.",
    ]:
        add_body(doc, text)


def add_design(doc):
    add_heading(doc, "TASARIM", 1)
    add_heading(doc, "Sistem Mimarisi ve Bileşenleri", 2)
    add_body(doc, "Board2Note sistemi üç ana katmandan oluşur: Android/Kotlin frontend, FastAPI backend ve model geliştirme/eğitim katmanı. Frontend kullanıcı deneyimini ve not kalıcılığını yönetirken backend görüntü işleme ve yapay zeka inference işlemlerini yürütür. Model klasörü ise TTM-V2 ve YGM V4 checkpoint'lerini, eğitim notebook'larını ve test görsellerini içerir.")
    arch_rows = [
        ["Android Frontend", "Yazı/Canvas not editörleri, dersler, arşiv, ayarlar, backend profilleri, Tahtadan Ekle akışı."],
        ["FastAPI Backend", "POST /api/v1/pipeline, model1/model2 ayrı endpointleri, artifact servisleri ve sağlık/model metadata endpointleri."],
        ["TTM-V2", "Tahta/projeksiyon alanı segmentasyonu, mask, quad, content_quad, perspektif crop üretimi."],
        ["YGM V4", "Restored, OCR-enhanced ve text-mask çıktıları; white_canvas üretiminde mask tabanlı yazı aktarımı."],
        ["OCR ve Not Formatlama", "PaddleOCR ile OCR metni, RuleBasedNoteFormatter ile başlık/body not üretimi."],
    ]
    add_table(doc, ["Bileşen", "Görev"], arch_rows, [4.0, 12.0])
    add_placeholder(doc, "[Şekil 2. Board2Note katmanlı mimari diyagramı]", "Buraya frontend-backend-model katmanlarını gösteren mimari şema eklenecek.", 8)
    add_heading(doc, "Backend Pipeline Tasarımı", 2)
    for text in [
        "Ana endpoint POST /api/v1/pipeline olarak tasarlanmıştır. Endpoint multipart image, threshold ve run_ocr parametrelerini alır. İşlem sonunda job_id, model1 cevabı, model2 cevabı, OCR metni, not gövdesi, uyarılar, süreler ve tüm artifact URL'leri tek JSON cevabında döndürülür.",
        "Model 1 sonucunda hem dış quad hem de içe alınmış content_quad üretilir. Fiziksel tahta kenarlarının Model 2 çıktısına taşınmaması için pipeline Model 2'ye content_quad ile kırpılmış model1_perspective_crop çıktısını gönderir. Debug amaçlı model1_outer_perspective_crop ayrıca saklanır.",
    ]:
        add_body(doc, text)
    pipeline_rows = [
        ["1", "Görüntü okuma", "Upload edilen dosya RGB numpy görüntüsüne çevrilir ve original.png olarak kaydedilir."],
        ["2", "TTM-V2 segmentasyon", "640x640 letterbox, sigmoid probability, threshold ve mask temizleme uygulanır."],
        ["3", "Geometri çıkarımı", "Contour/convex hull/min-area fallback ile quad, bbox ve content_quad hesaplanır."],
        ["4", "Perspektif düzeltme", "outer_perspective_crop ve content-based perspective_crop üretilir."],
        ["5", "YGM V4 enhancement", "Restored, OCR-enhanced, text_mask ve white_canvas çıktıları alınır."],
        ["6", "OCR ve not", "run_ocr=true ise PaddleOCR ve not formatlama çalışır; false ise canvas çıktısı hızlı döner."],
    ]
    add_table(doc, ["Adım", "Aşama", "Açıklama"], pipeline_rows, [1.4, 4.2, 10.4])
    add_heading(doc, "Yapay Zeka Katmanı", 2)
    add_body(doc, "Yapay zeka katmanı iki ana modelden oluşur. Birinci model sahnedeki tahta/projeksiyon alanını ayıran segmentasyon modelidir. İkinci model ise bulunan tahta bölgesinde yazı okunabilirliğini artıran ve yazı maskesini çıkaran multi-head görüntü iyileştirme modelidir.")
    add_heading(doc, "Ekranlar", 2)
    screen_placeholders = [
        ("[Şekil 3. Ana sayfa ve not grid ekranı]", "Buraya Board2Note ana ekranı, arama alanı, son notlar ve drawer/fab görünümü eklenecek."),
        ("[Şekil 4. Yeni not tipi seçim ekranı]", "Buraya Yazı Notu / Canvas Notu seçim bottom sheet ekran görüntüsü eklenecek."),
        ("[Şekil 5. Yazı notu tam ekran editörü]", "Buraya başlık alanı, metin yüzeyi ve + > Tahtadan Ekle menüsü eklenecek."),
        ("[Şekil 6. Canvas notu editörü]", "Buraya gri dış alan, beyaz kağıt, canvas araç çubuğu ve sayfa kontrolü eklenecek."),
        ("[Şekil 7. Tahtadan Ekle inceleme ekranı]", "Buraya seçilen tahta görselinin review/nota ekle ekranı eklenecek."),
        ("[Şekil 8. Backend ayar profilleri]", "Buraya ADB tüneli, Wi-Fi IP, Emulator ve özel URL ayarları eklenecek."),
    ]
    for title, desc in screen_placeholders:
        add_placeholder(doc, title, desc, 6)


def add_development(doc):
    add_heading(doc, "GELİŞTİRME", 1)
    add_heading(doc, "TTM-V2 Tahta Tespit Modelinin Geliştirilmesi", 2)
    for text in [
        "TTM-V2, tahta/projeksiyon alanı için tek sınıflı semantik segmentasyon problemi olarak tasarlanmıştır. Model MobileNetV3-Large backbone ve LR-ASPP segmentasyon başlığı kullanır. Girdi 640x640 letterbox formatındadır. Eğitimde BCE, Dice ve Edge loss bileşenleri birlikte kullanılmıştır.",
        "Eğitim notebook'unda A100 GPU, bfloat16 autocast, channels_last memory format, RAM cache ve otomatik batch size stratejileri kullanılmıştır. Veri seti Roboflow/COCO kaynaklarından gelen tahta/projeksiyon anotasyonlarıyla birleştirilmiştir.",
    ]:
        add_body(doc, text)
    add_table(
        doc,
        ["Özellik", "Değer"],
        [
            ["Model kod adı", "TTM-V2"],
            ["Mimari", "MobileNetV3-Large + LR-ASPP"],
            ["Girdi", "640x640 letterbox"],
            ["Parametre", "Yaklaşık 3.22M"],
            ["Eğitim split", "2226 train, 278 validation, 279 test"],
            ["Checkpoint", "Models/TTM-V2/stage3_polish_final_best.pt"],
            ["Final IoU", "0.9633"],
            ["Final Dice", "0.9798"],
            ["Precision / Recall", "0.9780 / 0.9847"],
        ],
        [4.3, 11.7],
    )
    add_placeholder(doc, "[Şekil 9. TTM-V2 segmentasyon çıktı örneği]", "Buraya model1_mask, overlay, bbox_crop ve perspective_crop karşılaştırması eklenecek.", 7)
    add_heading(doc, "YGM V4 Yazı Güçlendirme Modelinin Geliştirilmesi", 2)
    for text in [
        "YGM V4, NAFNet-Base tabanlı multi-head bir görüntü iyileştirme modelidir. Model tek bir çıktı üretmek yerine üç farklı görev başlığını birlikte optimize eder: restored output, OCR-enhanced output ve text mask output. Bu yapı özellikle Board2Note'un canvas not modunda kritik hale gelmektedir.",
        "Kullanılan veri seti paired sentetik tahta veri setidir. Input klasöründe bozulmuş/gölge/parlama/blur içeren tahta görüntüleri, target_restored klasöründe temizlenmiş hedef, target_ocr klasöründe OCR için güçlendirilmiş hedef ve text_mask klasöründe yazı maskesi bulunmaktadır. Eğitim splitleri 3920 train, 144 validation ve 156 test örneğinden oluşmaktadır.",
        "Modelin üçüncü çıktısı olan text mask, projenin en ayırt edici teknik katkılarından biridir. Kotlin frontend doğrudan white_canvas çıktısını canvas nota eklemektedir; bu white_canvas çıktısı ise OCR-enhanced görüntünün text mask ile alfa karışımı kullanılarak beyaz sayfaya aktarılmasıyla üretilir. Bu nedenle mask başlığının başarısı, canvas not kalitesini doğrudan belirler.",
    ]:
        add_body(doc, text)
    add_table(
        doc,
        ["Özellik", "Değer"],
        [
            ["Model kod adı", "YGM V4"],
            ["Mimari", "NAFNet-Base Multi-Head"],
            ["Çıktı 1", "Restored tahta görüntüsü"],
            ["Çıktı 2", "OCR-enhanced görüntü"],
            ["Çıktı 3", "Text mask / yazı maskesi"],
            ["Checkpoint", "Models/YGM 4. Model B2N Nafnet/final_model.pt"],
            ["Train / Val / Test", "3920 / 144 / 156"],
            ["Final test mask IoU", "0.5827 mean; resmi global IoU 0.5614"],
            ["Final test mask Dice/F1", "0.7196 mean; resmi global Dice/F1 0.7191"],
        ],
        [4.2, 11.8],
    )
    add_placeholder(doc, "[Şekil 10. YGM V4 üç çıktı karşılaştırması]", "Buraya input, restored, OCR-enhanced ve text_mask / white_canvas çıktı görselleri eklenecek.", 7)
    add_heading(doc, "Kotlin Frontend ve Not Sistemi", 2)
    for text in [
        "Android uygulaması Board2Note 0.9 ürünleşme hedefiyle iki not türünü destekleyecek şekilde geliştirilmiştir. SavedNote modelinde NoteType alanı Text ve Canvas olarak ayrılır. Text notları OCR metnini, Canvas notları ise yüksek çözünürlüklü kağıt yüzeyi ve görsel/çizim elemanlarını taşır.",
        "Canvas editörü Samsung Notes benzeri tam ekran bir yüzey olarak düzenlenmiştir. Dış alan gri, kağıt beyazdır; kağıt artık tablet kullanımına uygun yüksek çözünürlüklü 3200x4525 A4 oranlı çalışma alanına taşınmıştır. Araç çubuğu kalem, renk, kalınlık, silgi, seçim, pan, undo/redo, görsel ekleme ve sayfa ekleme işlevlerini içerir.",
        "Backend entegrasyonunda frontend POST /api/v1/pipeline endpoint'ine görüntüyü gönderir. Yazı notunda run_ocr=true ile note.body veya ocr_text not sonuna eklenir. Canvas notunda run_ocr=false ile hızlı model akışı çalışır ve artifacts.white_canvas indirilerek canvas yüzeyine eklenir.",
    ]:
        add_body(doc, text)
    add_table(
        doc,
        ["Frontend Modülü", "Görev"],
        [
            ["BackendBoard2NotesClient", "Pipeline çağrısı, artifact URL çözümleme, white_canvas/ocr/crop/overlay indirme."],
            ["FileNoteRepository", "Text/canvas not oluşturma, JSON kalıcılık, arşiv/favori/çöp davranışı."],
            ["CanvasDocument", "Canvas sayfaları, stroke ve image element serialization, eski kayıt migration."],
            ["SettingsRepository", "Backend URL profilleri, pipeline toggle, canvas/stylus ayarları."],
            ["Board2NotesApp", "Drawer navigation, ana ekran, editör, review/capture ve ayarlar ekranları."],
        ],
        [4.8, 11.2],
    )
    add_heading(doc, "Projenin Çalıştırılması İçin Gereksinimler ve Kurulum Aşamaları", 2)
    add_heading(doc, "Backend Kurulumu", 3)
    for text in [
        "Backend klasöründe FastAPI servisi çalıştırılır. Varsayılan model yolları TTM-V2 için stage3_polish_final_best.pt, YGM V4 için final_model.pt dosyalarıdır. CUDA destekli PyTorch önerilir; B2N_REQUIRE_CUDA=1 ile GPU zorunlu tutulabilir.",
        "Örnek komut: cd Backend; $env:B2N_REQUIRE_CUDA='1'; python -m uvicorn app.main:app --host 0.0.0.0 --port 8000",
    ]:
        add_body(doc, text)
    add_heading(doc, "Android Kurulumu", 3)
    for text in [
        "Android uygulaması Frontend klasöründe Gradle ile derlenir. Compile SDK 36, min SDK 24 ve versionName 0.9.0 olarak ayarlanmıştır. Bağlı fiziksel cihaz için ADB reverse veya Wi-Fi IP backend profili kullanılabilir.",
        "Temel doğrulama komutları: .\\gradlew.bat :app:compileDebugKotlin, .\\gradlew.bat :app:testDebugUnitTest, .\\gradlew.bat :app:assembleDebug",
    ]:
        add_body(doc, text)


def add_tests(doc):
    add_heading(doc, "TEST ve DOĞRULAMA", 1)
    add_heading(doc, "Model Testleri ve Başarı Değerlendirmesi", 2)
    add_body(doc, "Model değerlendirmesi iki seviyede yapılmıştır. TTM-V2 için segmentasyon kalitesi IoU, Dice, precision ve recall metrikleriyle ölçülmüştür. YGM V4 için restored/OCR-enhanced görsel metrikleri, text mask IoU/Dice ve resmi mask head threshold sweep değerlendirmesi yapılmıştır.")
    add_table(
        doc,
        ["Model", "Veri / Test", "Ana Metrikler", "Yorum"],
        [
            ["TTM-V2", "278 val / 279 test split", "IoU 0.9633, Dice 0.9798, Precision 0.9780, Recall 0.9847", "Tahta/projeksiyon alanını çok yüksek doğrulukla ayıran hafif segmentasyon modeli."],
            ["YGM V4", "156 test / 144 val", "Test mask IoU 0.5827, Dice 0.7196, composite 0.7406", "Üç başlıklı model; canvas akışını besleyen text mask yapısı güçlü ve kullanışlı çıktı üretmektedir."],
            ["YGM V4 Mask Head", "Resmi threshold sweep", "Test AP 0.9779, ROC AUC 0.9998, best Dice 0.7558", "Üçüncü çıktı olan mask başlığı yazı piksellerini ayırmada yüksek sıralama başarısı göstermektedir."],
        ],
        [3.0, 3.2, 5.4, 4.4],
    )
    add_heading(doc, "YGM V4 Üçüncü Çıktı: Text Mask Başarısı", 3)
    add_body(doc, "Bu projede özellikle Model 2'nin üçüncü çıktısı olan text mask başlığı ürün değerini belirleyen en önemli çıktı olarak konumlanmıştır. Çünkü beyaz canvas çıktısı bu maskenin yazı bölgelerini taşıması ile oluşturulur. Resmi testte varsayılan eşikte test Global Dice/F1 0.7191 ve Global Accuracy 0.9916 elde edilmiştir. Threshold sweep sonucunda test için en iyi eşik 0.85 bulunmuş, bu eşikte IoU 0.6075 ve Dice 0.7558 değerlerine ulaşılmıştır.")
    add_table(
        doc,
        ["Split", "Default IoU", "Default Dice/F1", "Precision", "Recall", "Accuracy", "Best Threshold", "Best IoU / Dice", "AP / ROC AUC"],
        [
            ["Test", "0.5614", "0.7191", "0.6086", "0.8788", "0.9916", "0.85", "0.6075 / 0.7558", "0.9779 / 0.9998"],
            ["Validation", "0.5827", "0.7363", "0.6197", "0.9070", "0.9929", "0.90", "0.6382 / 0.7791", "0.8033 / 0.9969"],
        ],
        [1.8, 2.0, 2.1, 1.8, 1.8, 1.8, 2.0, 2.2, 2.5],
    )
    add_placeholder(doc, "[Şekil 11. YGM V4 resmi mask head test grafikleri]", "Buraya mask_metrics_plots.png, threshold sweep veya TestOutput.png görseli eklenecek.", 7)
    add_heading(doc, "Backend ve Mobil Uygulama Testleri", 2)
    for text in [
        "Backend tarafında /api/v1/pipeline çıktıları artifact dosyalarıyla doğrulanmıştır. Son örnek çalıştırmada original, model1_mask, model1_overlay, model1_perspective_crop, model2_ocr_enhanced, model2_text_mask, model2_white_canvas, ocr_text, note ve pipeline_response JSON dosyaları üretilmiştir.",
        "Son kayıtlı pipeline çalıştırmasında Model 1 inference yaklaşık 37.9 ms, Model 2 inference yaklaşık 1550 ms ve toplam işlem süresi yaklaşık 2519 ms olarak ölçülmüştür. Bu ölçüm CUDA backend üzerinde model inference akışının ürün senaryosu için kabul edilebilir olduğunu göstermektedir.",
        "Frontend tarafında compileDebugKotlin, testDebugUnitTest ve assembleDebug doğrulamaları alınmıştır. BackendBoard2NotesClient testleri artifact URL çözümlemesini, FileNoteRepository testleri eski not migration, boş not oluşturma, arşivleme ve silme davranışlarını kontrol etmektedir.",
    ]:
        add_body(doc, text)
    add_table(
        doc,
        ["Backend Artifact", "Açıklama"],
        [
            ["original.png", "Kullanıcıdan gelen ham görüntü."],
            ["model1_mask.png", "TTM-V2 binary tahta maskesi."],
            ["model1_overlay.png", "Orijinal görsel üzerinde mask/quad debug görseli."],
            ["model1_perspective_crop.png", "content_quad ile temiz tahta kırpımı; Model 2 girdisi."],
            ["model2_ocr_enhanced.png", "OCR için güçlendirilmiş görsel."],
            ["model2_text_mask.png", "Yazı bölgelerini taşıyan üçüncü model çıktısı."],
            ["model2_white_canvas.png", "Canvas notuna eklenen beyaz sayfa çıktısı."],
            ["pipeline_response.json", "Tüm metrik, uyarı ve artifact URL'lerini içeren ana cevap."],
        ],
        [5.2, 10.8],
    )
    add_placeholder(doc, "[Şekil 12. Backend artifact klasörü ve pipeline response]", "Buraya Backend/outputs/{job_id} içeriği veya API response ekran görüntüsü eklenecek.", 6)
    add_heading(doc, "Genel Değerlendirme", 2)
    for text in [
        "Test sonuçları TTM-V2'nin tahta alanını yüksek doğrulukla ayırdığını, YGM V4'ün ise yazı maskesi ve beyaz canvas üretimi için yeterli ve geliştirilebilir bir temel sunduğunu göstermektedir. Özellikle text mask başlığının AP ve ROC AUC değerleri, mask skorlarının yazı piksellerini arka plandan ayırmada güçlü bir sıralama başarısına sahip olduğunu göstermektedir.",
        "Ürün tarafında en kritik doğrulama, model çıktılarının yalnızca teknik dosya olarak kalmaması ve not editörüne anlamlı biçimde yerleşmesidir. Board2Note 0.9 sürümü bu doğrulamada yazı notu ve canvas notu ayrımını ürün akışına dahil ederek önemli bir ürünleşme aşamasına ulaşmıştır.",
    ]:
        add_body(doc, text)


def add_requirements_deep_dive(doc):
    add_heading(doc, "Model Eğitimi, Veri ve Operasyonel Gereksinimler", 2)
    add_body(doc, "Board2Note projesinde eğitim ortamı, backend servisi ve Android uygulaması aynı ürün hedefi etrafında tasarlanmıştır. Bu nedenle yalnızca çalışan bir model değil, modeli güncelleyebilen, çıktıları izleyebilen ve mobil uygulamaya güvenilir biçimde servis edebilen bir yapı hedeflenmiştir.")
    add_table(
        doc,
        ["Gereksinim Alanı", "Açıklama", "Projede Kullanım"],
        [
            ["GPU runtime", "Yüksek çözünürlüklü tensor işleme ve hızlı eğitim.", "TTM-V2 ve YGM V4 eğitimlerinde A100 odaklı mixed precision, batch optimizasyonu ve inference hızlandırması."],
            ["Model dosya yönetimi", "Checkpoint dosyalarının backend tarafından okunması.", "Model dosyaları APK içine gömülmedi; servis tarafında versiyonlanabilir hale getirildi."],
            ["Artifact kalıcılığı", "Her pipeline çalışmasının çıktılarının saklanması.", "original, model1 ve model2 çıktıları job klasöründe tutuluyor."],
            ["Bağlantı profilleri", "Emulator, ADB ve gerçek cihaz ağ koşullarının ayrılması.", "Ayarlar ekranında backend URL profilleri kullanıcıya sunuldu."],
        ],
        [4.0, 5.6, 6.4],
    )
    add_body(doc, "TTM-V2 tarafında veri tek sınıflı tahta segmentasyonu için maske odaklıdır. YGM V4 tarafında ise paired veri yapısı kullanılmıştır: input, target_restored, target_ocr ve text_mask. Bu yapı sayesinde model, aynı anda hem görüntüyü iyileştirmeyi hem de yazı bölgelerini ayırmayı öğrenmiştir.")
    add_table(
        doc,
        ["Veri Alanı", "Anlamı", "Ürün Karşılığı"],
        [
            ["input", "Bozulmuş veya perspektifi değişmiş tahta görüntüsü.", "Kullanıcının çektiği fotoğrafa benzer dağılım."],
            ["target_restored", "Temizlenmiş/restored hedef görüntü.", "Model 2'nin görsel iyileştirme başarısını ölçer."],
            ["target_ocr", "OCR için okunabilirliği artırılmış hedef.", "OCR motoru için daha kararlı girdi sağlar."],
            ["text_mask", "Yazı piksellerini gösteren maske.", "white_canvas üretiminde ana teknik sinyal olarak kullanılır."],
        ],
        [3.6, 5.8, 6.6],
    )
    add_heading(doc, "Ürün Kısıtlarından Çıkan Tasarım Kararları", 2)
    add_table(
        doc,
        ["Kısıt", "Risk", "Alınan Karar"],
        [
            ["Tahta kenarları", "Segmentasyon sonrası fiziksel kenarların çıktıya kalması.", "content_quad içe alma eklendi; Model2 temiz iç crop ile besleniyor."],
            ["APK boyutu", "Gömülü model dosyaları nedeniyle büyük paket.", "ONNX modelleri frontend'den kaldırıldı, API-first mimari seçildi."],
            ["Tablet canvas alanı", "Küçük kağıt ve fazla kalın çizim hissi.", "3200x4525 piksel kağıt ve daha düşük varsayılan kalem kalınlığı."],
            ["Gerçek cihaz API bağlantısı", "Emulator IP'sinin fiziksel cihazda çalışmaması.", "Ayarlar tarafına ADB, Wi-Fi ve custom URL profilleri eklendi."],
        ],
        [4.0, 5.3, 6.7],
    )


def add_design_deep_dive(doc):
    add_heading(doc, "Kullanıcı Deneyimi ve Akış Tasarımı", 2)
    add_body(doc, "Board2Note 0.9, model demo uygulaması görüntüsünden çıkarılıp önce not uygulaması, sonra yapay zeka destekli tahta ekleme özelliği olan bir ürün gibi tasarlanmıştır. Bu nedenle yeni not oluşturma akışı iki net seçenekten oluşur: Yazı Notu ve Canvas Notu.")
    add_table(
        doc,
        ["Akış", "Kullanıcı Aksiyonu", "Sistem Davranışı"],
        [
            ["Yazı notu", "+ > Yazı Notu", "Tam ekran metin editörü açılır; tarama sonucu OCR metni nota eklenir."],
            ["Canvas notu", "+ > Canvas Notu", "Tam ekran kağıt yüzeyi açılır; tarama sonucu white_canvas sayfaya eklenir."],
            ["Tahtadan ekle", "Editör içindeki + menüsü", "Backend pipeline çalışır ve çıktı aktif not tipine göre işlenir."],
            ["Ayar yönetimi", "Sağ üst menü / drawer", "Backend profilleri, nav tercihi ve model ayarları yönetilir."],
        ],
        [3.5, 4.5, 8.0],
    )
    add_heading(doc, "Canvas Belge Modeli", 2)
    add_body(doc, "Canvas notları yalnızca bitmap gibi ele alınmamıştır. CanvasDocument yapısı sayfa, çizgi ve görsel katmanlarını taşır. Eski çizim verileri yüklenirken yeni yüksek çözünürlüklü kağıt boyutuna ölçeklenir. Bu sayede geçmiş notlarla uyumluluk korunurken tablet uyumlu daha büyük çalışma alanı sağlanır.")
    add_table(
        doc,
        ["Alan", "Açıklama"],
        [
            ["CanvasPaper", "3200 x 4525 piksel oranlı sınırlı beyaz kağıt."],
            ["CanvasPage", "Her sayfanın kendi stroke ve image listesi."],
            ["DrawingPath", "Kalem rengi, kalınlık ve canvas koordinatları."],
            ["Inserted image", "Backend white_canvas çıktısının sayfaya yerleştirilmiş hali."],
            ["Migration", "Eski düşük çözünürlüklü canvas verilerinin yeni kağıda ölçeklenmesi."],
        ],
        [4.8, 11.2],
    )
    add_placeholder(doc, "[Şekil 13. Canvas belge modeli ve koordinat sistemi]", "Buraya kağıt alanı, gri dış alan, zoom sınırı ve canvas koordinat yapısını anlatan görsel eklenecek.", 7)
    add_heading(doc, "API Response ve Frontend Sözleşmesi", 2)
    add_table(
        doc,
        ["JSON Alanı", "Beklenen İçerik", "Frontend Kullanımı"],
        [
            ["note.body", "OCR sonrası biçimlendirilmiş not metni.", "Yazı notunda ilk tercih edilen metin."],
            ["ocr_text", "OCR motorunun ham metin çıktısı.", "note.body boşsa fallback metin."],
            ["artifacts.white_canvas", "Model2 mask tabanlı beyaz sayfa çıktısı.", "Canvas notuna görsel olarak eklenir."],
            ["artifacts.ocr_enhanced", "OCR hedefli iyileştirilmiş çıktı.", "Debug ve kalite kontrol çıktısı."],
            ["model1.content_quad", "İçe alınmış tahta köşe noktaları.", "Tahta kenarlarını azaltan geometri kararının kanıtı."],
        ],
        [4.4, 5.5, 6.1],
    )


def add_development_deep_dive(doc):
    add_heading(doc, "Model Geliştirme Sürecinin Ayrıntıları", 2)
    add_body(doc, "Model geliştirme sürecinde iki aşamalı mimari bilinçli olarak seçilmiştir. İlk model sahnedeki doğru bölgeyi bulur; ikinci model yalnızca düzeltilmiş tahta kırpımı üzerinde çalışır. Böylece enhancement modeli gereksiz arka plan bilgisiyle uğraşmak yerine yazı okunabilirliği ve maske ayrımı üzerinde odaklanır.")
    add_heading(doc, "TTM-V2 Eğitim Stratejisi", 3)
    add_table(
        doc,
        ["Başlık", "Detay"],
        [
            ["Problem", "Tek sınıflı whiteboard segmentation."],
            ["Mimari", "MobileNetV3-Large + LRASPP; yaklaşık 3.22M parametre."],
            ["Girdi", "640x640 letterbox."],
            ["Seçim kriteri", "Validation mask başarısı ve en iyi checkpoint."],
            ["Ürün katkısı", "Model2'ye verilecek temiz tahta crop'unu belirler."],
        ],
        [3.8, 12.2],
    )
    add_heading(doc, "YGM V4 Eğitim Stratejisi", 3)
    add_table(
        doc,
        ["Başlık", "Detay"],
        [
            ["Mimari", "NAFNet-Base multi-head yapı."],
            ["Stage 1", "12 epoch, 1e-4 öğrenme oranı ile ana eğitim."],
            ["Stage 2", "8 epoch, 4e-5 öğrenme oranı ile fine-tuning."],
            ["Çıktı 1", "Restored görüntü."],
            ["Çıktı 2", "OCR-enhanced görüntü."],
            ["Çıktı 3", "Text mask; white_canvas için en önemli sinyal."],
        ],
        [3.8, 12.2],
    )
    add_body(doc, "Model 2'nin üçüncü çıktısı yalnızca yardımcı bir katman olarak bırakılmamış, ürün davranışına doğrudan bağlanmıştır. Kotlin frontend canvas modunda white_canvas görselini eklediği için mask başlığının kalitesi notun görsel kalitesini doğrudan belirlemektedir.")
    add_heading(doc, "Backend Modülerliği", 2)
    add_table(
        doc,
        ["Backend Modülü", "Sorumluluk"],
        [
            ["app.main", "Endpoint orkestrasyonu ve ana pipeline."],
            ["model1_segmentation", "TTM-V2 inference, mask ve perspektif crop."],
            ["model2_enhancement", "YGM V4 inference, üç çıktı ve white_canvas üretimi."],
            ["geometry", "Quad sıralama, içe alma ve perspektif yardımcıları."],
            ["artifact_store", "Job klasörü ve artifact URL yönetimi."],
            ["config", "Model yolları, threshold ve GPU tercihleri."],
        ],
        [4.8, 11.2],
    )
    add_heading(doc, "Android Ürünleştirme Kararları", 2)
    add_table(
        doc,
        ["Karar", "Gerekçe", "Sonuç"],
        [
            ["Tam ekran editör", "Not alma sırasında ekranın maksimum kullanılması.", "Yazı ve canvas modunda daha doğal deneyim."],
            ["Stepper kaldırma", "Tarama adımları not akışından kopuk görünüyordu.", "Tahtadan Ekle editör içi aksiyona dönüştü."],
            ["Büyük canvas kağıdı", "Tablet ekranında çalışma alanı yetersizdi.", "Daha geniş, kontrollü ve profesyonel canvas."],
            ["Backend-first model", "APK boyutu ve cihaz farklarını azaltmak.", "Model yönetimi backend'e taşındı."],
        ],
        [4.0, 5.6, 6.4],
    )
    add_placeholder(doc, "[Şekil 14. Modelden nota uçtan uca akış]", "Buraya ham görüntüden text/canvas not çıktısına kadar olan akış diyagramı eklenecek.", 7)


def add_testing_deep_dive(doc):
    add_heading(doc, "Testler Sonucu Alınan Kararlar ve Kabul Senaryoları", 2)
    add_body(doc, "Test süreci yalnızca model metriklerinden ibaret değildir. Gerçek cihaz bağlantısı, API profile seçimi, canvas ergonomisi, not migration ve backend artifact doğrulaması birlikte değerlendirilmiştir.")
    add_table(
        doc,
        ["Gözlem", "Etkisi", "Çözüm"],
        [
            ["Segmentasyonda tahta kenarı kalması", "White_canvas içine fiziksel kenar girebiliyordu.", "content_quad ile iç crop üretildi."],
            ["Canvas tablette küçük kalması", "Kalem kalınlığı ve çalışma alanı sorunları.", "Kağıt büyütüldü, default kalem inceltildi."],
            ["APK'nın büyümesi", "Kurulum ve build süresi uzuyordu.", "ONNX modelleri frontend'den kaldırıldı."],
            ["Gerçek cihaz backend erişimi", "10.0.2.2 yalnızca emulatörde geçerliydi.", "Wi-Fi ve ADB profilleri eklendi."],
        ],
        [4.2, 5.3, 6.5],
    )
    add_table(
        doc,
        ["Senaryo", "Beklenen Sonuç", "Durum"],
        [
            ["Yazı notu oluşturma", "Tam ekran metin editörü açılır.", "Uygulandı"],
            ["Canvas notu oluşturma", "Tam ekran kağıt ve araç çubuğu açılır.", "Uygulandı"],
            ["Yazı notunda Tahtadan Ekle", "OCR metni mevcut notun sonuna eklenir.", "Uygulandı"],
            ["Canvas notunda Tahtadan Ekle", "white_canvas aktif sayfaya görsel olarak eklenir.", "Uygulandı"],
            ["Backend kapalıyken deneme", "Kullanıcı anlaşılır bağlantı hatası görür.", "Uygulandı"],
            ["Eski notların açılması", "Eksik noteType Text kabul edilir.", "Uygulandı"],
        ],
        [4.8, 8.0, 3.2],
    )
    add_placeholder(doc, "[Şekil 15. Gerçek cihaz kabul testi]", "Buraya fiziksel cihazda yazı notu, canvas notu, backend loading ve çıktı yerleşimi ekranları eklenecek.", 7)
    add_heading(doc, "Rapor İçin Önerilen Ekran Görüntüsü Seti", 2)
    add_table(
        doc,
        ["Görsel", "İçerik"],
        [
            ["Ürün ana ekranı", "Logo, not kartları, arama ve + aksiyonu."],
            ["Not tipi seçimi", "Yazı Notu / Canvas Notu sheet'i."],
            ["Canvas editörü", "Gri dış alan, beyaz kağıt ve araç çubuğu."],
            ["Tahtadan Ekle", "Seçilen görüntü, loading overlay ve sonuç."],
            ["Backend çıktıları", "model1 ve model2 artifact dosyaları."],
            ["Mask head testi", "YGM V4 üçüncü çıktı metrikleri ve görsel örnekleri."],
        ],
        [4.4, 11.6],
    )


def add_conclusion(doc):
    add_heading(doc, "SONUÇ", 1)
    add_heading(doc, "Genel Sistem Değerlendirmesi", 2)
    for text in [
        "Board2Note projesi, tahta görüntülerini yapay zeka destekli dijital notlara dönüştüren uçtan uca bir sistem olarak geliştirilmiştir. Proje yalnızca OCR çalışan basit bir tarama uygulaması değildir; tahta alanını bulan, perspektif düzeltme yapan, yazıyı güçlendiren, yazı maskesi çıkaran ve sonucu profesyonel bir not uygulamasına aktaran bütünleşik bir üründür.",
        "TTM-V2 modeli yüksek IoU ve Dice değerleriyle tahta alanı tespitinde güçlü sonuç vermiştir. YGM V4 modeli ise restored, OCR-enhanced ve text mask başlıklarıyla çok görevli bir yaklaşım sunmuştur. Özellikle üçüncü çıktı olan text mask, Board2Note'un canvas not modunda kullanılabilir beyaz sayfa çıktısı oluşturmasını sağlayan ana teknik bileşendir.",
    ]:
        add_body(doc, text)
    add_heading(doc, "Klasik OCR ile Farkı", 2)
    add_body(doc, "Klasik OCR yaklaşımları genellikle doğrudan ham görüntüden metin okumaya çalışır. Board2Note ise OCR öncesinde tahtayı tespit eder, fiziksel arka planı azaltır, perspektifi düzeltir, yazıları belirginleştirir ve mask tabanlı beyaz canvas çıktısı üretir. Böylece kullanıcı hem metin notu hem de görsel/canvas notu elde edebilir.")
    add_heading(doc, "Genel Kazanımlar ve Geliştirme Alanları", 2)
    for item in [
        "Kazanım: API-first mimari sayesinde APK boyutu düşürülmüş ve güçlü PyTorch modelleri backend üzerinde çalıştırılmıştır.",
        "Kazanım: Text ve Canvas not tipleriyle ürün deneyimi klasik tarama uygulamasından gerçek not uygulamasına taşınmıştır.",
        "Kazanım: TTM-V2 ve YGM V4 modelleri birlikte çalışarak ham tahta fotoğrafını düzenlenebilir ve not alınabilir çıktıya dönüştürmektedir.",
        "Geliştirme alanı: OCR tarafında el yazısı için özel handwriting OCR veya LLM tabanlı düzeltme katmanı eklenebilir.",
        "Geliştirme alanı: YGM V4 text mask threshold'u cihaz/ışık koşuluna göre dinamik seçilerek white_canvas kalitesi artırılabilir.",
        "Geliştirme alanı: Canvas tarafında lasso selection, shape tool, çok sayfalı PDF dışa aktarma ve cloud sync eklenebilir.",
    ]:
        add_bullet(doc, item)
    add_heading(doc, "KAYNAKLAR", 1)
    sources = [
        "PyTorch ve Torchvision dokümantasyonu - model eğitimi ve inference altyapısı.",
        "FastAPI dokümantasyonu - REST API ve multipart dosya yükleme altyapısı.",
        "OpenCV dokümantasyonu - görüntü işleme, contour analizi ve perspektif dönüşümü.",
        "PaddleOCR dokümantasyonu - OCR motoru ve çıktı parse yaklaşımı.",
        "NAFNet mimarisi - görüntü restorasyonu için non-linear activation free network yaklaşımı.",
        "Android Jetpack Compose ve Material 3 dokümantasyonu - modern Android UI geliştirme.",
        "Google ML Kit Text Recognition dokümantasyonu - mobil OCR/fallback katmanı.",
        "Board2Note proje kaynak kodları, eğitim notebook'ları ve backend pipeline çıktıları.",
    ]
    for src in sources:
        add_bullet(doc, src)


def configure_styles(doc):
    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Times New Roman"
    normal.font.size = Pt(12)
    normal.paragraph_format.line_spacing = 1.08
    normal.paragraph_format.space_after = Pt(6)
    for name, size in [("Heading 1", 14), ("Heading 2", 13), ("Heading 3", 12), ("Heading 4", 12)]:
        style = styles[name]
        style.font.name = "Times New Roman"
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor(0, 0, 0)
        style.paragraph_format.space_before = Pt(8 if name != "Heading 1" else 12)
        style.paragraph_format.space_after = Pt(4)


def add_footer(doc):
    section = doc.sections[0]
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Board2Note Proje Raporu")
    r.font.name = "Times New Roman"
    r.font.size = Pt(9)
    r.font.color.rgb = RGBColor(90, 90, 90)


def build():
    if OUT.exists():
        OUT.unlink()
    doc = Document()
    configure_styles(doc)
    sec = doc.sections[0]
    sec.left_margin = Cm(2.5)
    sec.right_margin = Cm(2.5)
    sec.top_margin = Cm(2.5)
    sec.bottom_margin = Cm(2.5)
    add_footer(doc)
    add_cover(doc)
    add_toc(doc)
    add_intro(doc)
    add_requirements(doc)
    add_requirements_deep_dive(doc)
    add_design(doc)
    add_design_deep_dive(doc)
    add_development(doc)
    add_development_deep_dive(doc)
    add_tests(doc)
    add_testing_deep_dive(doc)
    add_conclusion(doc)
    doc.save(str(OUT))
    print(OUT)


if __name__ == "__main__":
    build()
