---
name: localize-strings
description: Add, change, or remove user-facing strings in Quick Search across values/strings.xml and all 16 localized files. Use whenever a change touches copy in app/src/main/res/values*/strings.xml, including as part of a larger feature.
---

# Localize strings

The app ships English plus 16 locales: ar, de, el, es, fr, hi, in, it, ja, nl, pl, pt-rBR, ru, te, tr, zh-rCN. Every translatable key must exist in all of them with the same format arguments.

## Before adding a key

- Search `values/strings.xml` for the same text first. Reuse an exact match; don't add a duplicate.
- Name the key after its feature and role, like its neighbours (`settings_<feature>_title`, `settings_<feature>_desc`).
- Pick where it goes: after the closest related key in `values/strings.xml`, inside the right `<!-- === -->` section.

## Adding or changing `<string>` keys

Write a JSON file in the scratchpad, then run the helper. It writes all 17 files, or nothing if a locale is missing:

```json
{"settings_foo_title": {"en": "Foo", "ar": "...", "de": "...", "el": "...", "es": "...", "fr": "...", "hi": "...", "in": "...", "it": "...", "ja": "...", "nl": "...", "pl": "...", "pt-rBR": "...", "ru": "...", "te": "...", "tr": "...", "zh-rCN": "..."}}
```

```bash
python3 scripts/add_strings.py <file>.json --after <existing_key>
```

- Write plain text. The script escapes `'`, `"`, `&`, `<`, `>` and keeps escapes already there (`\n`, `’`). Don't pre-escape.
- Existing keys are replaced in place in every locale, so changing English copy means passing new translations too. Don't leave the old meaning in other locales.
- It handles `<string>` only. Edit `<plurals>` and `<string-array>` by hand in every file, with each locale's plural quantities (for example, ru and pl need `one`/`few`/`many`/`other`; ja and zh-rCN only need `other`).
- To remove a key, delete it from all 17 files. `check_strings.py` flags stale keys left behind.

## Translation rules

- Translate meaning and tone, not word for word. Keep UI labels as short as English; button and title text has little room.
- Keep format arguments exactly as in English (`%1$s`, `%1$d`, `%%`). You may reorder them in a sentence, but don't change their numbers or types.
- `Quick Search` (the app name) stays in English everywhere. Brand and provider names (Gemini, OpenAI, Groq, Tavily, Google, Android) stay as-is.
- Feature names *are* translated. Reuse the translation the locale already uses: grep that locale's file for the existing term (for example, `settings_at_a_glance_title` is "Auf einen Blick" in de) instead of inventing a new one.
- Match each locale's existing register: formal/informal "you" (de, fr, es, ...) and script conventions. Check a few nearby strings in that file before writing new ones.
- Settings the user can search for also need an app-setting row. See the `new-app-setting` skill.

## Check

```bash
python3 scripts/check_strings.py
```

`scripts/verify.sh` runs the same check plus `StringResourceParityTest`.
