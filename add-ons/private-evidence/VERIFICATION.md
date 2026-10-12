# Verification record — 2026-10-12

## Current result: 0.1.2

- **23 unit/workflow tests and 1 package smoke test passed**, with zero failures,
  errors or skips. Final formatting/package checks passed; valid up-to-date unit
  results were reused after QA-only changes.
- **44 desktop checks passed** in isolated official ZAP 2.17.0, including four
  checks for a popup retained across a session transition. The final independent
  review found no remaining correctness blocker in the requested areas.
- Size **26,703 bytes**; SHA-256
  `04d1fb20a8e364af0db0bd0e0e80d0559da98b94398f7d53c777f54912b15d22`.
- An additional review found that 0.1.1 obtained its revision at activation, after
  the popup had cached its selection. A session change before activation could
  therefore give an old lazy selection a new token. Version 0.1.2 captures the
  revision during enablement, rejects stale activation before stored reads, and
  carries that token through preparation/publication.
- The corrected lazy fixture failed against retained 0.1.1 and passed against
  0.1.2. An earlier fixture incorrectly used a constructor that copied the list;
  that attempt is inconclusive and preserved separately. The existing menu test
  was extended for this verified coverage gap; no unrelated tests were added.
- Evidence is under `../qa/release-checks/private-evidence-012-final-desktop/`
  relative to the fork root. Cross-platform build and combined-runtime checks
  are recorded in [the final fork verification](../../docs/personal-fork-verification.md).

## Prior result: 0.1.1 (superseded)

- **23 unit/workflow tests and 1 package smoke test passed:** no failures,
  errors or skips. Formatting, compilation and packaging passed with the same
  bounded offline Gradle command below.
- **40 actual desktop checks passed** in official ZAP 2.17.0 with the packaged
  add-on, driven programmatically through the visible Swing UI on Xvfb.
- Current archive: `build/zapAddOn/bin/privateevidence-alpha-0.1.1.zap`.
  Size **26,606 bytes**; SHA-256
  `4fd18ac342260455b7cadc179dd0214f8ac78ed448734feebdd6913acbfc4f80`.
- The official runtime archive matched SHA-256
  `94c8f767b1c2e94f0db66b3ae56514d5e3f5a728ee1b6c798e0c8fe2d61fbff0`.
  Only mandatory Network/Call Home add-ons and Private Evidence were loaded.
- Desktop checks covered package/menu/translation loading in the app; preview
  omissions; path opt-in and CONNECT authorities; search/comparison; absent vs.
  unrecognized statuses; canceled/repeated export and exact saved bytes; close/
  reopen defaults; the 100-message cap; Private Evidence's own lazy enablement;
  stored reads off the UI thread; real session changes and failed-save recovery.
- Final run completed in about 11 seconds with a 640 MiB heap. Build and runtime
  jobs ran serially, with roughly 9 GiB host memory available.

The preserved optional Linux desktop harness is `qa/DesktopQa.java` and
`qa/run-desktop-qa.sh`. Final local evidence is in
`/home/jaykob/Documents/Codex/2026-10-12/task/qa/final-desktop-render/`, including
`runtime.log`, the namespace inventory and `results/desktop-checks.txt`.
The namespace had only loopback; API, update checks and proxy settings were
disabled in the disposable profile, and silent mode prevented unsolicited
requests. The harness sent no HTTP requests. Profile, Java home/preferences,
virtual display and saved sessions were task-local. ZAP generated a normal
profile-local root CA; it was never installed in a browser or system trust store.

## Independent review and follow-up

A separate read-only reviewer inspected the privacy/UI batch without edits or
builds. It identified three confirmed issues:

1. Generic reparsing could treat a CONNECT authority without a port as a path.
   Fixed by using the upstream path component and omitting CONNECT paths.
2. The inherited enablement check iterated lazy stored messages on the EDT.
   Private Evidence now validates count without retrieving bodies. Other
   unchanged upstream handlers can still load messages when the overall context
   menu is opened; this fix makes no claim about their behavior.
3. Unknown three-digit response statuses were labeled as absent responses.
   A separate response-present flag now distinguishes omitted status from an
   actually absent response.

The review's session-reference concern was initially unverified. Desktop QA
confirmed that IDs are reused across two saved synthetic sessions and an old
reference reads replacement-session data. A subsequent controlled third-session
switch during a blocked lazy read confirmed that CaptureGuard discards the
in-flight batch, displays no stale dialog, and permits a fresh private review.
Four focused guard tests also verify capture/publication invalidation, fresh
tickets and missing-record failure.

The follow-up review found no additional concrete lock/race/privacy issue, but
identified unmatched completion events after failed session operations. The
add-on deliberately remains closed in that case and provides explicit recovery
guidance. Desktop QA produced an ordinary Save Session error using a synthetic
path whose parent was a regular file, then successfully saved to a valid new
path and verified that review was restored. Automatic recovery after every
possible failed open/save is not claimed; a timeout reset is not used.

## Additional resolved attempts and remaining limits

- Early desktop harness attempts observed the main frame before Control/menu
  initialization completed. Waiting for the registered menu corrected the QA
  readiness assumption. A notification selector was updated for ZAP's
  JTextComponent-based labels.
- A parented-notification change initially used overloads absent from
  ViewDelegate and failed compilation; it now uses parented standard Swing
  dialogs. The final compile and desktop notification/retry checks pass.
- An early lazy-fixture test invoked every upstream context-menu handler and
  blocked an unrelated unchanged handler. The packaged Private Evidence item
  is tested separately for its lazy enablement and capture behavior.
- Early network namespaces with loopback down produced expected local network
  initialization warnings. The final harness supplies isolated loopback and
  has no external interface. No target contact occurred.
- **Unrun:** Windows/macOS desktop QA; actual screen-reader and high-DPI checks;
  physical keyboard/window-manager interaction; failed-open recovery variants;
  full upstream core/distribution suite. The packed synthetic UI panel was
  rendered in the running app and visually inspected; its labels, fields and
  buttons were readable without clipping. Native Xvfb root capture was
  inconsistent without a window manager, so the harness records the actual
  Swing panel render. The desktop checks use Swing actions, not physical input.
- Filesystem/export privacy limits documented below still apply. A failed
  session operation requires a subsequent successful session operation before
  new evidence review is enabled.

## Initial 0.1.0 result (superseded)

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

## Initial unrun checks and limits

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
