#!/usr/bin/env python3
"""
build_bao_cao_docx.py -- sinh BAO_CAO_KHAO_SAT_HE_THONG.docx tu file .md cung ten.

Nguon su that la ban .md. Ban .docx chi la ket qua xuat ra de nop, khong bao gio sua tay --
sua tay thi lan build sau mat sach, va hai ban lech nhau khong ai biet (dung lop loi ma
check_catalog.py sinh ra de chan o phia so do).

Quy uoc dinh dang lay tu chinh ban .docx dot truoc, giu nguyen de ban moi khong khac ban cu
ve hinh thuc:

  # / ## / ### / ####   -> Heading 1..4; Heading 1 can giua
  doan van              -> Normal, can deu hai bien (JUSTIFY)
  - muc                 -> List Bullet
  1. muc                -> List Number
  | bang |              -> Table Grid, hang dau in dam
  ![Hinh x.y: ...](p)   -> anh can giua rong 6.1 inch + mot doan CHU THICH in nghieng 11pt can giua
  **dam**  `ma`         -> run in dam / run Consolas
  [[TOC]]               -> "MUC LUC" + truong TOC that (Word bam Update Field de sinh) + ngat trang
  ---                   -> bo qua (ban .docx dot truoc cung bo)

Kho giay Letter 8.5x11, le 0.984 inch, chu Times New Roman 13pt -- doc tu template neu co.

Dung:
  python scripts/build_bao_cao_docx.py
  python scripts/build_bao_cao_docx.py --md <file.md> --out <file.docx>

Ma thoat:
  0  xong
  1  co anh tham chieu trong .md nhung khong ton tai tren dia
  2  khong doc duoc file nguon
"""

from __future__ import annotations

import argparse
import io
import os
import re
import struct
import sys

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml.ns import qn
from docx.shared import Inches, Pt

for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass

MD = "BAO_CAO_KHAO_SAT_HE_THONG.md"
OUT = "BAO_CAO_KHAO_SAT_HE_THONG.docx"

# Be ngang toi da = vung in duoc (8.5in - 2 x 0.984in le). Anh CAO hon mot trang thi
# thu nho theo CHIEU CAO thay vi cat mat phan duoi -- doc dinh nghia MAX_IMG_H.
IMG_W = Inches(6.53)
# Chieu cao con lai cua trang sau khi tru mot dong chu thich.
MAX_IMG_H = 8.6
CAPTION_PT = Pt(11)
BODY_PT = Pt(13)
MONO = "Consolas"
BODY_FONT = "Times New Roman"

RE_H = re.compile(r"^(#{1,6})\s+(.*)$")
RE_IMG = re.compile(r"^!\[([^\]]*)\]\(([^)]+)\)\s*$")
RE_BUL = re.compile(r"^\s*[-*]\s+(.*)$")
RE_NUM = re.compile(r"^\s*\d+\.\s+(.*)$")
RE_SPLIT = re.compile(r"(\*\*.+?\*\*|`[^`]+`)")


def add_runs(par, text: str) -> None:
    """Chia mot doan thanh cac run theo **dam** va `ma`. Khong co lop long nhau."""
    for piece in RE_SPLIT.split(text):
        if not piece:
            continue
        if piece.startswith("**") and piece.endswith("**") and len(piece) > 4:
            par.add_run(piece[2:-2]).bold = True
        elif piece.startswith("`") and piece.endswith("`") and len(piece) > 2:
            r = par.add_run(piece[1:-1])
            r.font.name = MONO
            # Chu co dau tieng Viet nam o bang East Asian; khong dat ca hai thi Word
            # tra ve font mac dinh va doan ma mat dinh dang.
            r._element.rPr.rFonts.set(qn("w:eastAsia"), MONO)
        else:
            par.add_run(piece)


def png_ratio(path: str) -> float | None:
    """Ty le cao/rong cua file PNG, doc thang tu header IHDR."""
    magic = bytes([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A])
    try:
        with open(path, "rb") as fh:
            if fh.read(8) != magic:
                return None
            fh.read(8)
            w, h = struct.unpack(">II", fh.read(8))
        return h / w
    except (OSError, struct.error):
        return None


def split_row(line: str) -> list[str]:
    return [c.strip() for c in line.strip().strip("|").split("|")]


def is_sep(line: str) -> bool:
    return bool(re.match(r"^\|[\s:\-|]+\|$", line.strip()))


def add_toc(doc) -> None:
    head = doc.add_paragraph("MỤC LỤC")
    head.alignment = WD_ALIGN_PARAGRAPH.CENTER

    par = doc.add_paragraph()
    run = par.add_run()
    for tag, attrs, text in (
        ("fldChar", {"w:fldCharType": "begin"}, None),
        ("instrText", {"xml:space": "preserve"}, 'TOC \\o "1-3" \\h \\z \\u'),
        ("fldChar", {"w:fldCharType": "separate"}, None),
    ):
        el = run._r.makeelement(qn("w:" + tag), {qn(k): v for k, v in attrs.items()})
        if text is not None:
            el.text = text
        run._r.append(el)
    hint = par.add_run("Nhấn chuột phải vào đây, chọn Update Field để sinh mục lục.")
    end = hint._r.makeelement(qn("w:fldChar"), {qn("w:fldCharType"): "end"})
    hint._r.append(end)

    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


def build(md_path: str, out_path: str) -> int:
    try:
        lines = io.open(md_path, encoding="utf-8").read().split("\n")
    except OSError as e:
        print(f"LOI: khong doc duoc {md_path}: {e}")
        return 2

    root = os.path.dirname(os.path.abspath(md_path))
    missing = [
        m.group(2) for m in (RE_IMG.match(x) for x in lines)
        if m and not os.path.exists(os.path.join(root, m.group(2)))
    ]
    if missing:
        print("LOI: .md tro toi anh khong ton tai:")
        for p in missing:
            print(f"   {p}")
        return 1

    doc = Document()
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Inches(8.5), Inches(11)
    for side in ("left_margin", "right_margin"):
        setattr(sec, side, Inches(0.984))
    normal = doc.styles["Normal"]
    normal.font.name = BODY_FONT
    normal.font.size = BODY_PT
    normal.element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)

    n_img = n_tbl = 0
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if not stripped or stripped == "---":
            i += 1
            continue

        if stripped == "[[TOC]]":
            add_toc(doc)
            i += 1
            continue

        m = RE_H.match(line)
        if m:
            level = min(len(m.group(1)), 4)
            par = doc.add_heading("", level=level)
            add_runs(par, m.group(2))
            if level == 1:
                par.alignment = WD_ALIGN_PARAGRAPH.CENTER
            i += 1
            continue

        m = RE_IMG.match(line)
        if m:
            caption, rel = m.group(1), m.group(2)
            par = doc.add_paragraph()
            par.alignment = WD_ALIGN_PARAGRAPH.CENTER
            # Dat het be ngang dung duoc, TRU KHI lam anh cao qua mot trang -- luc do lay
            # be ngang lon nhat ma van vua chieu cao. Chu trong hinh to nhat co the ma
            # khong bi cat mat phan duoi.
            src = os.path.join(root, rel)
            w = IMG_W
            ratio = png_ratio(src)
            if ratio and IMG_W.inches * ratio > MAX_IMG_H:
                w = Inches(MAX_IMG_H / ratio)
                print(f"   thu nho theo chieu cao: {rel} -> {w.inches:.2f} inch")
            par.add_run().add_picture(src, width=w)
            cap = doc.add_paragraph()
            cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            run = cap.add_run(caption)
            run.italic = True
            run.font.size = CAPTION_PT
            n_img += 1
            i += 1
            continue

        if stripped.startswith("|"):
            block = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                if not is_sep(lines[i]):
                    block.append(split_row(lines[i]))
                i += 1
            if block:
                tbl = doc.add_table(rows=0, cols=len(block[0]))
                tbl.style = "Table Grid"
                for r, cells in enumerate(block):
                    row = tbl.add_row().cells
                    for c, text in enumerate(cells[: len(block[0])]):
                        par = row[c].paragraphs[0]
                        add_runs(par, text)
                        if r == 0:
                            for run in par.runs:
                                run.bold = True
                n_tbl += 1
            continue

        m = RE_BUL.match(line)
        if m:
            add_runs(doc.add_paragraph(style="List Bullet"), m.group(1))
            i += 1
            continue

        m = RE_NUM.match(line)
        if m:
            add_runs(doc.add_paragraph(style="List Number"), m.group(1))
            i += 1
            continue

        par = doc.add_paragraph()
        par.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        add_runs(par, stripped)
        i += 1

    doc.save(out_path)
    print(f"OK {out_path}: {n_img} hinh, {n_tbl} bang.")
    return 0


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Sinh ban .docx cua bao cao khao sat tu ban .md")
    ap.add_argument("--md", default=MD)
    ap.add_argument("--out", default=OUT)
    args = ap.parse_args(argv)
    return build(args.md, args.out)


if __name__ == "__main__":
    sys.exit(main())
