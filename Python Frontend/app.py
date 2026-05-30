from __future__ import annotations

import json
import os
import threading
import time
import webbrowser
from io import BytesIO
from pathlib import Path
from tkinter import BOTH, END, LEFT, RIGHT, TOP, X, filedialog, messagebox
import tkinter as tk
from tkinter import ttk
from urllib.parse import urljoin

import requests
from PIL import Image, ImageDraw, ImageFilter, ImageTk


APP_DIR = Path(__file__).resolve().parent
REPO_ROOT = APP_DIR.parent
NOTES_DIR = APP_DIR / "notes"
BACKEND_OUTPUTS = REPO_ROOT / "Backend" / "outputs"
NOTE_PAGE_WIDTH = 1600
NOTE_PAGE_HEIGHT = 2263
NOTE_PAGE_MARGIN = 120
TEXT_MASK_THRESHOLD = 105


class Board2NotesTester(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Board2Notes Python Test UI")
        self.geometry("1280x820")
        self.minsize(980, 680)

        self.image_path: Path | None = None
        self.response: dict | None = None
        self.photo_refs: dict[str, ImageTk.PhotoImage] = {}
        self.note_canvas_image: Image.Image | None = None
        self.note_canvas_original: Image.Image | None = None
        self.note_canvas_photo: ImageTk.PhotoImage | None = None
        self.note_canvas_zoom = 1.0
        self.note_canvas_offset = (0, 0)
        self.note_canvas_display_size = (1, 1)
        self.note_canvas_last_point: tuple[int, int] | None = None
        self.note_crop_start: tuple[int, int] | None = None
        self.note_crop_rect_id: int | None = None
        self.note_canvas_mode = tk.StringVar(value="draw")
        self.pen_size = tk.IntVar(value=4)
        self.run_ocr = tk.BooleanVar(value=False)

        self.base_url = tk.StringVar(value="http://127.0.0.1:8000")
        self.threshold = tk.DoubleVar(value=0.50)
        self.status = tk.StringVar(value="Hazir")

        self._build_ui()

    def _build_ui(self):
        toolbar = ttk.Frame(self, padding=10)
        toolbar.pack(fill=X)

        ttk.Label(toolbar, text="FastAPI URL").pack(side=LEFT)
        ttk.Entry(toolbar, textvariable=self.base_url, width=32).pack(side=LEFT, padx=(6, 12))
        ttk.Button(toolbar, text="Gorsel Sec", command=self.select_image).pack(side=LEFT)
        ttk.Label(toolbar, text="Threshold").pack(side=LEFT, padx=(16, 4))
        ttk.Scale(toolbar, from_=0.35, to=0.75, variable=self.threshold, orient="horizontal", length=160).pack(side=LEFT)
        self.threshold_label = ttk.Label(toolbar, text="0.50", width=5)
        self.threshold_label.pack(side=LEFT, padx=(4, 12))
        self.threshold.trace_add("write", lambda *_: self.threshold_label.configure(text=f"{self.threshold.get():.2f}"))
        ttk.Checkbutton(toolbar, text="OCR", variable=self.run_ocr).pack(side=LEFT, padx=(0, 12))
        ttk.Button(toolbar, text="GPU Warmup", command=self.run_warmup).pack(side=LEFT)
        ttk.Button(toolbar, text="Pipeline Calistir", command=self.run_pipeline).pack(side=LEFT)
        ttk.Button(toolbar, text="Notu Kaydet", command=self.save_note).pack(side=LEFT, padx=(8, 0))
        ttk.Button(toolbar, text="Cikti Klasorunu Ac", command=self.open_output_folder).pack(side=LEFT, padx=(8, 0))

        ttk.Label(self, textvariable=self.status, padding=(10, 0)).pack(fill=X)

        self.notebook = ttk.Notebook(self)
        self.notebook.pack(fill=BOTH, expand=True, padx=10, pady=10)

        self.tabs = {}
        for name in [
            "Orijinal",
            "Model 1 Overlay",
            "Perspektif Crop",
            "Model 2 OCR Goruntusu",
            "Beyaz Canvas",
            "Not Canvas",
            "Text Mask",
            "OCR / Not",
        ]:
            frame = ttk.Frame(self.notebook, padding=8)
            self.notebook.add(frame, text=name)
            self.tabs[name] = frame

        self.image_labels = {}
        for name in [
            "Orijinal",
            "Model 1 Overlay",
            "Perspektif Crop",
            "Model 2 OCR Goruntusu",
            "Beyaz Canvas",
            "Text Mask",
        ]:
            label = ttk.Label(self.tabs[name], anchor="center")
            label.pack(fill=BOTH, expand=True)
            self.image_labels[name] = label

        self._build_note_canvas_tab()

        text_frame = self.tabs["OCR / Not"]
        self.ocr_text = tk.Text(text_frame, wrap="word", height=12)
        self.ocr_text.pack(side=LEFT, fill=BOTH, expand=True, padx=(0, 6))
        self.note_text = tk.Text(text_frame, wrap="word", height=12)
        self.note_text.pack(side=RIGHT, fill=BOTH, expand=True, padx=(6, 0))

    def _build_note_canvas_tab(self):
        frame = self.tabs["Not Canvas"]
        toolbar = ttk.Frame(frame)
        toolbar.pack(side=TOP, fill=X, pady=(0, 8))

        ttk.Button(toolbar, text="Yazi Katmanini Aktar", command=self.copy_text_layer_to_note_canvas).pack(side=LEFT)
        ttk.Button(toolbar, text="Sigdir", command=self.fit_note_canvas).pack(side=LEFT, padx=(8, 0))
        ttk.Button(toolbar, text="Yakinlastir", command=lambda: self.zoom_note_canvas(1.2)).pack(side=LEFT, padx=(8, 0))
        ttk.Button(toolbar, text="Uzaklastir", command=lambda: self.zoom_note_canvas(1 / 1.2)).pack(side=LEFT, padx=(4, 0))
        ttk.Button(toolbar, text="Crop Uygula", command=self.apply_note_crop).pack(side=LEFT, padx=(12, 0))
        ttk.Button(toolbar, text="Crop Sifirla", command=self.reset_note_canvas_crop).pack(side=LEFT, padx=(4, 0))
        ttk.Button(toolbar, text="PNG Kaydet", command=self.save_note_canvas_png).pack(side=LEFT, padx=(12, 0))

        ttk.Radiobutton(toolbar, text="Kalem", variable=self.note_canvas_mode, value="draw").pack(side=LEFT, padx=(16, 0))
        ttk.Radiobutton(toolbar, text="Silgi", variable=self.note_canvas_mode, value="erase").pack(side=LEFT, padx=(4, 0))
        ttk.Radiobutton(toolbar, text="Crop", variable=self.note_canvas_mode, value="crop").pack(side=LEFT, padx=(4, 0))
        ttk.Label(toolbar, text="Kalem").pack(side=LEFT, padx=(14, 4))
        ttk.Spinbox(toolbar, from_=1, to=32, textvariable=self.pen_size, width=4).pack(side=LEFT)

        self.note_canvas = tk.Canvas(frame, bg="#e7e7e7", highlightthickness=0)
        self.note_canvas.pack(fill=BOTH, expand=True)
        self.note_canvas.bind("<Configure>", lambda _event: self.render_note_canvas(fit=False))
        self.note_canvas.bind("<ButtonPress-1>", self.on_note_canvas_press)
        self.note_canvas.bind("<B1-Motion>", self.on_note_canvas_drag)
        self.note_canvas.bind("<ButtonRelease-1>", self.on_note_canvas_release)

    def select_image(self):
        path = filedialog.askopenfilename(
            title="Board image",
            filetypes=[("Images", "*.jpg *.jpeg *.png *.bmp *.webp"), ("All files", "*.*")],
            initialdir=str(REPO_ROOT / "TestVerileri") if (REPO_ROOT / "TestVerileri").exists() else str(REPO_ROOT),
        )
        if not path:
            return
        self.image_path = Path(path)
        self.status.set(f"Secili gorsel: {self.image_path.name}")
        self.show_local_image("Orijinal", self.image_path)

    def run_pipeline(self):
        if self.image_path is None:
            messagebox.showwarning("Eksik gorsel", "Once bir gorsel secin.")
            return
        thread = threading.Thread(target=self._run_pipeline_worker, daemon=True)
        thread.start()

    def run_warmup(self):
        thread = threading.Thread(target=self._run_warmup_worker, daemon=True)
        thread.start()

    def _run_warmup_worker(self):
        try:
            self.after(0, lambda: self.status.set("GPU warmup calisiyor..."))
            response = requests.post(api_url(self.base_url.get(), "/api/v1/warmup"), timeout=360)
            response.raise_for_status()
            payload = response.json()
            self.after(0, lambda: self.status.set(f"GPU warmup tamamlandi. Device={payload.get('device')} AMP={payload.get('use_amp')}"))
        except Exception as exc:
            self.after(0, lambda: messagebox.showerror("Warmup hatasi", str(exc)))
            self.after(0, lambda: self.status.set("Warmup hatasi"))

    def _run_pipeline_worker(self):
        try:
            self.after(0, lambda: self.status.set("Pipeline calisiyor... Model ve OCR ilk calistirmada yavas acilabilir."))
            with self.image_path.open("rb") as fh:
                files = {"image": (self.image_path.name, fh, "application/octet-stream")}
                data = {"threshold": f"{self.threshold.get():.3f}", "run_ocr": str(self.run_ocr.get()).lower()}
                response = requests.post(api_url(self.base_url.get(), "/api/v1/pipeline"), files=files, data=data, timeout=900)
            response.raise_for_status()
            payload = response.json()
            self.response = payload
            self.after(0, lambda: self.render_response(payload))
        except Exception as exc:
            self.after(0, lambda: messagebox.showerror("Pipeline hatasi", str(exc)))
            self.after(0, lambda: self.status.set("Hata olustu"))

    def render_response(self, payload: dict):
        artifacts = payload.get("artifacts", {})
        mapping = {
            "Model 1 Overlay": artifacts.get("overlay"),
            "Perspektif Crop": artifacts.get("perspective_crop"),
            "Model 2 OCR Goruntusu": artifacts.get("ocr_enhanced"),
            "Beyaz Canvas": artifacts.get("white_canvas"),
            "Text Mask": artifacts.get("text_mask"),
        }
        for tab, rel_url in mapping.items():
            if rel_url:
                self.show_remote_image(tab, rel_url)
        if artifacts.get("ocr_enhanced") and artifacts.get("text_mask"):
            self.load_note_canvas_from_text_layer(artifacts["ocr_enhanced"], artifacts["text_mask"])

        self.ocr_text.delete("1.0", END)
        self.ocr_text.insert("1.0", payload.get("ocr_text", ""))

        note = payload.get("note", {})
        note_body = f"{note.get('title', '')}\n\n{note.get('body', '')}".strip()
        self.note_text.delete("1.0", END)
        self.note_text.insert("1.0", note_body)

        elapsed = payload.get("timings_ms", {}).get("total_ms")
        suffix = f" | toplam {elapsed:.0f} ms" if isinstance(elapsed, (int, float)) else ""
        warnings = payload.get("warnings", [])
        warning_suffix = f" | uyarilar: {len(warnings)}" if warnings else ""
        self.status.set(f"Tamamlandi: {payload.get('job_id', '')}{suffix}{warning_suffix}")

    def show_local_image(self, tab_name: str, path: Path):
        image = Image.open(path).convert("RGB")
        self._show_pil_image(tab_name, image)

    def show_remote_image(self, tab_name: str, rel_url: str):
        image = self.fetch_remote_image(rel_url)
        self._show_pil_image(tab_name, image)

    def fetch_remote_image(self, rel_url: str) -> Image.Image:
        url = api_url(self.base_url.get(), rel_url)
        response = requests.get(url, timeout=120)
        response.raise_for_status()
        return Image.open(BytesIO(response.content)).convert("RGB")

    def _show_pil_image(self, tab_name: str, image: Image.Image):
        label = self.image_labels[tab_name]
        max_w = max(300, label.winfo_width() or 1000)
        max_h = max(220, label.winfo_height() or 650)
        display = image.copy()
        display.thumbnail((max_w, max_h), Image.Resampling.LANCZOS)
        photo = ImageTk.PhotoImage(display)
        self.photo_refs[tab_name] = photo
        label.configure(image=photo)

    def load_note_canvas_from_artifact(self, rel_url: str):
        try:
            image = self.fetch_remote_image(rel_url)
        except Exception as exc:
            self.status.set(f"Not canvas aktarilamadi: {exc}")
            return
        self.note_canvas_original = image.copy()
        self.note_canvas_image = image.copy()
        self.fit_note_canvas()

    def load_note_canvas_from_text_layer(self, ocr_url: str, mask_url: str):
        try:
            ocr_image = self.fetch_remote_image(ocr_url)
            mask_image = self.fetch_remote_image(mask_url).convert("L")
            page = build_note_page_from_text_layer(ocr_image, mask_image)
        except Exception as exc:
            self.status.set(f"Yazi katmani aktarilamadi: {exc}")
            return
        self.note_canvas_original = page.copy()
        self.note_canvas_image = page.copy()
        self.fit_note_canvas()

    def copy_text_layer_to_note_canvas(self):
        if not self.response:
            messagebox.showwarning("Cikti yok", "Once pipeline calistirin.")
            return
        artifacts = self.response.get("artifacts", {})
        ocr_url = artifacts.get("ocr_enhanced")
        mask_url = artifacts.get("text_mask")
        if not ocr_url or not mask_url:
            messagebox.showwarning("Yazi katmani yok", "Pipeline cevabinda ocr_enhanced veya text_mask artifact'i bulunamadi.")
            return
        self.load_note_canvas_from_text_layer(ocr_url, mask_url)
        self.notebook.select(self.tabs["Not Canvas"])

    def fit_note_canvas(self):
        if self.note_canvas_image is None:
            return
        width = max(200, self.note_canvas.winfo_width())
        height = max(160, self.note_canvas.winfo_height())
        img_w, img_h = self.note_canvas_image.size
        self.note_canvas_zoom = max(0.05, min((width - 24) / img_w, (height - 24) / img_h))
        self.render_note_canvas(fit=True)

    def zoom_note_canvas(self, factor: float):
        if self.note_canvas_image is None:
            return
        self.note_canvas_zoom = max(0.05, min(6.0, self.note_canvas_zoom * factor))
        self.render_note_canvas(fit=False)

    def render_note_canvas(self, fit: bool = False):
        if not hasattr(self, "note_canvas") or self.note_canvas_image is None:
            return
        canvas_w = max(1, self.note_canvas.winfo_width())
        canvas_h = max(1, self.note_canvas.winfo_height())
        img_w, img_h = self.note_canvas_image.size
        display_w = max(1, int(round(img_w * self.note_canvas_zoom)))
        display_h = max(1, int(round(img_h * self.note_canvas_zoom)))
        offset_x = max(8, (canvas_w - display_w) // 2)
        offset_y = max(8, (canvas_h - display_h) // 2)
        self.note_canvas_offset = (offset_x, offset_y)
        self.note_canvas_display_size = (display_w, display_h)

        display = self.note_canvas_image.resize((display_w, display_h), Image.Resampling.LANCZOS)
        self.note_canvas_photo = ImageTk.PhotoImage(display)
        self.note_canvas.delete("all")
        self.note_canvas.create_image(offset_x, offset_y, anchor="nw", image=self.note_canvas_photo, tags=("page",))
        self.note_canvas.configure(scrollregion=(0, 0, max(canvas_w, offset_x + display_w + 8), max(canvas_h, offset_y + display_h + 8)))
        self.note_crop_rect_id = None

    def on_note_canvas_press(self, event):
        if self.note_canvas_image is None:
            return
        point = self.canvas_to_image_point(event.x, event.y)
        if point is None:
            return
        if self.note_canvas_mode.get() == "crop":
            self.note_crop_start = point
            self.delete_crop_rect()
            x, y = self.image_to_canvas_point(*point)
            self.note_crop_rect_id = self.note_canvas.create_rectangle(x, y, x, y, outline="#1d4ed8", width=2, dash=(4, 3))
        else:
            self.note_canvas_last_point = point

    def on_note_canvas_drag(self, event):
        if self.note_canvas_image is None:
            return
        point = self.canvas_to_image_point(event.x, event.y)
        if point is None:
            return
        mode = self.note_canvas_mode.get()
        if mode == "crop" and self.note_crop_start and self.note_crop_rect_id:
            x0, y0 = self.image_to_canvas_point(*self.note_crop_start)
            x1, y1 = self.image_to_canvas_point(*point)
            self.note_canvas.coords(self.note_crop_rect_id, x0, y0, x1, y1)
            return
        if mode in {"draw", "erase"} and self.note_canvas_last_point:
            draw = ImageDraw.Draw(self.note_canvas_image)
            color = (255, 255, 255) if mode == "erase" else (20, 20, 20)
            width = max(1, int(self.pen_size.get()))
            draw.line([self.note_canvas_last_point, point], fill=color, width=width, joint="curve")
            self.note_canvas_last_point = point
            self.render_note_canvas(fit=False)

    def on_note_canvas_release(self, _event):
        self.note_canvas_last_point = None

    def canvas_to_image_point(self, x: int, y: int) -> tuple[int, int] | None:
        if self.note_canvas_image is None:
            return None
        offset_x, offset_y = self.note_canvas_offset
        display_w, display_h = self.note_canvas_display_size
        if x < offset_x or y < offset_y or x > offset_x + display_w or y > offset_y + display_h:
            return None
        img_w, img_h = self.note_canvas_image.size
        ix = int((x - offset_x) / max(1, display_w) * img_w)
        iy = int((y - offset_y) / max(1, display_h) * img_h)
        return max(0, min(img_w - 1, ix)), max(0, min(img_h - 1, iy))

    def image_to_canvas_point(self, x: int, y: int) -> tuple[int, int]:
        offset_x, offset_y = self.note_canvas_offset
        display_w, display_h = self.note_canvas_display_size
        img_w, img_h = self.note_canvas_image.size if self.note_canvas_image else (1, 1)
        return (
            int(offset_x + x / max(1, img_w) * display_w),
            int(offset_y + y / max(1, img_h) * display_h),
        )

    def apply_note_crop(self):
        if self.note_canvas_image is None or self.note_crop_start is None or self.note_crop_rect_id is None:
            messagebox.showwarning("Crop yok", "Crop modunda canvas uzerinde bir alan secin.")
            return
        coords = self.note_canvas.coords(self.note_crop_rect_id)
        if len(coords) != 4:
            return
        end = self.canvas_to_image_point(int(coords[2]), int(coords[3]))
        if end is None:
            return
        x0, y0 = self.note_crop_start
        x1, y1 = end
        left, right = sorted((x0, x1))
        top, bottom = sorted((y0, y1))
        if right - left < 8 or bottom - top < 8:
            messagebox.showwarning("Crop kucuk", "Secilen crop alani cok kucuk.")
            return
        self.note_canvas_image = self.note_canvas_image.crop((left, top, right, bottom)).copy()
        self.note_crop_start = None
        self.delete_crop_rect()
        self.fit_note_canvas()

    def reset_note_canvas_crop(self):
        if self.note_canvas_original is None:
            return
        self.note_canvas_image = self.note_canvas_original.copy()
        self.note_crop_start = None
        self.delete_crop_rect()
        self.fit_note_canvas()

    def delete_crop_rect(self):
        if self.note_crop_rect_id is not None:
            self.note_canvas.delete(self.note_crop_rect_id)
            self.note_crop_rect_id = None

    def save_note_canvas_png(self):
        if self.note_canvas_image is None:
            messagebox.showwarning("Canvas bos", "Once pipeline calistirin veya beyaz ciktiyi aktarin.")
            return
        NOTES_DIR.mkdir(parents=True, exist_ok=True)
        job_id = self.response.get("job_id", "job") if self.response else "job"
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        path = NOTES_DIR / f"{timestamp}_{job_id}_canvas.png"
        self.note_canvas_image.save(path)
        self.status.set(f"Canvas kaydedildi: {path.name}")

    def save_note(self):
        if not self.response:
            messagebox.showwarning("Kayit yok", "Once pipeline calistirin.")
            return
        NOTES_DIR.mkdir(parents=True, exist_ok=True)
        job_id = self.response.get("job_id", "job")
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        note_text = self.note_text.get("1.0", END).strip()
        ocr_text = self.ocr_text.get("1.0", END).strip()
        txt_path = NOTES_DIR / f"{timestamp}_{job_id}.txt"
        json_path = NOTES_DIR / f"{timestamp}_{job_id}.json"
        txt_path.write_text(f"{note_text}\n\n--- OCR ---\n{ocr_text}\n", encoding="utf-8")
        json_path.write_text(json.dumps(self.response, ensure_ascii=False, indent=2), encoding="utf-8")
        if self.note_canvas_image is not None:
            self.note_canvas_image.save(NOTES_DIR / f"{timestamp}_{job_id}_canvas.png")
        self.status.set(f"Not kaydedildi: {txt_path.name}")

    def open_output_folder(self):
        if not self.response:
            messagebox.showwarning("Cikti yok", "Once pipeline calistirin.")
            return
        job_id = self.response.get("job_id")
        path = BACKEND_OUTPUTS / job_id
        if path.exists():
            os.startfile(path)
            return
        webbrowser.open(api_url(self.base_url.get(), f"/api/v1/artifacts/{job_id}/pipeline_response.json"))


def api_url(base: str, path: str) -> str:
    base = base.rstrip("/") + "/"
    return urljoin(base, path.lstrip("/"))


def build_note_page_from_text_layer(ocr_image: Image.Image, mask_image: Image.Image) -> Image.Image:
    ocr = ocr_image.convert("RGB")
    mask = mask_image.convert("L")
    if mask.size != ocr.size:
        mask = mask.resize(ocr.size, Image.Resampling.BILINEAR)

    alpha = mask.point(lambda value: 255 if value >= TEXT_MASK_THRESHOLD else 0)
    bbox = alpha.getbbox()
    page = Image.new("RGB", (NOTE_PAGE_WIDTH, NOTE_PAGE_HEIGHT), "white")
    if bbox is None:
        return page

    text_rgb = ocr.crop(bbox)
    text_alpha = alpha.crop(bbox).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(0.7))
    text_layer = Image.new("RGBA", text_rgb.size, (255, 255, 255, 0))
    text_layer.paste(text_rgb.convert("RGBA"), (0, 0), text_alpha)

    available_width = NOTE_PAGE_WIDTH - NOTE_PAGE_MARGIN * 2
    available_height = NOTE_PAGE_HEIGHT - NOTE_PAGE_MARGIN * 2
    scale = min(1.0, available_width / text_layer.width, available_height / text_layer.height)
    if scale < 1.0:
        new_size = (
            max(1, int(round(text_layer.width * scale))),
            max(1, int(round(text_layer.height * scale))),
        )
        text_layer = text_layer.resize(new_size, Image.Resampling.LANCZOS)

    x = NOTE_PAGE_MARGIN
    y = NOTE_PAGE_MARGIN
    page.paste(text_layer, (x, y), text_layer)
    return page


if __name__ == "__main__":
    Board2NotesTester().mainloop()
