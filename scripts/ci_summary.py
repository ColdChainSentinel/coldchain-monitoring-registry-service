"""Summarize actual Maven reports; never replace missing results with successes."""
import os
from pathlib import Path
import xml.etree.ElementTree as ET

lines = ["# ColdChain Sentinel — CI results", ""]
reports = list(Path("target/surefire-reports").glob("TEST-*.xml"))
if reports:
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    for path in reports:
        root = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(root.get(key, 0))
    lines.extend(["| Metric | Value |", "|---|---:|"] + [f"| {k} | {v} |" for k, v in totals.items()])
else:
    lines.append("No test reports: tests may not have run. Inspect the failing step.")
coverage = Path("target/site/jacoco/jacoco.xml")
if coverage.exists():
    for counter in ET.parse(coverage).getroot().findall("counter"):
        if counter.get("type") == "LINE":
            covered, missed = int(counter.get("covered")), int(counter.get("missed"))
            total = covered + missed
            lines.append(f"\nLine coverage: {100 * covered / total:.2f}% ({covered}/{total}). Gate: 70%.")
else:
    lines.append("\nCoverage report unavailable in this run.")
text = "\n".join(lines) + "\n"
print(text)
if os.environ.get("GITHUB_STEP_SUMMARY"):
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as output:
        output.write(text)
