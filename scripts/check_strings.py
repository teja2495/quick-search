#!/usr/bin/env python3
"""Fast string-resource parity check (no Gradle).

Mirrors app/src/test/.../StringResourceParityTest.kt, which stays the authoritative check.
Exits 1 and prints problems when a locale:
  - is missing a translatable key, or carries a key that is gone from values/strings.xml,
  - carries a key marked translatable="false" in values/strings.xml,
  - uses different format arguments than the base string,
  - has a <plurals> missing a quantity its language needs (PLURAL_QUANTITIES), or a plural
    item with format arguments the base plural doesn't have. Only "other" must keep every
    base argument; an item for an exact number (Arabic "one", "two") may spell it out.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
TAGS = {"string", "plurals", "string-array"}
FORMAT_ARG = re.compile(r"%(?:(\d+)\$)?[-#+0,(]*\d*(?:\.\d+)?([a-zA-Z%])")

# Quantities each locale's grammar needs for integer counts (CLDR plural rules). Android
# falls back to "other" when one is missing, which is wrong grammar in these languages.
# A new locale must be added here; check_strings.py fails until it is.
PLURAL_QUANTITIES = {
    "en": {"one", "other"},
    "ar": {"zero", "one", "two", "few", "many", "other"},
    "de": {"one", "other"},
    "el": {"one", "other"},
    "es": {"one", "other"},
    "fr": {"one", "other"},
    "hi": {"one", "other"},
    "in": {"other"},
    "it": {"one", "other"},
    "ja": {"other"},
    "nl": {"one", "other"},
    "pl": {"one", "few", "many", "other"},
    "pt-rBR": {"one", "other"},
    "ru": {"one", "few", "many", "other"},
    "te": {"one", "other"},
    "tr": {"one", "other"},
    "zh-rCN": {"other"},
}


def format_args(text):
    matches = [m for m in FORMAT_ARG.finditer(text) if m.group(2) != "%"]
    return frozenset(f"{m.group(1) or i + 1}:{m.group(2).lower()}" for i, m in enumerate(matches))


def parse(path):
    """Maps "tag/name" to (translatable, args, plural items {quantity: args})."""
    entries = {}
    for element in ET.parse(path).getroot():
        if element.tag not in TAGS:
            continue
        items = {}
        if element.tag == "plurals":
            items = {item.get("quantity"): format_args("".join(item.itertext())) for item in element}
        args = format_args("".join(element.itertext())) if element.tag == "string" else frozenset()
        args = frozenset().union(*items.values()) if items else args
        entries[f"{element.tag}/{element.get('name')}"] = (element.get("translatable") != "false", args, items)
    return entries


def plural_problems(locale_name, key, items, base_args):
    problems = []
    locale = locale_name[len("values-"):] if locale_name != "values" else "en"
    missing = PLURAL_QUANTITIES.get(locale, set()) - items.keys()
    if missing:
        problems.append(f"{locale_name}: {key} missing quantities {sorted(missing)}")
    for quantity, args in items.items():
        if not args <= base_args:
            problems.append(f"{locale_name}: {key} '{quantity}' has args {sorted(args - base_args)} not in base")
    if "other" in items and items["other"] != base_args:
        problems.append(f"{locale_name}: {key} 'other' args {sorted(items['other'])} != base {sorted(base_args)}")
    return problems


def main():
    base = parse(RES / "values/strings.xml")
    translatable = {key for key, (is_translatable, _, _) in base.items() if is_translatable}
    problems = []
    locale_files = sorted(RES.glob("values-*/strings.xml"))
    for path in locale_files:
        locale = path.parent.name[len("values-"):]
        if locale not in PLURAL_QUANTITIES:
            problems.append(f"values-{locale}: add its plural quantities to PLURAL_QUANTITIES")
    for key, (_, args, items) in base.items():
        if items:
            problems += plural_problems("values", key, items, args)
    for path in locale_files:
        locale_name = path.parent.name
        locale = parse(path)
        for key in sorted(translatable - locale.keys()):
            problems.append(f"{locale_name}: missing {key}")
        for key in sorted(locale.keys() - base.keys()):
            problems.append(f"{locale_name}: {key} is not in values/strings.xml")
        for key in sorted(locale.keys() & (base.keys() - translatable)):
            problems.append(f"{locale_name}: {key} is translatable=\"false\" in values/strings.xml")
        for key, (_, args, items) in locale.items():
            if key not in base:
                continue
            if key.startswith("plurals/"):
                problems += plural_problems(locale_name, key, items, base[key][1])
            elif base[key][1] != args:
                problems.append(f"{locale_name}: {key} format args {sorted(args)} != base {sorted(base[key][1])}")
    if problems:
        print("String resource parity problems:\n  " + "\n  ".join(problems))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
