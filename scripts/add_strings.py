#!/usr/bin/env python3
"""Add or update <string> resources in values/strings.xml and every localized strings.xml.

Usage: python3 scripts/add_strings.py strings.json [--after EXISTING_KEY]

strings.json maps each key to its text per locale. "en" is values/strings.xml; the other
locales are the values-<locale> folder suffixes (de, pt-rBR, zh-rCN, ...). Every key needs
every locale. Give plain text: the script escapes &, <, >, ' and " for Android. Escapes that
are already there (\\n, \\u2019, \\') are kept.

    {"reminders_snooze_title": {"en": "Snooze", "de": "Schlummern", ...}}

Existing keys are replaced in place. New keys go after --after (or before </resources>) in
the base file, in JSON order. In each locale a new key goes after the nearest preceding base
key that the locale already has, because locale files are not in the same order as the base.
Handles <string> only; edit <plurals> and <string-array> by hand.
"""
import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"
ELEMENT = r'^([ \t]*)<(string|plurals|string-array) name="{key}"[^>]*>.*?</\2>[ \t]*\n'


def escape(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r"(?<!\\)'", r"\\'", text)
    text = re.sub(r'(?<!\\)"', r'\\"', text)
    return "\\" + text if text.startswith(("@", "?")) else text


def find(text, key):
    return re.search(ELEMENT.format(key=re.escape(key)), text, re.MULTILINE | re.DOTALL)


def keys_in(text):
    return re.findall(r'<(?:string|plurals|string-array) name="([^"]+)"', text)


def upsert(text, key, value, anchor):
    """Replace key if present, else insert it after anchor (or before </resources>)."""
    existing = find(text, key)
    if existing:
        line = f'{existing.group(1)}<string name="{key}">{value}</string>\n'
        return text[: existing.start()] + line + text[existing.end():]
    match = find(text, anchor) if anchor else None
    if match:
        line = f'{match.group(1)}<string name="{key}">{value}</string>\n'
        return text[: match.end()] + line + text[match.end():]
    end = text.rindex("</resources>")
    return text[:end] + f'    <string name="{key}">{value}</string>\n' + text[end:]


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("json_file")
    parser.add_argument("--after", help="existing base key to insert new keys after")
    args = parser.parse_args()

    entries = json.loads(Path(args.json_file).read_text(encoding="utf-8"))
    files = {"en": RES / "values/strings.xml"}
    files.update({p.parent.name[len("values-"):]: p for p in sorted(RES.glob("values-*/strings.xml"))})

    problems = [
        f"{key}: missing {', '.join(sorted(set(files) - set(texts)))}"
        for key, texts in entries.items()
        if set(files) - set(texts)
    ]
    problems += [
        f"{key}: unknown locale {', '.join(sorted(set(texts) - set(files)))}"
        for key, texts in entries.items()
        if set(texts) - set(files)
    ]
    base = files["en"].read_text(encoding="utf-8")
    if args.after and not find(base, args.after):
        problems.append(f"--after key {args.after} is not in values/strings.xml")
    if problems:
        print("Nothing written:\n  " + "\n  ".join(problems))
        return 1

    anchor = args.after
    for key, texts in entries.items():
        if not find(base, key):
            base = upsert(base, key, escape(texts["en"]), anchor)
            anchor = key
        else:
            base = upsert(base, key, escape(texts["en"]), None)
    base_order = keys_in(base)

    outputs = {"en": base}
    for locale, path in files.items():
        if locale == "en":
            continue
        text = path.read_text(encoding="utf-8")
        for key, texts in entries.items():
            present = set(keys_in(text))
            index = base_order.index(key)
            previous = next((k for k in reversed(base_order[:index]) if k in present), None)
            text = upsert(text, key, escape(texts[locale]), previous)
        outputs[locale] = text

    for locale, text in outputs.items():
        try:
            ET.fromstring(text.encode("utf-8"))
        except ET.ParseError as error:
            print(f"Nothing written: {files[locale]} would not parse ({error})")
            return 1
    for locale, text in outputs.items():
        files[locale].write_text(text, encoding="utf-8")
    print(f"Wrote {len(entries)} key(s) to {len(outputs)} files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
