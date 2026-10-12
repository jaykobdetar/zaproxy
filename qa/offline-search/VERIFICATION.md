# Offline Search verification — 2026-10-12

## Passed

- **5 focused core UI unit tests:** zero failures, errors or skips. They cover
  literal punctuation/embedded quote terminators/Unicode, preserved regex
  behavior, empty-literal rejection, significant whitespace, and prompt/mode
  restoration. Existing upstream tests were retained. Only this test class ran.
- Core compilation, JAR packaging, `:zap:spotlessJavaCheck` and
  `:zap:spotlessLicenseCheck` passed after the final source change. Shell syntax
  and `git diff --check` passed.
- **22 actual fork desktop checks:** zero failed checks in the final run. The
  app loaded the rebuilt `2.18.0-SNAPSHOT` core and exact Gradle runtime
  dependencies, with only the mandatory Network/Call Home add-ons from the
  previously verified copied official 2.17.0 runtime.
- Real stored synthetic records verified literal punctuation, `\\E`, Unicode,
  inherited A-Z and non-ASCII case behavior, request/response filters, inverse
  matching, an empty query preserving previous results, programmatic regex mode
  restoration, navigation and unchanged source traffic.
- The checkbox is named and focusable under FlatLaf. Its actual Space action
  binding toggles the mode and updates the query prompt. Both toolbars' bounds
  fit at frame widths 800/1000/1200 pixels with a nonzero match count and
  completion status. The actual Swing panel renders were visually inspected:
  labels, input, controls and results are readable without clipping.
- Independent read-only reviews of the literal mode and layout found no blocking correctness/API/lifecycle
  issue. Review prompted more precise case-handling wording; runtime QA
  identified the focusability and toolbar-clipping fixes.

The built JAR is **5,219,891 bytes**, SHA-256
`6a0543041c2c14641f8446401a8c4730e376e6c3e655150f736f2bd49e5d3a65`.
It is a core development artifact, not an add-on or complete installer.

Final evidence is outside Git at
`/home/jaykob/Documents/Codex/2026-10-12/task/qa/literal-search-layout-final/`:
`runtime.log`, `network-namespace.txt`, `results/desktop-checks.txt`, three panel
renders and component-bound records. The unit report is
`zap/build/test-results/test/TEST-org.zaproxy.zap.extension.search.SearchPanelUnitTest.xml`.

## Isolation and resources

The Linux harness uses a fresh profile/home/preferences store, a private Xvfb
display and an isolated network namespace containing only loopback. Its final
interface inventory reported zero packets. API, proxy and update checks are
disabled; silent/Safe mode is used. The harness creates synthetic records with
the ordinary database API and never sends their requests. No scanner runs,
targets, credential changes, trust-store changes or user installation occurred.
It reuses only the earlier disposable QA configuration and profile-local CA.

Core builds were serial, with one worker, a 1 GiB Gradle heap/384 MiB metaspace,
two logical processors and one 192 MiB test JVM. Desktop heap is 640 MiB with
two processors and a 120-second overall limit. Host available memory remained
around 9 GiB; FE8 was paused and left untouched. The final desktop completed in
about 11 seconds. The QA-only init script and runner are preserved here.

The deliberately minimal runtime logs an existing missing vulnerability-provider
message because optional common-library/reporting add-ons are absent. That
fixture limitation did not affect offline search. No production distribution
qualification is claimed.

## Resolved failed attempts

- The first offline formatter check could not resolve an uncached official
  formatter. After its authorized dependency fetch, ZAP's formatter identified
  and corrected only the two changed Java files.
- The first QA-only dependency-copy initialization script also applied to
  Gradle's separate `buildSrc` project. It now skips projects without `:zap`.
- An early desktop search began during startup's database replacement and
  encountered a closed connection. The harness now waits for bootstrap
  completion and synchronously creates its own saved session. A subsequent
  startup persistence prompt caused a timeout; it is disabled only in the
  disposable QA profile.
- FlatLaf made the new toolbar checkbox non-focusable. Setting focusability
  after adding it fixed this observed accessibility issue.
- ZAP's warning uses a text component; the first empty-query selector recognized
  only labels and timed out. Correcting the selector verified unchanged results.
- The original single-row toolbar clipped at 1000 pixels. Separating search
  controls from result controls fixed this at all three tested widths. An early
  bounds assertion also required decorative separators/spacers to have nonzero
  width; their zero bounds are normal. Final containment checks and visual
  inspection verify the meaningful controls.

Earlier failed QA directories/logs are retained outside the checkout. Final
passed results supersede those attempts; failures are not counted as passed.

## Initial unrun checks and limits (see final checkpoint below)

- Full upstream test suite, GUI suite, API transport suite, binary-compatibility
  task, distribution/installer build and normal user installation.
- Physical Tab/Space input, window-manager behavior, actual screen-reader use,
  high-DPI, alternate themes, translations, Windows and macOS. Space binding was
  invoked programmatically; physical keyboard qualification is not claimed.
- All custom third-party searchers and every upstream search type. Public
  contracts were reviewed; runtime checks covered Request/Response, inverse
  and programmatic regex searches on the supplied synthetic fixture.
- Broad traffic profiling and concurrent session changes while the unchanged
  core search engine runs. This patch does not redesign the search backend or
  claim to fix all existing session races.

## Reproduce the desktop run

After the bounded core build/dependency copy in
[the setup guide](../../docs/personal-fork-setup.md), from the fork root:

```bash
bash qa/offline-search/run-desktop-qa.sh \
  ../qa/runtime/ZAP_2.17.0 ../toolchain/jdk-17.0.20.1+1 \
  zap/build/libs/zap-2.18.0-SNAPSHOT.jar zap/build/offlineSearchQa/lib \
  ../qa/final-desktop-render/profile/config.xml ../qa/new-search-check
```

Requires Linux `unshare`, `ip`, `xvfb-run`, `timeout` and a full JDK. The runner
refuses an existing output directory. Use only a disposable copied runtime with
the two mandatory add-ons and a disposable QA seed configuration; never supply
an existing personal installation/profile. Initial runtime/JDK/dependencies
were verified/prepared earlier and are outside Git.

## Final broader regression and runnable bundle

Source-code commit `42eb75c0f7b88783182a1324b4bd80866c67e6c3` also received
upstream `assemble`/`check` on full task-local JDK17 and21. Both runs passed the
same 431 selected offline cases with zero failures/errors/skips, formatting,
license/property checks and `japicmp`. The full unfiltered core suite remains
unrun. The selected filters cover Search, HTTP panels, model/session, view and
utility tests; scanner/crawler/target-traffic tests are excluded.

The final personal distribution includes exact libraries, Private Evidence
0.1.2 and unchanged pinned Network/Call Home/Common Library packages. The
existing Search harness passed 22 checks against the exact extracted archive;
the Private Evidence harness passed 44 checks there. Common Library resolves
the earlier missing-provider fixture error; final startup has no ERROR message.
The packaged upstream launcher reports `2.18.0-SNAPSHOT` in a version-only
isolated smoke run. See [the final report](../../docs/personal-fork-verification.md)
for source/artifact hashes, evidence and limits.
