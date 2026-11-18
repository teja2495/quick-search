---
name: localize-strings
description: Add, change, rename, or remove user-facing strings in Quick Search across values/strings.xml and all 16 localized files. Use whenever a change touches copy in app/src/main/res/values*/strings.xml, including as part of a larger feature.
---

# Localize strings

The app ships English plus one locale per `values-*/strings.xml` folder (currently 16: ar, de, el, es, fr, hi, in, it, ja, nl, pl, pt-rBR, ru, te, tr, zh-rCN). Every translatable key must exist in all of them with the same format arguments, and every `<plurals>` needs the quantities its language uses.

All edits go through `scripts/add_strings.py`. It writes all files or nothing.

## Before adding a key

- Look for the text first: `python3 scripts/add_strings.py --show "<english text>"` lists keys whose English matches. Reuse an exact match; don't add a duplicate.
- Name the key after its feature and role, like its neighbours (`settings_<feature>_title`, `settings_<feature>_desc`).
- Pick where it goes: after the closest related key in `values/strings.xml`, inside the right `<!-- === -->` section. Pass that key as `--after`.
- Decide whether it is translatable. URLs, people's names, brand and product names (Google, WhatsApp, Gemini), symbols, and strings that are only format arguments (`%1$s · %2$s`) are `"translatable": false`: they live in `values/strings.xml` only and locales fall back to them.

## Adding or changing keys

Write a JSON file in the scratchpad, then run the helper:

```json
{
  "settings_foo_title": {"en": "Foo", "ar": "...", "de": "...", "...": "every locale"},
  "foo_item_count": {
    "en": {"one": "%d item", "other": "%d items"},
    "ru": {"one": "%d элемент", "few": "%d элемента", "many": "%d элементов", "other": "%d элемента"},
    "...": "every locale, with its quantities"
  },
  "foo_help_url": {"en": "https://example.com", "translatable": false}
}
```

```bash
python3 scripts/add_strings.py <file>.json --after <existing_key>
```

- Write plain text. The script escapes `'`, `"`, `&`, `<`, `>` and keeps escapes already there (`\n`, `’`). Don't pre-escape.
- It refuses empty or `...` placeholders, mismatched format arguments, missing plural quantities, and translations identical to English. If a translation really is the same as English (a loanword), pass `--allow-same <key>`; if every locale would be identical, the key should be `"translatable": false` instead.
- Existing keys are replaced in place in every locale, so changing English copy means passing new translations too. Don't leave the old meaning in other locales.
- `<string-array>` is not handled; edit it by hand in every file.

## Plurals

Required quantities per locale are the `PLURAL_QUANTITIES` table in `scripts/check_strings.py` (mirrored in `StringResourceParityTest`):

- `one`/`other`: en, de, el, es, fr, hi, it, nl, pt-rBR, te, tr.
- `other` only: ja, zh-rCN, in.
- `one`/`few`/`many`/`other`: ru, pl. Russian: 1, 21 → `one` (день); 2–4 → `few` (дня); 5–20, 0 → `many` (дней); `other` is for fractions and usually matches `few`.
- `zero`/`one`/`two`/`few`/`many`/`other`: ar. 2 is the dual (يومان), 3–10 take the plural (أيام), 11–99 the accusative singular (يومًا), 100+ and `other` the singular (يوم). The `one` and `two` items may drop `%d` and spell the number out (مرة واحدة, مرتان); every other item, and `other` in all locales, keeps every format argument.
- Abbreviated units (`%d min`, `%d ч`) still need every quantity, with the same text in each.

## Renaming and removing

```bash
python3 scripts/add_strings.py --rename <old_key> <new_key>
```

```bash
python3 scripts/add_strings.py --remove <key> [<key> ...]
```

- `--rename` renames the key in all files and rewrites `R.string`/`R.plurals`/`@string`/`@plurals` references under `app/src`.
- `--remove` refuses while code or resources still reference the key. Remove the usage first; use `--force` only for keys that are referenced dynamically or by name.

## Translation rules

- Translate meaning and tone, not word for word. Keep UI labels as short as English; button and title text has little room.
- Keep format arguments exactly as in English (`%1$s`, `%1$d`, `%%`). You may reorder them in a sentence, but don't change their numbers or types.
- `Quick Search` (the app name) stays in English everywhere. Brand and provider names (Gemini, OpenAI, Groq, Tavily, Google, Android) stay as-is inside translated sentences.
- Feature names *are* translated. Reuse the term the locale already uses: `python3 scripts/add_strings.py --show <related_key>` prints a key in every locale (for example, `settings_at_a_glance_title` is "Auf einen Blick" in de).
- Match each locale's existing register: formal/informal "you" (de, fr, es, ...) and script conventions. Check a few nearby strings in that file before writing new ones.
- Arabic is right-to-left: don't add direction marks or reorder `%1$s` to "look right" in an editor; write the sentence in logical order and Android lays it out.
- Settings the user can search for also need an app-setting row. See the `new-app-setting` skill.

## Check

```bash
python3 scripts/check_strings.py
```

It flags missing or stale keys, keys that are `translatable="false"` in the base but present in a locale, format-argument mismatches, and plurals missing a required quantity. Adding a new `values-*` locale fails the check until its quantities are added to `PLURAL_QUANTITIES` here and in `StringResourceParityTest`. `scripts/verify.sh` always runs this check. `StringResourceParityTest` runs with the unit suite when the app's `versionCode` changes.
