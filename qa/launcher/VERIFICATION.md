# Personal builder and launcher verification

Verified on 2026-10-12 for `feature/familiar-workspace`, after the published UI
checkpoint `beb8888fe67e9f2297e8232963ff6f12b8a8a6d1`. This record covers the new
builder/launcher and opt-in offline GUI startup guard. Earlier UI, Search and
Private Evidence evidence is preserved in its separate reports.

## Passed

| Check | Evidence/result |
| --- | --- |
| Launcher offline tests | 17 passed; isolation, safe flags, quoting, repeat/fresh profiles, profile/path locks, download/cache integrity and failed-build preservation |
| Core bootstrap regression | Existing `ZapBootstrapUnitTest`: 5 passed, zero failures/errors/skips |
| Core quality | `spotlessJavaCheck`, `spotlessLicenseCheck`, `spotlessPropertiesCheck` and `japicmp` passed |
| First builder invocation | Downloaded official Call Home/Common Library/Network packages, verified upstream SHA-256 pins, packaged core and Private Evidence serially |
| Cached builder invocation | `build --offline` succeeded with the verified add-ons and populated Gradle cache |
| Fresh source assembly | New checkout with pending builder changes applied, no project build outputs or source plugins; path contained spaces; cached offline build succeeded |
| Add-on quality/package | Fresh checkout executed formatting checks, 23 unit/workflow tests and 1 packaged-add-on smoke, all passed; the unchanged primary checkout reused valid up-to-date results |
| Actual upstream launcher | Six normal starts/closes: twice on JDK 17, twice on JDK 21, twice from the fresh relocated source build |
| Safe startup | All six had zero TCP sockets and zero packets in the isolated network namespace; startup guard logged, Private Evidence loaded, no ERROR/FATAL |
| Recovery/settings | Reopen retained Safe mode, Traffic review layout, update/API/session settings and profile-local CA; timestamped sessions remained under the private profile |
| Duplicate launch | Live inherited profile lock refused a second launcher for the same profile |
| Packaging/privacy | Exactly four intended add-ons; licenses/notices preserved; runtime hashes checked; profiles, binaries, caches and raw QA evidence excluded from Git |
| Static/CLI checks | Python tests, Bash syntax, help/common prerequisite errors and `git diff --check` passed |

Core Gradle ran with a 1 GiB heap, 384 MiB metaspace, two visible processors,
one worker and no parallel tasks. Add-on Gradle used 512 MiB/256 MiB, its tests
192 MiB. GUI processes used 640 MiB and two processors. Jobs were serial; host
memory available during the final checks was approximately 6–8 GiB. FE8 was
left paused and untouched.

The primary verified `jarWithBom` SHA-256 was
`457e21ee4f45f8f01f9d76c0b6584d1e4234828eb8d22402562aa3479603ced4`.
This is a local development artifact, not a release or a claim of byte-for-byte
reproducibility across generated SBOM metadata. Each runtime has its own local
`BUILD-METADATA.json` integrity record.

Official add-on pins came from `zap/src/main/main-add-ons.yml`:

- Call Home 0.20.0: `a9e499dfa9f2fc85992faba66ab90db48d46b47e4241dc60106c78094412a679`
- Common Library 1.39.0: `477af158c562770951abedefb5bb988fda935ef253a80616c3e298174fc58fb0`
- Network 0.25.0: `bf32c4d2c40c76171efa29da1987375a312cc09595ece29d67018e7ae54d6cf3`

Private Evidence remains version 0.1.2. The unchanged primary package hash is
`04d1fb20a8e364af0db0bd0e0e80d0559da98b94398f7d53c777f54912b15d22`.

## Resolved failures and corrected assumptions

The first desktop harness timed out while handling ZAP's initial persistence
prompt. New private profiles now use the existing timestamped persisted-session
option; final desktop checks closed normally and reopened successfully.

A subsequent startup inspection found a localhost:8080 listener: Network 0.25
forces its main proxy enabled, ignoring `mainProxy.enabled=false`. The old
harness's zero-packet evidence never proved listener absence. The wrapper now
sets `org.zaproxy.zap.personal.offline=true`; the small GUI-only guard skips
extension command-line startup, where Network starts listeners. The final
harness asserts TCP absence after completed startup. Ordinary launches without
the property retain their existing command-line execution path.

The first core-check invocation used a nonexistent properties-task name and
failed during task selection. The corrected `spotlessPropertiesCheck` invocation
passed. No final required check in this bounded scope remains failed.

## Unrun and limits

- A completely empty Gradle/Maven cache download was not repeated. Official
  add-on downloads were real and fresh; source assembly reused the existing
  verified Gradle cache. Offline mode requires a previously cached wrapper too.
- macOS, Windows, non-X11 desktops, Java versions beyond 17/21, screen readers,
  high-DPI and normal production-user profiles remain unrun.
- Full upstream CI/scanner/target traffic suites were not run. Existing tests
  were preserved. No target was contacted and no vulnerability was reproduced.
- These are process startup defaults, not an OS network sandbox. Interactive
  upstream tools or later installed add-ons retain their own behavior.
- Exact publication SHA and GitHub CI availability are checked separately after
  committing/pushing. Local results do not establish GitHub CI success.

## Reproduce

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s qa/launcher -v
./zap-personal build --offline
qa/launcher/run-desktop-qa.sh /absolute/path/to/fork /absolute/path/to/full-jdk \
  /absolute/path/to/new-qa-output
```

The GUI harness additionally requires `unshare`, `ip`, Xvfb, Openbox, xdotool,
ImageMagick `import`, ripgrep and permission to create an unprivileged network
namespace. It creates a new QA-only named profile and reopens it, preserving all
data. Production launching does not need these QA utilities or namespaces.

Raw local evidence is under `../qa/launcher-*`: build/check logs, unit XML,
desktop screenshots, before/after namespace counters, TCP inventories and saved
QA configurations. These include profile-local CA material and remain outside
Git and deliverables. The fresh source checkout is QA-only; the user's development
checkout remains the original isolated fork workspace.
