# Personal ZAP fork: bounded development guide

## Verified starting point

- Personal fork: https://github.com/jaykobdetar/zaproxy
- Authenticated GitHub owner: `jaykobdetar` (GitHub user ID 212103200), verified
  through GitHub's user endpoint and the connected GitHub account.
- Official core: https://github.com/zaproxy/zaproxy, default branch `main`.
  The official [build guide](https://www.zaproxy.org/docs/developer/quick-start-build/)
  links this upstream. Starting commit: `38ffe34a1b03fd3241e0bb233c2a083df875ee33`.
- Official add-on source reviewed: https://github.com/zaproxy/zap-extensions,
  commit `ee36d786210a37c7451259f3531681d128c88442`.
- Core and add-ons use Apache License 2.0. Preserve the core's LICENSE,
  LEGALNOTICE.md, existing copyright notices, and third-party attribution.
- The attached `deep-research-report (1).md` was materialized and read on this
  executor (40,835 bytes; SHA-256
  `f5f3b68acace6e3244f9f05812dedf8c0277203b12ecf971ef81a2bb935ad6eb`).
  It is a general from-scratch roadmap, not evidence that ZAP lacks its features.
  The private reference is deliberately outside the Git checkout.

## Architecture and actual gap assessment

Keep ZAP's Java/Swing core, session database, and extension hooks. The
[add-on guide](https://www.zaproxy.org/docs/developer/creating-new-addon-in-zap-extensions/)
supports independently maintained add-ons. There is no evidence for replacing
the proxy core, UI framework, database, or plugin system. The source currently
targets Java 17, although the workstation also has Java 21.

| Guide area | Existing implementation verified | Decision |
| --- | --- | --- |
| Request/response viewing and search | Core Request/Response views and `extension/search`; default search compiles every query as a regex | Reuse views; add an explicit literal-text option to the existing Search tab |
| Offline comparison | [Diff add-on](https://www.zaproxy.org/docs/desktop/addons/diff/) | Reuse for full messages; add only summary comparison in the private review workflow |
| Sessions/projects | `org.parosproxy.paros.model.Session` provides open, persistence and snapshots; HSQLDB dependency | No new session format; investigate a concrete recovery problem before altering storage |
| Export | [Import/Export](https://www.zaproxy.org/docs/desktop/addons/import-export/) supports raw, XML, HAR and text exports | Existing `PopupMenuSaveRawMessage`, `PopupMenuExportMessages`, and HAR exporter write traffic; a private summary preview is a narrow, useful addition |
| Reports | [Report Generation](https://www.zaproxy.org/docs/desktop/addons/report-generation/) has templates, formats, scope and sections | Reuse; no new report engine |
| Extensibility | `ExtensionAdaptor`, HTTP message context menus, `org.zaproxy.add-on` Gradle plugin | Use a small add-on |
| Passive analysis | Existing passive scanner and rule add-ons are listed in the official catalog | No new rule without a demonstrated gap and synthetic fixtures |

The checked Import/Export and Report Generation Java sources did not expose a
general redacted-preview export path. This is a scoped observation at the pinned
commit, not a claim about every marketplace add-on or third-party script.
Report-job environment-variable sanitization is unrelated to traffic redaction.

## Bounded first plan for 6.1 sol

The authorized first iteration contains two coherent batches, implemented in
`add-ons/private-evidence` independently of the upstream core build.

1. **Private evidence preview/export:** snapshot only the selected existing
   messages; retain a small allowlist of HTTP method/media-type metadata, status
   and body byte counts; omit all bodies, credentials, header values, hosts,
   query strings and fragments. Default to omitting URL paths too. Provide an
   explicit path disclosure option and review the exact UTF-8 preview before
   saving.
2. **Offline review and reliable file output:** search the sanitized preview
   literally, compare the summaries of exactly two messages, label search
   controls for accessibility, prepare summaries and write files outside the
   Swing event thread, limit each review to 100 messages, and stage new output
   without replacing existing files.

Acceptance criteria: synthetic credential/body values never appear in default
output; path opt-in still omits authority/query/fragment; original messages
remain unchanged; no network, proxy, sender or scanner hooks; metadata
comparison is truthful about equality of summaries; file conflicts preserve
existing bytes; the add-on compiles, tests, and packages with the official plugin.

## Prioritized follow-on work

These are bounded next candidates, not claims of missing upstream capabilities
or a promise to complete the entire reference roadmap.

1. User review of the alpha summary workflow in a normal ZAP installation,
   including screen-reader and high-DPI checks. Collect a concrete usability
   issue before expanding the interface.
2. A separately approved policy for additional metadata fields or structured
   summary exports. Keep body inclusion out until a robust, explicit privacy
   design exists. Masking credentials alone cannot anonymize arbitrary traffic.
3. Read-only session-recovery assessment using synthetic saved sessions and
   existing upstream tests. Preserve ZAP's format; propose a change only for a
   demonstrated recovery gap.
4. Focused passive rule quality improvements only when an existing supplied
   traffic case demonstrates a real false positive or missed defensive signal.

## Excluded guide items

Do not implement or enhance active vulnerability probing, attack payloads,
credential attacks, offensive fuzzing, OAST exploitation, autonomous offensive
workflows, or scanner/crawler orchestration. Do not run scans against targets or
reproduce vulnerabilities. Existing upstream capabilities remain unchanged.
Network sending, interception changes, automatic authentication and distributed
scan infrastructure are outside this iteration.

No proprietary Burp code/assets are used. No security settings, credentials,
telemetry, auto-update behavior or marketplace publication are changed.

## Working agreement

Use this isolated checkout; do not modify or resume FE8. Check memory before
heavy work, run builds serially with one worker and bounded heaps, and pause
heavy work if host headroom falls below the configured needs. Keep build caches
local to this task. Local reviewable commits are authorized. The user authorized
the initial `feature/private-evidence` publication at `efd5ffc` on 2026-10-12.
The newer `feature/familiar-workspace` UI work remains local. Further pushes,
merges, and release publication require separate approval.

Use the add-on's README for reproducible commands. Record successful, failed
and unrun checks separately; a packaged alpha add-on is not a fully validated
cross-platform release. Do not invent test results or treat an unrun full core
suite as passing.

## Completed follow-on reliability batch

Real ZAP desktop verification of the packaged add-on passed 40 checks using
synthetic traffic and disposable sessions, in a network namespace with only
loopback. An independent review identified and prompted fixes for CONNECT
authority/path disclosure, UI-thread lazy loading by this add-on's enablement
check, and absent/unrecognized status confusion.

The next project-reliability gap was verified rather than assumed: saved
sessions reuse history row IDs, and an old uncached HistoryReference can retrieve
the replacement session's row. Private Evidence now listens for session
transitions, rejects capture begun during a transition, checks a revision around
each lazy read, and discards stale publication. The normal ZAP session format
and upstream runtime code remain unchanged.

Failed session operations can omit the upstream completion notification.
Review stays closed and supplies recovery guidance; a subsequent successful
session save restored review in the disposable runtime test. No heuristic
timeout/reset is used. See the add-on's VERIFICATION.md for the independent
review record, resolved failed attempts, package hash and remaining QA limits.

## Completed offline search and accessibility batch

The official [Search tab documentation](https://www.zaproxy.org/docs/desktop/ui/tabs/search/)
describes regex queries. At the pinned core commit, `SearchPanel.doSearch`
compiles the user's text directly as a regex, and `SearchThread` and result
highlighting use that regex. Searching literal punctuation therefore requires
manual escaping. A plain-text option is a concrete gap in the existing offline
viewer, unlike most of the reference guide's already implemented foundations.

The core Search tab now has a **Literal** checkbox, off by default. It quotes
the complete query with `Pattern.quote`, including embedded `\\E`, and uses the
existing stored-history engine. Empty literal queries show a warning without
clearing results. Whitespace is significant. Matching retains the engine's
existing A-Z case folding; it does not add non-ASCII case folding. Programmatic
searches that populate the toolbar restore regex mode; API contracts and engine
flags are unchanged.

The query has an accessible name and the inverse label identifies its control.
The new checkbox remains focusable under ZAP's toolbar look and feel. Result
navigation, counts/status and export/options now occupy a separate bottom bar,
preventing the clipping found during narrow-window desktop QA. Existing actions
and component instances are reused. This small core-owned view change avoids
an add-on reaching into another extension's private Swing controls.

See [offline-search verification](../qa/offline-search/VERIFICATION.md) for the
focused tests, desktop evidence, resolved failures and remaining platform limits.
The Private Evidence add-on still works independently on stock ZAP 2.17.0+.

## Reference roadmap disposition

This is the completion matrix for the bounded safe iteration. It records scoped
source/doc verification, rather than claiming an exhaustive marketplace audit.

| Guide area | Current disposition | Evidence or next condition |
| --- | --- | --- |
| Offline request/response inspection | Existing foundation retained | Core HTTP panels, History and Search |
| Literal search and accessible controls | Implemented | Explicit literal mode, named query, labeled inverse control and compact results bar |
| Full traffic comparison | Existing add-on retained | Official Diff add-on; no replacement comparison engine |
| Private summary comparison/search/export | Implemented | Private Evidence 0.1.2, exact preview and new-file output, 100-message cap |
| Project/session persistence and snapshots | Existing foundation retained | Core Session and HSQLDB; no new database/file format |
| Session transition provenance/recovery | Implemented for Private Evidence | Verified stale stored references are rejected; successful-save recovery verified after a failed operation |
| Standard report templates/import/export | Existing add-ons retained | Reports and Import/Export source reviewed; private summaries supplement them |
| UI framework and plugin architecture | Existing foundation retained | Swing/ExtensionAdaptor/public message-menu hooks; no core rewrite |
| Passive analysis of supplied traffic | Existing foundation retained | Official passive scanner/rules; no concrete new rule defect established by this guide |
| Performance and reliability | Bounded work implemented | Background capture/export, bounded reviews, session invalidation; no claim of large-traffic performance profiling |
| Session encryption and shared workspaces | Deferred safe design work | Needs specific data, compatibility and recovery requirements; not inferred to be missing from all extensions |
| Platform/accessibility qualification | Partly verified, partly unrun | Linux Swing runtime checked; physical keyboard, screen-reader, high-DPI, other OSes/themes still need QA |
| Active probes, exploit payloads, credential attacks, fuzzing, OAST and offensive automation | Excluded | Outside the authorized implementation scope; no target contact or scanner execution |

The guide does not establish another distinct high-value safe gap at the reviewed
source pins. The next useful work is platform/usability qualification, or a
specific supplied-traffic/session case that demonstrates a defect. Additional
fields, formats, collaboration or storage changes need concrete requirements
before expanding this alpha workflow. Existing tests and valid evidence are
preserved; the full upstream suite and normal installation are not claimed as
passed.

Use [the setup and handoff guide](personal-fork-setup.md) for the published initial
checkpoint, artifacts and reproducible bounded checks. The newer unpushed UI
batch is described in [the workspace guide](personal-fork-workspace.md).

## Final readiness checkpoint

Private Evidence 0.1.2 supersedes 0.1.1 after final review found a cached-popup
provenance gap. Selection revision is now bound during enablement and checked
before activation. The corrected synthetic fixture fails against 0.1.1 and
passes against 0.1.2. Existing tests were extended only for that verified gap.

A runnable local snapshot and standalone add-on are ready. Java 17 and 21 each
passed upstream assembly/check with 431 explicitly selected offline tests,
formatting and binary compatibility. The extracted final archive passed both
existing desktop harnesses (22 Search and 44 Private Evidence checks). This
qualifies the bounded safe paths, not the full unfiltered upstream CI matrix or
a platform-certified installer. [The final report](personal-fork-verification.md)
records the immutable runtime-code commit, exact hashes and remaining limits.
