#!/usr/bin/env python3
"""
build_erd_docx.py -- sinh BAO_CAO_CO_SO_DU_LIEU_ERD.docx tu 08-diagram/erd/report.md.

Dinh dang (kho giay, le, font, heading, bang, chu thich) giong het BAO_CAO_KHAO_SAT_HE_THONG.docx vi
dung chung build_bao_cao_docx.py; chi them buoc tien xu ly .md cho hop khuon bao cao (xem prepare()).

Vi sao khong dung thang PNG co san: ERD theo schema ve du cot nen rong, nhet vao trang doc Letter
(6.53 inch) thi chu con ~2 pt. Cac viec lam hinh to ra:
  1. Xuat lai tu .drawio bang draw.io CLI o scale 3 (vector -> PNG vai nghin px), khong phong to PNG cu.
  2. Cat vien trang quanh hinh (PNG goc co le trang).
  3. Hinh rong hon SPLIT_ABOVE_PX duoc tach doi (phan 1/2, 2/2) tai khe doc giua hai cot bang, doc
     toa do tu .drawio, nen khong cat xuyen qua bang nao. Khung chu giai bi cat khoi anh va dua vao
     van ban ngay duoi hinh.
  4. (Tuy chon --landscape) thay cho viec tach: moi hinh nam tren mot trang NGANG rieng.
PNG trong 08-diagram/erd/ khong bi ghi de; ban render nam o thu muc tam.

Dung:
  python scripts/build_erd_docx.py
  python scripts/build_erd_docx.py --out F:/x.docx --scale 4

Ma thoat: 0 xong; 1 thieu anh hoac draw.io loi; 3 khong co draw.io CLI.
"""

from __future__ import annotations

import argparse
import html
import os
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

from PIL import Image, ImageChops

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(REPO, ".claude", "skills", "drawio-uml-architect", "scripts"))

import build_bao_cao_docx as base  # noqa: E402
from render_drawio import find_cli  # noqa: E402

ERD_DIR = os.path.join(REPO, "08-diagram", "erd")
MD = os.path.join(ERD_DIR, "report.md")
OUT = os.path.join(REPO, "BAO_CAO_CO_SO_DU_LIEU_ERD.docx")
PAD = 30
SPLIT_ABOVE_UNITS = 1700  # hinh rong hon (don vi drawio) thi tach doi: chu ~2x to hon khi vao trang doc
OVERLAP = 0.03          # moi nua lan sang nua kia 3% be rong de duong noi cat ngang van theo doi duoc

SUBTITLE = "NỀN TẢNG LUYỆN TẬP THUẬT TOÁN LẬP TRÌNH VÀ ÔN TẬP PHỎNG VẤN TÍCH HỢP AI"
RE_IMG = re.compile(r"^!\[[^\]]*\]\(([^)]+)\)\s*$")


def prepare(text: str, notes: dict[str, str] | None = None) -> str:
    """Dua report.md ve khuon cua bao cao khao sat (tieu de, muc luc that, chu thich hinh mot lan).
    notes: duong dan anh -> chu giai da bi cat khoi anh, chen ngay duoi chu thich hinh."""
    notes = notes or {}
    lines = text.split("\n")
    out: list[str] = []
    i = 0
    while i < len(lines):
        ln = lines[i]
        if i == 0 and ln.startswith("# "):
            out += ["# BÁO CÁO THIẾT KẾ CƠ SỞ DỮ LIỆU", "", "## " + SUBTITLE, "", "**" + ln[2:].strip() + "**"]
        elif ln.startswith("## 1."):
            out += ["[[TOC]]", "", ln]
        elif ln.strip() == "## Mục lục":
            i += 1
            while i < len(lines) and not lines[i].startswith("## "):
                i += 1
            continue
        elif re.match(r"^\*Hình [\d.]+:", ln):
            # Chu thich da duoc them tu alt cua anh; chi giu phan ghi chu phia sau (nguon, luu y).
            body = ln.strip().strip("*")
            m = re.search(r"(Nguồn sơ đồ:.*)$", body)
            if m:
                out.append(m.group(1))
            else:
                m = re.search(r"\((.*)\)\.?$", body)
                if m:
                    out.append("Ghi chú: " + m.group(1) + ".")
        else:
            out.append(ln)
            m = RE_IMG.match(ln)
            if m and m.group(1) in notes:
                out += ["", notes[m.group(1)]]
        i += 1
    return "\n".join(out)


def trim(path: str) -> None:
    """Cat vien trang, chua PAD pixel quanh noi dung."""
    im = Image.open(path).convert("RGB")
    box = ImageChops.difference(im, Image.new("RGB", im.size, (255, 255, 255))).getbbox()
    if box:
        l, t, r, b = box
        im = im.crop((max(l - PAD, 0), max(t - PAD, 0), min(r + PAD, im.width), min(b + PAD, im.height)))
    im.save(path)


def read_geometry(drawio: str):
    """Doc .drawio: khung cac bang, goc tren-trai cua noi dung, va khung chu giai (neu co).
    Tra (box_bang, lo_x, lo_y, legend) voi legend = (y, van ban) hoac None."""
    root = ET.parse(drawio).getroot()
    boxes, xs, ys, legend = [], [], [], None
    for c in root.iter("mxCell"):
        g = c.find("mxGeometry")
        if g is None:
            continue
        for pt in g.iter("mxPoint"):
            if pt.get("x"):
                xs.append(float(pt.get("x")))
                ys.append(float(pt.get("y", 0)))
        if c.get("vertex") != "1" or not g.get("width"):
            continue
        x, y, w = float(g.get("x", 0)), float(g.get("y", 0)), float(g.get("width"))
        text = re.sub(r"<[^>]+>", " ", html.unescape(c.get("value") or ""))
        text = re.sub(r"\s+", " ", text).strip()
        ys.append(y)
        if c.get("id") == "title":
            continue
        xs += [x, x + w]
        if text.startswith("Chú giải"):
            legend = (y, text)
            continue
        boxes.append((x, x + w))
    return boxes, min(xs), min(ys), legend


def gap_cut(boxes, lo_x: float, scale: float) -> tuple[int, int] | None:
    """Toa do x (pixel, tren anh da cat vien) cua khe doc giua cac bang gan tam hinh nhat,
    cung nua be rong khe (pixel), hoac None neu khong co khe nao du rong."""
    if not boxes:
        return None
    merged: list[list[float]] = []
    for a, b in sorted(boxes):
        if merged and a <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], b)
        else:
            merged.append([a, b])
    mid = (min(a for a, _ in boxes) + max(b for _, b in boxes)) / 2
    gaps = [((m1[1] + m2[0]) / 2, m2[0] - m1[1]) for m1, m2 in zip(merged, merged[1:]) if m2[0] - m1[1] >= 20]
    if not gaps:
        return None
    # uu tien khe gan tam, nhung khe rong hon thi duoc cong diem
    x, gw = min(gaps, key=lambda g: abs(g[0] - mid) - g[1] * 0.5)
    return int(PAD + (x - lo_x) * scale), int(gw / 2 * scale)


def split_halves(path: str, drawio: str, scale: float) -> tuple[list[str], str | None]:
    """Cat doi theo chieu ngang tai khe giua hai cot bang (khong cat xuyen qua bang nao).
    Khung chu giai (rong qua nua hinh, se bi cat doi) duoc cat khoi anh va tra ve dang van ban."""
    full = Image.open(path)
    w, h = full.size
    boxes, lo_x, lo_y, legend = read_geometry(drawio)
    found = gap_cut(boxes, lo_x, scale)
    if found is None or not (0.2 * w < found[0] < 0.8 * w):
        found = (w // 2, int(w * OVERLAP))
        print("   canh bao: khong tim duoc khe giua cac bang, cat giua hinh")
    cut, half = found
    if legend:
        h = min(h, int(PAD + (legend[0] - lo_y) * scale) - 8)
    # Phan chong khong duoc lan vao bang ke ben: gioi han theo nua be rong khe.
    ov = max(min(int(w * OVERLAP), half - 12), 0)
    out = []
    for k, box in enumerate(((0, 0, cut + ov, h), (cut - ov, 0, w, h)), 1):
        part = path[:-4] + f"_p{k}.png"
        full.crop(box).save(part)
        out.append(part)
    return out, legend[1] if legend else None


def render_complete(cli: str, drawio: str, out_png: str, scale: float) -> float | None:
    """Xuat PNG, ha scale cho toi khi hinh KHONG bi cat. Bai hoc 2026-10-23: o scale 3, draw.io desktop
    ve thieu phan duoi cua hinh lon (judge 5088 px cao nhung chi co noi dung toi 3840; ai, problem
    tuong tu) ma van tra ma thoat 0. Phat hien bang cach xem noi dung co cham sat mep duoi/phai
    (le -b 10 nen chi con ~10*scale px trang) hay khong. Tra scale da dung, hoac None neu that bai."""
    for sc in (scale, 2.5, 2, 1.5):
        if sc > scale:
            continue
        r = subprocess.run([cli, "-x", "-f", "png", "-s", str(sc), "-b", "10", "-o", out_png, drawio],
                           capture_output=True, text=True)
        if r.returncode != 0 or not os.path.exists(out_png):
            return None
        im = Image.open(out_png).convert("RGB")
        box = ImageChops.difference(im, Image.new("RGB", im.size, (255, 255, 255))).getbbox()
        if box and im.height - box[3] <= 14 * sc + 10 and im.width - box[2] <= 14 * sc + 10:
            return sc
        print(f"   scale {sc} bi cat mat phan duoi/phai, thu scale thap hon")
    return None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--md", default=MD)
    ap.add_argument("--out", default=OUT)
    ap.add_argument("--scale", type=float, default=3)
    ap.add_argument("--landscape", action="store_true", help="moi hinh tren mot trang ngang rieng, khong tach doi")
    args = ap.parse_args()

    cli = find_cli()
    if not cli:
        print("LOI: khong tim thay draw.io CLI (xem render_drawio.py --find)")
        return 3

    text = open(args.md, encoding="utf-8").read()
    rels = sorted(set(re.findall(r"!\[[^\]]*\]\(([^)]+\.png)\)", text)))
    tmp = tempfile.mkdtemp(prefix="erd_")
    img_map: dict = {}
    notes: dict[str, str] = {}
    for rel in rels:
        drawio = os.path.join(os.path.dirname(args.md), rel[:-4] + ".drawio")
        if not os.path.exists(drawio):
            print(f"LOI: khong co {drawio}")
            return 1
        out_png = os.path.join(tmp, os.path.basename(rel))
        scale = render_complete(cli, drawio, out_png, args.scale)
        if scale is None:
            print(f"LOI: draw.io xuat that bai hoac cat mat phan duoi hinh: {drawio}")
            return 1
        trim(out_png)
        size = Image.open(out_png).size
        if size[0] / scale > SPLIT_ABOVE_UNITS and not args.landscape:
            img_map[rel], note = split_halves(out_png, drawio, scale)
            if note:
                notes[rel] = note
            print(f"   {rel} -> {size}, tach 2 phan")
        else:
            img_map[rel] = out_png
            print(f"   {rel} -> {size}")

    # .md tam nam canh report.md de duong dan anh tuong doi van dung.
    tmp_md = os.path.join(os.path.dirname(args.md), ".report_build.md")
    with open(tmp_md, "w", encoding="utf-8") as fh:
        fh.write(prepare(text, notes))
    try:
        return base.build(tmp_md, args.out, img_map=img_map, landscape_figs=args.landscape)
    finally:
        os.remove(tmp_md)


if __name__ == "__main__":
    sys.exit(main())
