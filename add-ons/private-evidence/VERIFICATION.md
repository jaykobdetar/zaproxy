# Verification record — 2026-10-12

## Passed

- Fork creation and follow-up GitHub read: `jaykobdetar/zaproxy` is a fork of
  `zaproxy/zaproxy`, default branch `main`. Authenticated owner verified as
  `jaykobdetar` (ID 212103200).
- The consumer-local Library guide exists, is readable, is 40,835 bytes, and
  retains its Library identity/version metadata. Its SHA-256 is recorded in the
  development plan.
- Add-on compile, AOSP Java/Kotlin formatting checks, and packaging passed with
  the official `org.zaproxy.add-on` plugin 0.13.1.
- **15 unit/workflow tests: 0 failures, 0 errors, 0 skipped.**
  Includes raw secret omission, path disclosure boundaries, source preservation,
  snapshot stability, absent responses, safe metadata comparison, literal/Unicode
  search, actual Swing search/tab actions and default privacy controls,
  exact UTF-8 writes, restrictive POSIX permissions, file/symlink conflicts,
  concurrent publication, staging cleanup and missing-parent failures.
- **1 package smoke test: 0 failures, 0 errors, 0 skipped.**
  ZAP's `AddOn` parses the archive and its actual `AddOnClassLoader` loads the
  extension and message bundle. Headless hook execution registers no proxy or
  session listeners. License and notice entries exist.
- Package content inspection: only the add-on classes/resources, manifest,
  license and notice are bundled. The packaged license exactly matches the
  unchanged upstream LICENSE.
- Staged changes pass `git diff --cached --check`; existing core source, tests,
  license and attribution are unchanged.

Final package:

- `build/zapAddOn/bin/privateevidence-alpha-0.1.0.zap`
- Size: **24,164 bytes**
- SHA-256: `9e3d4e5a0f84dc1a873fa04763fc1bc6db1d2be4a587c0b6f291b6314dc00938`
- Requires ZAP **2.17.0+**, status **alpha**.

The final verification command was:

```bash
JAVA_HOME=/home/jaykob/Documents/Codex/2026-10-12/task/toolchain/jdk-17.0.20.1+1 \
GRADLE_USER_HOME=/home/jaykob/Documents/Codex/2026-10-12/task/.gradle-private-evidence \
nice -n 10 ./gradlew -p add-ons/private-evidence --offline \
  --no-daemon --max-workers=1 --no-parallel \
  spotlessApply spotlessCheck test packageSmokeTest
```

Gradle's required local IPC sockets were allowed for this offline command.
It succeeded in 10 seconds after caching dependencies. Tests used synthetic
messages and local files only. No target requests or scanner runs occurred.

## Failed attempts, resolved

1. The first compilation attempt stopped before compiling: installed Java 21
   had no compiler, and the available system compiler was Java 8. Formatting
   passed. Used a task-local official Temurin JDK 17.0.20.1+1; its archive matched
   SHA-256 `3808d1d15e3ec6bd5b84057fb5d84c33d8a1536a258146bcea2e603fc726e08e`.
   System Java and security settings were not changed.
2. An offline build under restricted network sandboxing could not initialize
   Gradle's local file-lock sockets (`Could not determine a usable wildcard IP`).
   No tests ran in that attempt. The supported approval path allowed local IPC;
   the same bounded offline build then passed.
3. A new Swing workflow test initially clicked a scrollbar button instead of
   **Find Next**, producing one assertion failure. Corrected its selector to use
   the translated action label; the final test checks Enter, Find Next and
   comparison-tab selection and passes.

## Unrun checks and limits

- Full upstream core suite and distribution builds: core was not modified, and
  the standalone add-on is built/tested independently.
- Manual installation and end-to-end dialog use in a running complete ZAP
  desktop; high-DPI, screen-reader, Windows and macOS QA.
- Live traffic, active scans, vulnerability reproduction, and offensive
  roadmap features were not run and are outside scope.
- Filesystems without hard-link support fail closed on export. Non-POSIX
  file permissions follow OS/filesystem defaults. Power-loss durability is not
  claimed; interrupted staging may leave a private temporary file.
- Exported status, media types and byte counts can identify activity. URL paths
  can contain personal data and require explicit opt-in. Review before sharing.
  Equality of omitted summary fields does not establish equality of raw traffic.

FE8 pause was confirmed before build work. Heavy jobs ran serially with one
Gradle worker, 512 MiB heap/256 MiB metaspace, and one 192 MiB test JVM. Host
available memory stayed around 9 GiB during verification. No FE8 processes,
checkout, saved analysis or commits were modified or resumed.

Implementation commits remain local on `feature/private-evidence`. No
implementation was pushed, no PR was opened, and no release was published.
