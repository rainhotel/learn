from __future__ import annotations

import subprocess
from pathlib import Path

import fitz


ROOT = Path(__file__).resolve().parents[2]
TOPIC = ROOT / "01-topics" / "database-systems-review"
TMP = ROOT / "tmp" / "database-ppt-ocr"
TESSERACT = Path(r"C:\Program Files\Tesseract-OCR\tesseract.exe")
TESSDATA = ROOT / "tmp" / "tessdata"
OUTPUT = TOPIC / "raw-ppt-ocr.md"

PDFS = [
    ("ch1-1", Path(r"D:\qq_setup_31980\第1章-1.pdf")),
    ("ch3-1", Path(r"D:\qq_setup_31980\第3章-1.pdf")),
    ("ch3-2", Path(r"D:\qq_setup_31980\第3章-2.pdf")),
    ("ch3-3", Path(r"D:\qq_setup_31980\第3章-3.pdf")),
    ("ch3-4", Path(r"D:\qq_setup_31980\第3章-4.pdf")),
    ("ch3-5", Path(r"D:\qq_setup_31980\第3章-5.pdf")),
    ("ch4-1", Path(r"D:\qq_setup_31980\第4章-1.pdf")),
    ("ch4-2", Path(r"D:\qq_setup_31980\第4章-2.pdf")),
    ("ch5-1", Path(r"D:\qq_setup_31980\第5章-1.pdf")),
    ("ch5-2", Path(r"D:\qq_setup_31980\第5章-2.pdf")),
    ("ch5-3", Path(r"D:\qq_setup_31980\第5章-3.pdf")),
    ("ch5-4", Path(r"D:\qq_setup_31980\第5章-4.pdf")),
    ("ch7", Path(r"D:\qq_setup_31980\第7章.pdf")),
]


def require_files() -> None:
    if not TESSERACT.exists():
        raise SystemExit(f"Tesseract not found: {TESSERACT}")
    lang = TESSDATA / "chi_sim.traineddata"
    if not lang.exists():
        raise SystemExit(f"Missing Chinese OCR model: {lang}")
    missing = [str(path) for _, path in PDFS if not path.exists()]
    if missing:
        raise SystemExit("Missing PDFs:\n" + "\n".join(missing))


def render_page(pdf: fitz.Document, page_index: int, dest: Path) -> None:
    page = pdf[page_index]
    pix = page.get_pixmap(matrix=fitz.Matrix(2.0, 2.0), alpha=False)
    pix.save(dest)


def ocr_image(image: Path) -> str:
    cmd = [
        str(TESSERACT),
        str(image),
        "stdout",
        "-l",
        "chi_sim",
        "--tessdata-dir",
        str(TESSDATA),
        "--psm",
        "6",
    ]
    proc = subprocess.run(
        cmd,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return proc.stdout.strip()


def main() -> None:
    require_files()
    TMP.mkdir(parents=True, exist_ok=True)
    parts: list[str] = ["# 数据库系统课件 OCR 原文\n"]

    for label, pdf_path in PDFS:
        parts.append(f"\n## {label}: {pdf_path.name}\n")
        with fitz.open(pdf_path) as pdf:
            for page_index in range(len(pdf)):
                image = TMP / f"{label}-p{page_index + 1:03d}.png"
                render_page(pdf, page_index, image)
                text = ocr_image(image)
                parts.append(f"\n### {label} page {page_index + 1}\n\n{text}\n")
                print(f"OCR {label} page {page_index + 1}/{len(pdf)}")

    OUTPUT.write_text("\n".join(parts), encoding="utf-8")
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
