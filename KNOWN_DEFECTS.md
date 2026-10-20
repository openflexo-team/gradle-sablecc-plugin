# gradle-sablecc-plugin — Known defects

Bugs known and not fixed yet: symptom, what is verified, how to reproduce. Features go to `BACKLOG.md`.

Identifiers are stable and never reused: `SABLECC-D-<n>`. Status legend: `TODO` · `IN PROGRESS` · `BLOCKED` ·
`DONE` · `DEFERRED`.

---

### SABLECC-D-1 — `generateGrammarSource` fails one run out of two  ·  `TODO`

**Symptom.** Run again on a module that already holds its generated sources, `generateGrammarSource` fails with a
SableCC error: `The symbol EOF aready exists.` (rhapsody-ta-parser) or `N >= 0` (fml-parser, b-ta-parser,
dsl-ta-parser, with N a number). The failed run leaves an empty output, so the next run succeeds. The pattern
alternates.

**Verified (2026-10-19, openflexo-dev composite, Java 17, Gradle 8.14.3).**
- It shows whenever the task is not up to date, for instance after the build plugin changed, and fails the
  aggregate `./gradlew compile` (the modules that depend on the parser are skipped).
- `fml-parser` alone succeeded on the first try; the other three needed a second run.
- `-x :<module>:generateGrammarSource` is a workaround when the generated sources are present.

**Not verified.** The cause: a stale output directory that the task does not clean before SableCC runs, versus
the SableCC invocation itself.

**Reproduce.** `./gradlew :openflexo-rhapsody:rhapsody-ta-parser:generateGrammarSource` from `openflexo-dev`,
twice in a row, after forcing the task to run (`--rerun-tasks` on that task).
