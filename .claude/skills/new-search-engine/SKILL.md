---
name: new-search-engine
description: Add a built-in (SearchEngine enum-backed) search engine to Quick Search. Use when asked to add, support, or integrate a new web search engine, site search, or app search target such as Kagi, Perplexity, or a store search. Not for user-created custom engines.
---

# New built-in search engine

1. Read and follow `app/src/main/java/com/tk/quicksearch/searchEngines/new-search-engine.md`. It is the source of truth; don't work from memory.
2. The engine's name string goes in `values/strings.xml` and all 16 `values-*/strings.xml` files. Brand names stay untranslated.
3. Run `SearchEngineRegistryContractTest` first (`./gradlew :app:testStandardDebugUnitTest --tests '*SearchEngineRegistryContractTest'`), then `scripts/verify.sh`.
4. If the guide is wrong or missing a step you needed, update the guide in the same change.
