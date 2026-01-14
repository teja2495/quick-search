---
trigger: always_on
---

- After code changes, check for compilation errors using this command: 
./gradlew compileDebugKotlin compileDebugJavaWithJavac

- Do not include code in the task summary.
- Follow existing code architecture and UI design patterns when writing new code.
- Try to avoid unnecessary complex logic unless its really needed.
- The code you write should be readable, easy to scale and maintain, do not over-engineer.
- Do not add unnecessary comments.
- Do not hardcode user-facing strings, put them in strings.xml; keep non-UI constants inline.
- Organize new files in their respective folders.

- When building new features, Refer to AI_CONTEXT.md & DESIGN_SYSTEM.md to get an understanding of the project. 