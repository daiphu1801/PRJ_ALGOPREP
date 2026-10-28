#!/usr/bin/env python3
"""Check structure and TDD trace of one QA testcase cluster.

Business branch and oracle review remains a human/agent task.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


FIELDS = (
    "AC / DD source",
    "Kind / priority",
    "Environment",
    "Preconditions",
    "Test data",
    "Steps",
    "Expected",
    "Cleanup",
    "Evidence",
    "Design review",
)
CASE_HEADING = re.compile(r"(?m)^### (TC-[A-Za-z0-9-]+)(?:\s|$)")
ROW = re.compile(r"(?m)^\| ([^|]+) \| (.*?) \|\s*$")
TRACE_LINK = re.compile(r"\[([^\]]*TC-[^\]]+)\]\(([^)#]+\.md)(?:#[^)]*)?\)")
FLAGS = re.compile(r"(?i)(?<!no )open oracle|oracle blocked|unresolved oracle|chưa chốt|cần chốt|chưa xác định|chưa định dạng chính xác|nếu đã được chốt|\bTBD\b")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cases", type=Path, nargs="+", required=True)
    parser.add_argument("--tdd", type=Path, nargs="+", required=True)
    parser.add_argument("--ac", action="append", default=[], help="AC label, e.g. AC-25 or PA-AC-03")
    parser.add_argument("--json", action="store_true", help="Print machine-readable report")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    errors: list[str] = []
    review_flags: list[str] = []
    cases: dict[str, Path] = {}
    source_labels: dict[str, str] = {}
    all_headers: dict[str, Path] = {}

    def matches_ac(source: str, ac: str) -> bool:
        return re.search(r"(?<![A-Za-z0-9])" + re.escape(ac) + r"(?![A-Za-z0-9])", source) is not None

    for path in args.cases:
        path = path.resolve()
        if not path.is_file():
            errors.append(f"Case file missing: {path}")
            continue
        content = path.read_text(encoding="utf-8")
        headings = list(CASE_HEADING.finditer(content))
        if not headings:
            errors.append(f"No testcase headings: {path}")
        for index, heading in enumerate(headings):
            case_id = heading.group(1)
            if case_id in all_headers:
                errors.append(f"Duplicate ID {case_id}: {all_headers[case_id]} and {path}")
            all_headers[case_id] = path
            end = headings[index + 1].start() if index + 1 < len(headings) else len(content)
            block = content[heading.end() : end]
            rows = {name.strip(): value.strip() for name, value in ROW.findall(block)}
            source = rows.get("AC / DD source", "")
            if args.ac and not any(matches_ac(source, ac) for ac in args.ac):
                continue
            cases[case_id] = path
            for field in FIELDS:
                if not rows.get(field):
                    errors.append(f"{case_id}: missing/empty {field}")
            source_labels[case_id] = source
            if FLAGS.search(block):
                review_flags.append(case_id)

    trace: dict[str, set[Path]] = {}
    for tdd_path in args.tdd:
        tdd_path = tdd_path.resolve()
        if not tdd_path.is_file():
            errors.append(f"TDD file missing: {tdd_path}")
            continue
        content = tdd_path.read_text(encoding="utf-8")
        for label, target in TRACE_LINK.findall(content):
            labels = []
            for item in re.finditer(r"(TC-[A-Za-z0-9-]+-)([0-9]+)(?:\s*(?:[–—]|đến\s*-)\s*([0-9]+))?", label):
                prefix, first, last = item.groups()
                start, stop = int(first), int(last or first)
                if stop < start or stop - start > 1000:
                    errors.append(f"Invalid testcase range {label}: {tdd_path}")
                    continue
                labels.extend(f"{prefix}{suffix:0{len(first)}d}" for suffix in range(start, stop + 1))
            for case_id in labels:
                trace.setdefault(case_id, set()).add((tdd_path.parent / target).resolve())

    for case_id, path in cases.items():
        if case_id not in trace:
            errors.append(f"{case_id}: no TDD trace link")
        elif path not in trace[case_id]:
            errors.append(f"{case_id}: TDD trace points to a different file")
    selected_paths = {path.resolve() for path in args.cases}
    for case_id, targets in trace.items():
        for target in targets & selected_paths:
            if all_headers.get(case_id) != target:
                errors.append(f"{case_id}: TDD references missing ID in {target}")
    for ac in args.ac:
        matched = [case_id for case_id, source in source_labels.items() if matches_ac(source, ac)]
        if not matched:
            errors.append(f"{ac}: no case source field in selected cluster")

    report = {
        "case_count": len(cases),
        "selected_acs": args.ac,
        "trace_links_for_cases": sum(len(trace.get(case_id, ())) for case_id in cases),
        "structural_errors": errors,
        "semantic_review_flags": sorted(set(review_flags)),
    }
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print(f"Cases: {report['case_count']}; trace links: {report['trace_links_for_cases']}; errors: {len(errors)}; review flags: {len(report['semantic_review_flags'])}")
        for issue in errors:
            print(f"ERROR {issue}")
        for case_id in report["semantic_review_flags"]:
            print(f"REVIEW {case_id}: unresolved-oracle wording")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
