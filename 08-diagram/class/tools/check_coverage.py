"""Build and validate actor-to-class-diagram traceability from RD user stories."""

from __future__ import annotations

import argparse
import csv
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "08-diagram/class/actor_coverage.csv"
STORIES = {
    "A1": ROOT / "01-rd/req/user_stories/a1_student.md",
    "A2": ROOT / "01-rd/req/user_stories/a2_instructor.md",
    "A3": ROOT / "01-rd/req/user_stories/a3_admin.md",
    "A4": ROOT / "01-rd/req/user_stories/a4_system.md",
}
CATALOG = ROOT / "01-rd/overview/feature_catalog.md"
DEAD = {"F4-04", "F5-27", "F6-11"} | {f"F4-09{x}" for x in "abcde"}
FIELDS = (
    "actor", "story", "criterion", "source", "line", "summary", "feature_codes",
    "catalog_gap", "diagram_group", "diagram", "class_refs", "evidence",
    "status", "notes",
)
HEADING = re.compile(r"^### (US-A[1-4]-\d{2}[a-z]?): (.+)$")
CODE = re.compile(r"\bF[1-6]-\d{2}[a-z]?\b")
RANGE = re.compile(r"\bF([1-6])-(\d{2})\s*(?:tới|đến|→)\s*F\1-(\d{2})\b")
FALLBACK_CODES = {
    "US-A1-10#06": ["F1-23"],  # screen USR0702 access scope, under class membership
    "US-A2-07#03": ["F1-23", "F1-25"],  # invalid/expired invite code
    "US-A4-03#01": ["F2-08"],  # code is in story heading
    "US-A4-05#01": ["F6-08"],  # code is in story heading
}
GROUPS = {
    "US-A1-01": "a1_auth", "US-A1-02": "a1_problem_discovery",
    "US-A1-03": "a1_submission", "US-A1-04": "a1_submission",
    "US-A1-05": "a1_profile_progress", "US-A1-06": "a1_ai_review",
    "US-A1-07": "a1_mock_interview", "US-A1-08": "a1_interview_bank",
    "US-A1-09": "a1_bookmark", "US-A1-10": "a1_my_classes",
    "US-A2-01": "a2_problem_authoring", "US-A2-01b": "a2_problem_authoring",
    "US-A2-02": "a2_testcases", "US-A2-03": "a2_assignments",
    "US-A2-05": "a2_ai_testcases", "US-A2-07": "a2_class_management",
    "US-A2-08": "a2_class_progress", "US-A2-09": "a2_class_progress",
    "US-A2-10": "a2_interview_content", "US-A2-11": "a2_overview",
    "US-A3-01": "a3_permissions", "US-A3-02": "a3_users",
    "US-A3-03": "a3_judge_operations", "US-A3-04": "a3_configuration",
    "US-A4-01": "a4_harness", "US-A4-02": "a4_judge_worker",
    "US-A4-03": "a4_hidden_testcases", "US-A4-04": "a4_ai_processing",
    "US-A4-05": "a4_interview_feedback",
}


def feature_codes(text: str) -> list[str]:
    found = set(CODE.findall(text))
    for module, first, last in RANGE.findall(text):
        found.update(f"F{module}-{n:02d}" for n in range(int(first), int(last) + 1))
    return sorted(found - DEAD)


def catalog_codes() -> set[str]:
    text = CATALOG.read_text(encoding="utf-8")
    return set(re.findall(r"^\| (F[1-6]-\d{2}) \|", text, re.M))


def story_rows() -> list[dict[str, str]]:
    live = catalog_codes()
    result: list[dict[str, str]] = []
    for actor, path in STORIES.items():
        lines = path.read_text(encoding="utf-8").splitlines()
        story = ""
        story_index = 0
        start = 0
        body: list[str] = []

        def flush() -> None:
            nonlocal body
            if not body:
                return
            raw = " ".join(part.strip() for part in body)
            summary = re.sub(r"\*\*|`", "", raw).removeprefix("- ")
            criterion = f"{story}#{story_index:02d}"
            codes = feature_codes(raw) or FALLBACK_CODES.get(criterion, [])
            gap = sorted(set(codes) - live)
            result.append({
                "actor": actor,
                "story": story,
                "criterion": criterion,
                "source": path.relative_to(ROOT).as_posix(),
                "line": str(start),
                "summary": summary[:220],
                "feature_codes": ";".join(codes),
                "catalog_gap": ";".join(gap),
                "diagram_group": GROUPS.get(story, ""),
                "diagram": "",
                "class_refs": "",
                "evidence": "",
                "status": "planned",
                "notes": "",
            })
            body = []

        for line_no, line in enumerate(lines, 1):
            match = HEADING.match(line)
            if match:
                flush()
                story = match.group(1)
                story_index = 0
                continue
            if line.startswith("### "):
                flush()
                story = ""
                continue
            if story and line.startswith("- **Cho**"):
                flush()
                story_index += 1
                start = line_no
                body = [line]
            elif body:
                if line.startswith("- "):
                    flush()
                else:
                    body.append(line)
        flush()
    return result


def read_existing() -> dict[str, dict[str, str]]:
    if not OUT.exists():
        return {}
    with OUT.open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    return {row["criterion"]: row for row in rows}


def write_rows(rows: list[dict[str, str]]) -> None:
    existing = read_existing()
    for row in rows:
        previous = existing.get(row["criterion"], {})
        for field in ("diagram_group", "diagram", "class_refs", "evidence", "status", "notes"):
            row[field] = previous.get(field) or row[field]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)


def check_rows(require_covered: bool) -> int:
    expected = {row["criterion"]: row for row in story_rows()}
    existing = read_existing()
    missing = sorted(set(expected) - set(existing))
    extra = sorted(set(existing) - set(expected))
    changed = sorted(key for key in expected.keys() & existing.keys()
                     if expected[key]["feature_codes"] != existing[key]["feature_codes"]
                     or expected[key]["source"] != existing[key]["source"]
                     or expected[key]["line"] != existing[key]["line"])
    uncovered = []
    invalid = []
    bad_refs = []
    missing_covers = []
    if require_covered:
        for key, row in existing.items():
            if row.get("status") != "covered":
                uncovered.append(key)
                continue
            target = ROOT / row.get("diagram", "")
            if not row.get("diagram") or not target.is_file() or not row.get("class_refs"):
                invalid.append(key)
                continue
            spec_path = target.with_suffix(".spec.json")
            if not spec_path.is_file():
                invalid.append(key)
                continue
            spec = json.loads(spec_path.read_text(encoding="utf-8"))
            classes = {item["name"]: item for item in spec["class"]["classes"]}
            for ref in row["class_refs"].split(";"):
                owner, separator, method = ref.partition(".")
                if not separator or owner not in classes or not any(
                    re.search(rf"\b{re.escape(method)}\s*\(", member)
                    for member in classes[owner].get("methods", [])
                ):
                    bad_refs.append(f"{key}: {ref}")
            codes = set(row["feature_codes"].split(";")) - {""}
            if not codes.issubset(set(spec.get("covers", []))):
                missing_covers.append(f"{key}: {sorted(codes - set(spec.get('covers', [])))}")
            source = ROOT / row["source"]
            if not source.is_file() or not row["line"].isdigit() or int(row["line"]) > len(source.read_text(encoding="utf-8").splitlines()):
                invalid.append(key)
    report = {
        "expected_criteria": len(expected),
        "by_actor": {actor: sum(row["actor"] == actor for row in expected.values())
                     for actor in STORIES},
        "missing": missing,
        "extra": extra,
        "source_or_code_changed": changed,
        "uncovered": uncovered,
        "invalid_covered_rows": invalid,
        "bad_class_method_refs": bad_refs,
        "missing_spec_covers": missing_covers,
        "catalog_gaps": sorted({code for row in expected.values()
                                for code in row["catalog_gap"].split(";") if code}),
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return int(bool(missing or extra or changed or uncovered or invalid or bad_refs
                    or missing_covers or report["catalog_gaps"]))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--sync", action="store_true", help="Create/update CSV without losing mappings")
    parser.add_argument("--check", action="store_true", help="Require all criteria to be covered")
    args = parser.parse_args()
    if args.sync:
        write_rows(story_rows())
    return check_rows(require_covered=args.check)


if __name__ == "__main__":
    raise SystemExit(main())
