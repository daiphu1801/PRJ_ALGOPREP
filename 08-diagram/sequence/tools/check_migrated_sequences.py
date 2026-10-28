"""Check the architecture boundary of migrated sequence specs in the baseline manifest."""

from __future__ import annotations

import csv
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
MANIFEST = ROOT / "08-diagram/sequence/baseline_manifest.csv"


def walk_messages(items: list[dict]) -> list[dict]:
    messages: list[dict] = []
    for item in items:
        if "alt" in item:
            messages.extend(walk_messages(item["alt"].get("then", [])))
            messages.extend(walk_messages(item["alt"].get("else", [])))
        elif "from" in item or "to" in item:
            messages.append(item)
    return messages


def check_spec(path: Path) -> list[str]:
    errors: list[str] = []
    spec = json.loads(path.read_text(encoding="utf-8"))
    sequence = spec["sequence"]
    participants = sequence["participants"]
    names = [p["name"] for p in participants]
    roles = {p["name"]: p.get("role", "") for p in participants}
    actor_positions = [i for i, p in enumerate(participants) if p.get("role") == "actor"]
    if actor_positions and actor_positions[0] != 0:
        errors.append("human actor must be the leftmost lifeline")
    if actor_positions and any(p.get("role") == "controller" for p in participants):
        if len(participants) < 2 or participants[1].get("role") != "controller":
            errors.append("controller must follow the leftmost human actor")
    if len(names) != len(set(names)):
        errors.append("duplicate lifeline")
    if len(names) > 10:
        errors.append(f"{len(names)} lifelines exceeds detail limit 10")
    for msg in walk_messages(sequence["messages"]):
        source = msg.get("from")
        target = msg.get("to")
        if source not in roles or target not in roles:
            errors.append(f"unknown lifeline: {source} -> {target}")
            continue
        if roles[source] in {"controller", "service"} and roles[target] == "datastore":
            errors.append(f"direct application/controller -> datastore: {source} -> {target}")
        if roles[source] == "controller" and roles[target] == "repository":
            errors.append(f"controller bypasses application: {source} -> {target}")
    content = path.read_text(encoding="utf-8")
    if "\ufffd" in content or "??" in content:
        errors.append("possible broken Unicode")
    if not spec.get("covers"):
        errors.append("missing covers")
    return errors


def main() -> int:
    with MANIFEST.open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    failures = 0
    migrated = [row for row in rows if row["migration_status"] == "pilot_reworked"]
    for row in migrated:
        for relative in row["replacement_specs"].split("|"):
            path = ROOT / relative
            if not path.is_file():
                print(f"FAIL {relative}: missing file")
                failures += 1
                continue
            problems = check_spec(path)
            if problems:
                for problem in problems:
                    print(f"FAIL {relative}: {problem}")
                    failures += 1
            else:
                print(f"PASS {relative}")
    print(f"Migrated baseline pages: {len(migrated)}/{len(rows)}; failures: {failures}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
