# Personal ZAP fork — final report

Verified fork: https://github.com/jaykobdetar/zaproxy, owned by the authenticated
account `jaykobdetar`. This report preserves the initial implementation's local
validation evidence. After that validation, the user authorized publication and
`feature/private-evidence` was pushed at
**efd5ffc70dca073705a43ab82dacc21b42575c57** on 2026-10-12. The remote SHA was
verified; no CI runs or check runs were present for that head. No PR, merge or
release publication occurred. FE8 remained paused and untouched.

The newer UI work was published on `feature/familiar-workspace` at
`beb8888fe67e9f2297e8232963ff6f12b8a8a6d1` on 2026-10-12 after separate
validation and authorization; no exact-head CI runs or check runs were present. Its
separate scope and results are in [the workspace guide](personal-fork-workspace.md)
and `qa/workspace/VERIFICATION.md`. The test counts below concern the initial
implementation, not the later UI batch.

Verified runtime-code commit: **42eb75c0f7b88783182a1324b4bd80866c67e6c3**.
Upstream base: **38ffe34a1b03fd3241e0bb233c2a083df875ee33**.
The final local distribution's BUILD-METADATA.json records those source commits.
Later handoff changes concern documentation and QA-runner compatibility.

## Completed scope

| Area | Result |
| --- | --- |
| Private Evidence 0.1.2 | Offline preview, literal summary search, two-summary comparison and exact UTF-8 new-file export |
| Default privacy | Bodies, credentials, hosts, query/fragment, arbitrary headers and paths omitted; explicit path opt-in |
| Session provenance | Popup selection revision bound before activation; stale/in-flight captures rejected; successful-save recovery verified |
| Core Search | Optional literal mode; regex remains default; programmatic regex behavior retained |
| Accessibility/layout | Named query, inverse label association, focusable Literal control and separate results bar; 800/1000/1200-pixel layouts verified |
| Existing foundations | Core HTTP views/sessions and existing Diff, Import/Export and Reports architecture retained |
| Local runtime | Runnable custom snapshot with exact libraries, Private Evidence and unchanged pinned Network/Call Home/Common Library add-ons |
| Offensive roadmap items | Excluded; no new active probing, payloads, credential attacks, fuzzing, OAST or offensive automation |

The supplied broad Burp-alternative guide was read as reference material. The
verified gaps were narrow additions to ZAP, not grounds for a core rewrite.
The assessment and remaining priorities are in
[the development guide](personal-fork-development.md).

## Verification

| Check | Result |
| --- | --- |
| Java 17 assembly/check | Passed with 431 selected offline regression cases; zero failures/errors/skips |
| Java 21 assembly/check | Passed with the same 431 selected offline regression cases; zero failures/errors/skips |
| Upstream style/license/property checks | Passed |
| `japicmp` binary compatibility | Passed on both build runs |
| Add-on unit/workflow/package tests | 23 unit/workflow + 1 package smoke passed; valid results reused after QA-only changes |
| Standalone add-on desktop | 44 checks passed in official ZAP 2.17.0 |
| Final extracted custom distribution | 22 Search + 44 Private Evidence desktop checks passed |
| Packaged upstream launcher | Version-only smoke passed with Java 17 and bounded heap |
| Archive integrity | ZIP CRC check, plugin inventory, default-config provenance, license/notice preservation and package hashes verified |
| Independent final review | One confirmed provenance issue fixed; no remaining correctness blocker found in the requested review areas |

ZAP's CI specifies `assemble` and `check` on Java 17/21. The executed test filters
cover core Search, HTTP view models, session/model code, Swing view models and
utilities. Scanner/crawler/target-traffic tests were excluded. Passing this
selected check run is not a claim that the full upstream CI suite passed.

The final desktop checks used the exact extracted distribution in fresh
profiles/home/preferences, private Xvfb displays and a network namespace with
only loopback. No requests were sent, no scanners ran, and the recorded interface
inventory showed zero packets. No user installation, system Java, credentials
or trust store changed. QA profiles and their normal profile-local CAs are kept
outside deliverables. Build/run jobs were serial with one worker, bounded JVMs
and approximately 9–10 GiB host memory available.

The final bundle has no startup ERROR or missing vulnerability-provider message.
The first minimal bundle's missing-provider error was resolved by including the
unchanged Common Library 1.39.0 package, whose SHA-256 matches upstream's pinned
main-add-ons file. No scanner/crawler rule packs are bundled.

Evidence remains local under `../qa/release-checks/`: Java 17/21 build logs and
unit XML, final package logs, standalone and bundled desktop results/renders,
namespace inventories and launcher output. Screenshots and raw logs are not
included in the Library handoff.

## Resolved failures

Final review found that 0.1.1 obtained its revision when the action ran, after a
popup had cached the selection. A completed session transition could give an
old lazy selection a fresh revision. Version 0.1.2 binds the revision during
enablement and rejects stale activation before a stored read. The corrected
lazy fixture fails against retained 0.1.1 and passes against 0.1.2. An earlier
fixture copied the lazy list during construction and was inconclusive; it was
corrected and preserved separately. Reuse 0.1.2 for current work.

Earlier uncached style dependencies, QA readiness/selectors, focusability and
toolbar clipping were corrected and are recorded in the component verification
files. No final required check within the selected scope remains failed.

## Deliverables and installation distinction

| Deliverable | Purpose |
| --- | --- |
| `privateevidence-alpha-0.1.2.zap` | Installs only Private Evidence on stock ZAP 2.17.0+ |
| `ZAP_personal_2.18.0-SNAPSHOT.zip` | Custom runnable build containing both core Search changes and the add-on |
| `personal-zap-safe-changes.patch` | Complete local implementation, tests, QA and documentation diff from the pinned upstream base |
| `private-evidence-addon.patch` | Add-on-only diff |
| `literal-search-core.patch` | Core UI/test/QA diff |

The `.zap` cannot install the core Search changes. Extract the custom archive into
a separate directory for both features, use Java 17+, and follow its
README-personal.md or [the setup guide](personal-fork-setup.md). This is an alpha
development snapshot, not an official ZAP release or platform-certified installer.

Archive: **60,249,393 bytes**, SHA-256
`bb40ac7aba08df3d2ba10fdf63a3164be708dc39c1a476104a7028b0b886b65f`.
Add-on: **26,703 bytes**, SHA-256
`04d1fb20a8e364af0db0bd0e0e80d0559da98b94398f7d53c777f54912b15d22`.
Bundled core JAR with runtime SBOM: SHA-256
`dbfbb545cd2302613220bf0259526ecc3f1ee73dcddbbb6a0e0dbd98fe9c23e6`.
The separately retained QA JAR hashes
`6a0543041c2c14641f8446401a8c4730e376e6c3e655150f736f2bd49e5d3a65`;
its class/resource payload matches the bundled JAR, which additionally contains
the runtime software bill of materials.
Apache-2.0 and upstream/third-party attribution are preserved. No proprietary
Burp code/assets, credentials, real traffic, QA sessions/CA keys, private guide
or unrelated files are included.

## Remaining limits

- Full unfiltered upstream suite, GUI suite, API transport suite, scanner/target
  integration workflows and native installer signing/certification were unrun.
- Normal user-profile installation, physical keyboard/window-manager behavior,
  screen-reader, high-DPI, alternate themes/translations, Windows and macOS remain
  unrun. Programmatic Space activation and real Swing renders were checked.
- Large real-traffic profiling, every third-party searcher and failed-open
  recovery variants remain unrun. The unchanged core search backend was not
  redesigned for concurrent session changes.
- Private summaries are lossy and not fully anonymous: counts and optional paths
  can reveal activity. Equal summaries do not establish equal raw traffic. Export
  needs hard-link support; power-loss durability and non-POSIX permissions are not
  certified. Failed session operations need a successful subsequent operation
  before new private review; successful save was verified.

The bounded safe iteration is complete at a clean local checkpoint. Additional
work should be driven by platform QA or a specific supplied-traffic/session
case, rather than inventing features from the general roadmap.
