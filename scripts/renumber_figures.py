#!/usr/bin/env python3
"""
renumber_figures.py -- danh so lai hinh trong BAO_CAO_KHAO_SAT_HE_THONG.md.

Hai viec:
  1. So do bi CAT lam nhieu phan: mot dong `![...](.../x.png)` tro toi file khong con ton tai
     duoc thay bang N dong tro toi `x_p1.png ... x_pN.png`, chu thich them ` (phan k/N)`.
  2. Danh so lai toan bo `Hình <chuong>.<so>` cho lien tuc trong tung chuong, VA sua moi
     tham chieu `Hình <chuong>.<so>` nam trong doan van theo dung anh xa moi.

Vi sao la script chu khong phai dem tay: dot 2026-09-20 dem tay da lam trung `Hình 10.4`
roi lech 48 hinh phia sau ma khong ai phat hien. Xem 06-plan/260927-1719-sequence-vua-mot-trang.md.

  python scripts/renumber_figures.py            # kiem, khong ghi
  python scripts/renumber_figures.py --fix      # ghi de

Ma thoat: 0 khong co gi phai doi; 1 co thay doi (hoac da ghi); 2 khong doc duoc file.
"""

from __future__ import annotations

import argparse
import glob
import io
import os
import re
import sys

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass

MD = "BAO_CAO_KHAO_SAT_HE_THONG.md"
IMG = re.compile(r"^!\[([^\]]*)\]\(([^)]+)\)\s*$")
CAP = re.compile(r"^Hình (\d+)\.(\d+):\s*(.*)$")
REF = re.compile(r"Hình (\d+)\.(\d+)")
# So CUOI cua mot dai ("Hình 10.13 den 10.15") khong mang tien to "Hình", nen REF khong bat
# duoc. De nguyen thi doi so xong ra "Hình 10.19 den 10.15" -- dung lop loi ma script nay
# sinh ra de chan. Bat rieng phan sau cua dai.
REF_END = re.compile(r"(?<=đến )(\d+)\.(\d+)|(?<=tới )(\d+)\.(\d+)")


def expand(lines: list[str]) -> tuple[list[str], int]:
    """Thay dong anh tro toi file da bi cat bang N dong tro toi cac phan cua no."""
    out, n_exp = [], 0
    for line in lines:
        m = IMG.match(line)
        if not m or os.path.exists(m.group(2)):
            out.append(line)
            continue
        cap, path = m.group(1), m.group(2)
        parts = sorted(glob.glob(path[: -len(".png")] + "_p*.png"))
        if not parts:
            print(f"   KHONG TIM DUOC phan nao cho {path}")
            out.append(line)
            continue
        body = CAP.match(cap)
        text = body.group(3) if body else cap
        head = f"Hình {body.group(1)}.{body.group(2)}: " if body else ""
        for i, p in enumerate(parts, 1):
            out.append(f"![{head}{text} (phần {i}/{len(parts)})]({p.replace(os.sep, '/')})")
        n_exp += 1
        print(f"   {os.path.basename(path)} -> {len(parts)} phan")
    return out, n_exp


def renumber(lines: list[str]) -> tuple[list[str], dict[str, str], int]:
    """Danh so lai lien tuc trong tung chuong; tra ve anh xa so cu -> so moi."""
    counter: dict[str, int] = {}
    mapping: dict[str, str] = {}
    out, n_fix = [], 0
    for line in lines:
        m = IMG.match(line)
        if not m:
            out.append(line)
            continue
        cap, path = m.group(1), m.group(2)
        b = CAP.match(cap)
        if not b:
            out.append(line)
            continue
        ch, old, text = b.group(1), b.group(2), b.group(3)
        counter[ch] = counter.get(ch, 0) + 1
        new = counter[ch]
        # Nhieu phan cua cung mot so do goc deu mang so cu giong nhau: chi ghi anh xa
        # LAN DAU, de tham chieu trong doan van tro ve phan dau tien.
        mapping.setdefault(f"{ch}.{old}", f"{ch}.{new}")
        if old != str(new):
            n_fix += 1
        out.append(f"![Hình {ch}.{new}: {text}]({path})")
    return out, mapping, n_fix


def retarget(lines: list[str], mapping: dict[str, str]) -> tuple[list[str], int]:
    """Sua tham chieu `Hình C.N` trong doan van. Di qua moc tam de so cu va so moi
    khong dam nhau giua chung (vi du 10.5 -> 10.6 va 10.6 -> 10.7 cung luot)."""
    n = 0
    out = []
    for line in lines:
        if IMG.match(line):
            out.append(line)
            continue

        def sub(m: re.Match) -> str:
            nonlocal n
            key = f"{m.group(1)}.{m.group(2)}"
            if key in mapping and mapping[key] != key:
                n += 1
                return "Hình \x00" + mapping[key]
            return m.group(0)

        def sub_end(m: re.Match) -> str:
            nonlocal n
            g = [x for x in m.groups() if x is not None]
            key = f"{g[0]}.{g[1]}"
            if key in mapping and mapping[key] != key:
                n += 1
                return "\x00" + mapping[key]
            return m.group(0)

        line = REF_END.sub(sub_end, REF.sub(sub, line))
        out.append(line.replace("\x00", ""))
    return out, n


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Danh so lai hinh trong bao cao")
    ap.add_argument("--fix", action="store_true")
    ap.add_argument("--md", default=MD)
    a = ap.parse_args(argv)

    try:
        text = io.open(a.md, encoding="utf-8").read()
    except OSError as e:
        print(f"LOI: khong doc duoc {a.md}: {e}")
        return 2
    lines = text.split("\n")

    print("1. Mo rong so do da bi cat:")
    lines, n_exp = expand(lines)
    if not n_exp:
        print("   khong co")

    print("2. Danh so lai:")
    lines, mapping, n_fix = renumber(lines)
    print(f"   {n_fix} hinh doi so")

    print("3. Sua tham chieu trong doan van:")
    lines, n_ref = retarget(lines, mapping)
    print(f"   {n_ref} tham chieu doi theo")

    new = "\n".join(lines)
    # -- kiem lai: khong trung so, lien tuc trong tung chuong, moi anh ton tai
    seen: dict[str, list[int]] = {}
    bad = 0
    for line in lines:
        m = IMG.match(line)
        if not m:
            continue
        if not os.path.exists(m.group(2)):
            print(f"   VAN THIEU FILE: {m.group(2)}"); bad += 1
        b = CAP.match(m.group(1))
        if b:
            seen.setdefault(b.group(1), []).append(int(b.group(2)))
    for ch, nums in sorted(seen.items()):
        if nums != list(range(1, len(nums) + 1)):
            print(f"   CHUONG {ch} KHONG LIEN TUC: {nums}"); bad += 1
    print(f"4. Kiem: {sum(len(v) for v in seen.values())} hinh, "
          f"{'0 loi' if not bad else str(bad) + ' loi'}")
    if bad:
        return 1

    if new == text:
        print("KHOP: khong co gi phai doi.")
        return 0
    if a.fix:
        io.open(a.md, "w", encoding="utf-8", newline="\n").write(new)
        print(f"DA GHI {a.md}.")
    else:
        print("Co thay doi. Chay lai voi --fix de ghi.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
