#!/usr/bin/env python3
"""Add, update, remove, rename, or look up string resources in every strings.xml.

    python3 scripts/add_strings.py strings.json [--after EXISTING_KEY] [--allow-same KEY ...]
    python3 scripts/add_strings.py --show KEY_OR_TEXT
    python3 scripts/add_strings.py --remove KEY [KEY ...] [--force]
    python3 scripts/add_strings.py --rename OLD NEW

strings.json maps each key to its text per locale. "en" is values/strings.xml; the other
locales are the values-<locale> folder suffixes (de, pt-rBR, zh-rCN, ...). Every key needs
every locale. Give plain text: the script escapes &, <, >, ' and " for Android. Escapes that
are already there (\\n, \\u2019, \\') are kept.

    {"reminders_snooze_title": {"en": "Snooze", "de": "Schlummern", ...},
     "reminders_count": {"en": {"one": "%d reminder", "other": "%d reminders"},
                         "ru": {"one": "...", "few": "...", "many": "...", "other": "..."}, ...},
     "support_url": {"en": "https://example.com", "translatable": false}}

A value that is an object of quantities makes a <plurals>; each locale needs the quantities
in check_strings.PLURAL_QUANTITIES. "translatable": false writes the key to values/strings.xml
only. Nothing is written if a locale is missing, a translation is empty or "...", format
arguments differ from English, or a translation equals the English text (pass --allow-same KEY
when that is right, such as a brand name).

Existing keys are replaced in place. New keys go after --after (or before </resources>) in
the base file, in JSON order. In each locale a new key goes after the nearest preceding base
key that the locale already has, because locale files are not in the same order as the base.
<string-array> is not handled; edit it by hand.

--remove deletes keys from every file and refuses while code or resources still reference
them (--force skips that). --rename renames a key in every file and updates R.string /
R.plurals / @string / @plurals references under app/src.
"""
import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from check_strings import PLURAL_QUANTITIES, format_args

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
SOURCES = ROOT / "app/src"
ELEMENT = r'^([ \t]*)<(string|plurals|string-array) name="{key}"[^>]*>.*?</\2>[ \t]*\n'
QUANTITY_ORDER = ["zero", "one", "two", "few", "many", "other"]
PLACEHOLDERS = {"", "...", "…", "todo", "tbd"}


def escape(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r"(?<!\\)'", r"\\'", text)
    text = re.sub(r'(?<!\\)"', r'\\"', text)
    return "\\" + text if text.startswith(("@", "?")) else text


def element(key, value, translatable=True):
    attr = '' if translatable else ' translatable="false"'
    if isinstance(value, dict):
        items = "".join(
            f'\n    <item quantity="{q}">{escape(value[q])}</item>' for q in QUANTITY_ORDER if q in value
        )
        return f'<plurals name="{key}"{attr}>{items}\n</plurals>'
    return f'<string name="{key}"{attr}>{escape(value)}</string>'


def find(text, key):
    return re.search(ELEMENT.format(key=re.escape(key)), text, re.MULTILINE | re.DOTALL)


def keys_in(text):
    return re.findall(r'<(?:string|plurals|string-array) name="([^"]+)"', text)


def upsert(text, key, markup, anchor):
    """Replace key if present, else insert it after anchor (or before </resources>)."""
    existing = find(text, key)
    indent = (existing or (find(text, anchor) if anchor else None))
    markup = markup.replace("\n", "\n" + (indent.group(1) if indent else "    "))
    if existing:
        return text[: existing.start()] + f"{existing.group(1)}{markup}\n" + text[existing.end():]
    match = find(text, anchor) if anchor else None
    if match:
        return text[: match.end()] + f"{match.group(1)}{markup}\n" + text[match.end():]
    end = text.rindex("</resources>")
    return text[:end] + f"    {markup}\n" + text[end:]


def resource_files():
    files = {"en": RES / "values/strings.xml"}
    files.update({p.parent.name[len("values-"):]: p for p in sorted(RES.glob("values-*/strings.xml"))})
    return files


def write_all(files, outputs):
    for locale, text in outputs.items():
        try:
            ET.fromstring(text.encode("utf-8"))
        except ET.ParseError as error:
            print(f"Nothing written: {files[locale]} would not parse ({error})")
            return False
    for locale, text in outputs.items():
        files[locale].write_text(text, encoding="utf-8")
    return True


def args_of(value):
    if isinstance(value, dict):
        return frozenset().union(*(format_args(v) for v in value.values()))
    return format_args(value)


def text_problems(key, locale, value, english, allow_same):
    """Placeholder, plural-quantity, format-argument and copied-English problems for one value."""
    problems = []
    if isinstance(english, dict) != isinstance(value, dict):
        return [f"{key}/{locale}: must be {'quantities' if isinstance(english, dict) else 'text'} like en"]
    values = value.values() if isinstance(value, dict) else [value]
    if any(not isinstance(v, str) or v.strip().lower() in PLACEHOLDERS for v in values):
        problems.append(f"{key}/{locale}: empty or placeholder text")
        return problems
    if isinstance(value, dict):
        unknown = value.keys() - set(QUANTITY_ORDER)
        missing = PLURAL_QUANTITIES.get(locale, {"other"}) - value.keys()
        if unknown:
            problems.append(f"{key}/{locale}: unknown quantities {sorted(unknown)}")
        if missing:
            problems.append(f"{key}/{locale}: missing quantities {sorted(missing)}")
        base_args = args_of(english)
        for quantity, text in value.items():
            if not format_args(text) <= base_args:
                problems.append(f"{key}/{locale} '{quantity}': args not in en")
        if "other" in value and format_args(value["other"]) != base_args:
            problems.append(f"{key}/{locale} 'other': args differ from en")
    elif format_args(value) != format_args(english):
        problems.append(f"{key}/{locale}: format args differ from en")
    if locale != "en" and value == english and key not in allow_same and re.search(r"[A-Za-z]{3}", json.dumps(value)):
        problems.append(f"{key}/{locale}: same as English (translate it, mark it \"translatable\": false, or --allow-same {key})")
    return problems


def add(args):
    entries = json.loads(Path(args.json_file).read_text(encoding="utf-8"))
    files = resource_files()
    allow_same = set(args.allow_same or [])
    problems = []
    for key, texts in entries.items():
        translatable = texts.pop("translatable", True)
        if "en" not in texts:
            problems.append(f"{key}: missing en")
            continue
        locales = set(files) if translatable else {"en"}
        if set(texts) - locales:
            problems.append(f"{key}: unexpected locale {', '.join(sorted(set(texts) - locales))}")
        if locales - set(texts):
            problems.append(f"{key}: missing {', '.join(sorted(locales - set(texts)))}")
        for locale in sorted(locales & set(texts)):
            problems += text_problems(key, locale, texts[locale], texts["en"], allow_same)
        entries[key] = (texts, translatable)
    base = files["en"].read_text(encoding="utf-8")
    if args.after and not find(base, args.after):
        problems.append(f"--after key {args.after} is not in values/strings.xml")
    if problems:
        print("Nothing written:\n  " + "\n  ".join(problems))
        return 1

    anchor = args.after
    for key, (texts, translatable) in entries.items():
        new = not find(base, key)
        base = upsert(base, key, element(key, texts["en"], translatable), anchor if new else None)
        anchor = key if new else anchor
    base_order = keys_in(base)

    outputs = {"en": base}
    for locale, path in files.items():
        if locale == "en":
            continue
        text = path.read_text(encoding="utf-8")
        for key, (texts, translatable) in entries.items():
            if not translatable:
                text = remove_key(text, key)
                continue
            present = set(keys_in(text))
            index = base_order.index(key)
            previous = next((k for k in reversed(base_order[:index]) if k in present), None)
            text = upsert(text, key, element(key, texts[locale]), previous)
        outputs[locale] = text
    if not write_all(files, outputs):
        return 1
    print(f"Wrote {len(entries)} key(s) to {len(outputs)} files.")
    return 0


def remove_key(text, key):
    match = find(text, key)
    return text[: match.start()] + text[match.end():] if match else text


def references(key):
    """Source and resource files (other than strings.xml) that reference key."""
    pattern = re.compile(rf"(?:\bR\.(?:string|plurals|array)\.|@(?:string|plurals|array)/){re.escape(key)}\b")
    hits = []
    for path in SOURCES.rglob("*"):
        if path.suffix not in {".kt", ".java", ".xml"} or path.name == "strings.xml" or not path.is_file():
            continue
        if pattern.search(path.read_text(encoding="utf-8", errors="ignore")):
            hits.append(path)
    return pattern, hits


def remove(args):
    files = resource_files()
    base = files["en"].read_text(encoding="utf-8")
    problems = [f"{key} is not in values/strings.xml" for key in args.remove if not find(base, key)]
    if not args.force:
        for key in args.remove:
            for path in references(key)[1]:
                problems.append(f"{key} is still referenced in {path.relative_to(ROOT)} (--force to remove anyway)")
    if problems:
        print("Nothing written:\n  " + "\n  ".join(problems))
        return 1
    outputs = {}
    for locale, path in files.items():
        text = path.read_text(encoding="utf-8")
        for key in args.remove:
            text = remove_key(text, key)
        outputs[locale] = text
    if not write_all(files, outputs):
        return 1
    print(f"Removed {len(args.remove)} key(s) from {len(outputs)} files.")
    return 0


def rename(args):
    old, new = args.rename
    files = resource_files()
    base = files["en"].read_text(encoding="utf-8")
    if not find(base, old) or find(base, new):
        print(f"Nothing written: {old} must exist and {new} must not exist in values/strings.xml")
        return 1
    name = re.compile(rf'(<(?:string|plurals|string-array) name="){re.escape(old)}(")')
    outputs = {locale: name.sub(rf"\g<1>{new}\g<2>", path.read_text(encoding="utf-8")) for locale, path in files.items()}
    if not write_all(files, outputs):
        return 1
    pattern, hits = references(old)
    for path in hits:
        text = path.read_text(encoding="utf-8")
        path.write_text(pattern.sub(lambda m: m.group(0)[: -len(old)] + new, text), encoding="utf-8")
        print(f"Updated references in {path.relative_to(ROOT)}")
    print(f"Renamed {old} to {new} in {len(outputs)} files.")
    return 0


def show(args):
    files = resource_files()
    roots = {locale: ET.parse(path).getroot() for locale, path in files.items()}
    by_name = {e.get("name"): e for e in roots["en"]}

    def text(e):
        if e.tag == "plurals":
            return "  ".join(f"[{i.get('quantity')}] {''.join(i.itertext())}" for i in e)
        return "".join(e.itertext())

    query = args.show
    if query in by_name:
        for locale, root in roots.items():
            match = next((e for e in root if e.get("name") == query), None)
            print(f"{locale:7} {text(match) if match is not None else '(missing)'}")
        return 0
    matches = [e for e in roots["en"] if e.get("name") and query.lower() in text(e).lower()]
    for e in matches[:40]:
        print(f"{e.get('name')}: {text(e)}")
    if len(matches) > 40:
        print(f"... {len(matches) - 40} more")
    if not matches:
        print(f"No key or English text matches {query!r}")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument("json_file", nargs="?")
    action.add_argument("--show", metavar="KEY_OR_TEXT", help="print a key in every locale, or keys whose English text matches")
    action.add_argument("--remove", nargs="+", metavar="KEY", help="delete keys from every strings.xml")
    action.add_argument("--rename", nargs=2, metavar=("OLD", "NEW"), help="rename a key and its references")
    parser.add_argument("--after", help="existing base key to insert new keys after")
    parser.add_argument("--allow-same", nargs="+", metavar="KEY", help="keys whose translations may equal English")
    parser.add_argument("--force", action="store_true", help="--remove even if the key is still referenced")
    args = parser.parse_args()
    if args.show:
        return show(args)
    if args.remove:
        return remove(args)
    if args.rename:
        return rename(args)
    return add(args)


if __name__ == "__main__":
    sys.exit(main())
