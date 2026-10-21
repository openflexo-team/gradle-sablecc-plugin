# gradle-sablecc-plugin — Known defects

Bugs known and not fixed yet: symptom, what is verified, how to reproduce. Features go to `BACKLOG.md`.

Identifiers are stable and never reused: `SABLECC-D-<n>`. Status legend: `TODO` · `IN PROGRESS` · `BLOCKED` ·
`DONE` · `DEFERRED`.

---

### SABLECC-D-1 — `generateGrammarSource` fails one run out of two  ·  `DONE`

**Symptom.** Run again on a module that already holds its generated sources, `generateGrammarSource` fails with a
SableCC error: `The symbol EOF aready exists.` (rhapsody-ta-parser) or `N >= 0` (fml-parser, b-ta-parser,
dsl-ta-parser, with N a number). The failed run leaves an empty output, so the next run succeeds. The pattern
alternates.

**Verified (2026-10-19, openflexo-dev composite, Java 17, Gradle 8.14.3).**
- It shows whenever the task is not up to date, for instance after the build plugin changed, and fails the
  aggregate `./gradlew compile` (the modules that depend on the parser are skipped).
- `fml-parser` alone succeeded on the first try; the other three needed a second run.
- `-x :<module>:generateGrammarSource` is a workaround when the generated sources are present.

**Cause (found 2026-10-21).** SableCC keeps static state and the task called it in the JVM of the Gradle daemon, through the class
loader of the plugin, which the daemon reuses: a second grammar processed in the same JVM (another module, or the same one again)
failed. **Fixed** in 1.4.0: each grammar is processed by SableCC loaded in a class loader of its own. Verified: the six grammars of the
workspace regenerated twice in a row in one daemon (`--rerun-tasks`), both runs green.

**Reproduce.** `./gradlew :openflexo-rhapsody:rhapsody-ta-parser:generateGrammarSource` from `openflexo-dev`,
twice in a row, after forcing the task to run (`--rerun-tasks` on that task).
