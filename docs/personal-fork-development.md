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
| Request/response viewing and search | Core Request/Response views and `extension/search` | Reuse; no replacement editor |
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
local to this task. Local reviewable commits are authorized. Pushing
implementation, merging, and publishing releases require further user approval.

Use the add-on's README for reproducible commands. Record successful, failed
and unrun checks separately; a packaged alpha add-on is not a fully validated
cross-platform release. Do not invent test results or treat an unrun full core
suite as passing.
