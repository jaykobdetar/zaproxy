# Traffic review workspace verification

Date: 2026-10-12. UI branch: `feature/familiar-workspace`, based on the published
Private Evidence checkpoint `efd5ffc70dca073705a43ab82dacc21b42575c57`.
The UI batch is local and unpushed. Existing test sources were preserved.

## Passed final checks

| Check | Result |
| --- | --- |
| Java 17 compilation and core JAR | Passed |
| Selected existing view/history/HTTP-panel/Search tests | 216 tests across 22 classes; zero failures, errors or skips |
| Upstream `:zap:check` | Passed with the selected tests; Java/Kotlin formatting, license/property checks and `japicmp` passed |
| Real desktop on Java 17 | 37 synthetic offline checks passed |
| Real desktop on Java 21 | Same 37 checks passed against the same core JAR |
| Disposable profile restart on Java 17 | 3 restoration checks passed |
| Independent source and screenshot review | No blocker found; narrow-window polish limitations noted |
| QA network isolation | Only loopback present; before/after counters show zero received and transmitted packets |
| Existing disposable CA | Seed and final `rootCa` XML content hashes match; no CA trust or normal profile changes |
| QA shell syntax and patch whitespace | Passed |

The final tested core JAR SHA-256 is
`6a25f8b11f67c58feb387e18e1e4dd229f971274bee227ff25006b7594693e04`.
It is a core JAR requiring the existing runtime libraries and add-ons, not a
standalone installer or release.

Desktop checks cover the labeled preset, Display option ID 3, simultaneous
request/response viewing, history selection via the existing keyboard action,
site-tree selection, layouts at 800/1000/1200 pixels, classic/full/review
transitions, independent divider restoration, extension pin/detach/reattach and
hide state, cancellation with a detached request, light/dark theme focus,
restored classic tab styling, stored message integrity and profile restart.
The keyboard checks exercise Swing focus targets and the existing table action;
they do not establish physical-keyboard or screen-reader usability.

## Repaired and failed intermediate attempts

- A new toolbar button lost its focus target initially and again after theme
  refresh. The final transparent toolbar container fixes both; final desktop
  checks passed in both themes and both Java runtimes.
- The initial pin fixture only changed a panel flag. It was corrected to use
  the actual tab pin behavior and its saved state; final detach/pin/hide checks
  passed. This was a fixture error, not a reported runtime defect.
- An earlier combined test selection had five Search setup failures when a
  cached column label could not be found. The unchanged baseline control passed
  all 216 tests and the current Search class passed all five in isolation. The
  final 216-test run used `forkEvery = 1` to isolate static translation state and
  passed. This is a test-state limitation; no test was removed or weakened.
- Intermediate Gradle lock-socket, command-option and compile-import errors and
  QA shell quoting errors were corrected before the final runs. Earlier logs
  and failed attempts remain outside Git alongside the successful evidence.

## Remaining limitations and unrun checks

At 800 pixels, the existing site labels and History columns truncate, and the
main toolbar extends past the window's right edge. The preset and both message
viewers remain visible. A compact History column option, an optional collapsed
navigator and toolbar overflow handling are follow-up UI work.

High-DPI/fractional scaling, translated labels, screen-reader use, physical
keyboard navigation, other look and feels, Windows/macOS, normal installation,
and the full unfiltered core/GUI/API/scanner suites were not run. No scanner or
lab was run and no target was contacted. Native installed-Burp inspection was
unavailable; public official UI references were used with the user's acceptance.

## Reproduce

Use the populated task-local Gradle cache, a full JDK 17+, one Gradle worker and
bounded memory. `bounded.gradle` is QA-only and isolates test classes; it does
not change upstream test behavior outside this invocation.

```sh
./gradlew --offline --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx1g -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=2' \
  -I qa/workspace/bounded.gradle :zap:jar :zap:test \
  --tests 'org.zaproxy.zap.view.*' \
  --tests 'org.parosproxy.paros.extension.history.*' \
  --tests 'org.zaproxy.zap.extension.httppanel.*' \
  --tests 'org.zaproxy.zap.extension.search.*' :zap:check
```

Run `qa/workspace/run-desktop-qa.sh` with a minimal disposable runtime, full JDK,
core JAR, runtime libraries, existing disposable QA configuration and a new
output directory. It also compiles the shared offline QA utilities and
QA-only tab access class. Use `run-profile-restore-qa.sh` with the completed
QA directory and a new result directory to verify restart. Both scripts print
their full positional usage when invoked without arguments. Never use a normal
user profile as their seed.

Successful evidence is retained locally under `../qa/ui-workspace-final-17-02`,
`../qa/ui-workspace-final-21`, `../qa/ui-workspace-restore-17` and
`../qa/ui-workspace-final-tests`, with the final build log at
`../qa/ui-workspace-final-build-03.log`. Comparison images show classic and
review layouts in the same UI implementation and use the same synthetic data.
Profiles, certificates, databases, vendor reference images, runtime binaries
and build caches remain outside the repository.
