#!/usr/bin/env python3
"""Fast string-resource parity check (no Gradle).

Mirrors app/src/test/.../StringResourceParityTest.kt, which stays the authoritative check.
Exits 1 and prints problems when a locale is missing a translatable key, carries a key that
is gone from values/strings.xml, or uses different format arguments than the base string.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
TAGS = {"string", "plurals", "string-array"}
FORMAT_ARG = re.compile(r"%(?:(\d+)\$)?[-#+0,(]*\d*(?:\.\d+)?([a-zA-Z%])")


def format_args(text):
    matches = [m for m in FORMAT_ARG.finditer(text) if m.group(2) != "%"]
    return {f"{m.group(1) or i + 1}:{m.group(2).lower()}" for i, m in enumerate(matches)}


def parse(path):
    entries = {}
    for element in ET.parse(path).getroot():
        if element.tag not in TAGS:
            continue
        args = format_args("".join(element.itertext())) if element.tag == "string" else set()
        entries[f"{element.tag}/{element.get('name')}"] = (element.get("translatable") != "false", args)
    return entries


def main():
    base = parse(RES / "values/strings.xml")
    translatable = {key for key, (is_translatable, _) in base.items() if is_translatable}
    problems = []
    for path in sorted(RES.glob("values-*/strings.xml")):
        locale_name = path.parent.name
        locale = parse(path)
        for key in sorted(translatable - locale.keys()):
            problems.append(f"{locale_name}: missing {key}")
        for key in sorted(locale.keys() - base.keys()):
            problems.append(f"{locale_name}: {key} is not in values/strings.xml")
        for key, (_, args) in locale.items():
            if key in base and base[key][1] != args:
                problems.append(f"{locale_name}: {key} format args {sorted(args)} != base {sorted(base[key][1])}")
    if problems:
        print("String resource parity problems:\n  " + "\n  ".join(problems))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
