# Personal ZAP: Traffic review workspace

## First UI batch

This is a local UI branch based on the reviewed Private Evidence implementation:
`feature/familiar-workspace`, starting at `efd5ffc70dca073705a43ab82dacc21b42575c57`.
The previous branch and its published commits are preserved. No UI commits have
been pushed, merged or published as a release.

Choose **Traffic review** in the main toolbar to arrange the existing workspace:

- Sites and other navigator panels on the left.
- Existing History, Search, Alerts, Output and add-on tool tabs above the message viewers.
- Request and response visible side by side below the selected traffic.

The same layout is also available under **Tools > Options > Display > Display**.
The Display option controls composition; the toolbar preset additionally chooses
side-by-side viewers and visible tab labels. The existing request/response
position controls remain available. A cancellation prompt for detached viewers
cancels their rearrangement; the workspace choice still applies.

## Design choice and architecture

The design uses familiar tool tabs, traffic-first hierarchy and paired message
viewers from public Burp UI references, with original code, ZAP icons, and the
existing ZAP look and feel. No Burp source, extracted application assets, branding
or proprietary code is used. The installed Burp launcher was found, but native
application control is unavailable in this executor. The official public HTTP
history screenshot was inspected as a layout reference only and is excluded
from the repository and deliverables.

References inspected on 2026-10-12:

- [Burp HTTP history](https://portswigger.net/burp/documentation/desktop/tools/proxy/http-history)
- [Burp site map layout](https://portswigger.net/burp/documentation/desktop/tools/target/site-map/site-map-layout)
- [Burp message editor](https://portswigger.net/burp/documentation/desktop/tools/message-editor)
- [Burp layout customization](https://portswigger.net/burp/documentation/desktop/burps-layout)
- [Official history screenshot in the tutorial](https://portswigger.net/burp/documentation/desktop/getting-started/intercepting-http-traffic)
- [ZAP desktop UI](https://www.zaproxy.org/docs/desktop/ui/)

ZAP already provided three layouts, hidden/pinned/detached tabs, font and theme
options, and several request/response arrangements. The verified gap was a
single coherent traffic-above-messages composition and an understandable preset,
not missing editors or a missing navigation framework.

`WorkbenchPanel.Layout.TRAFFIC_REVIEW` adds ID 3 while preserving IDs 0–2 and the
unknown-ID fallback. It reuses `TabbedPanel2`, the existing panels and selection
listeners. It has independent navigator and traffic divider preferences.
Review-only padding and FlatLaf tab height/insets/separators preserve the user's
font and colors; previous tab properties are restored when leaving the layout.
Other look and feels retain the new composition with their own native styling.
The new labeled preset is kept outside the toolbar's direct-button focus handling
so theme updates do not remove its keyboard focus target.

This batch changes presentation only. Existing message sending, scanners and
other tools retain upstream behavior. No new attack actions, probing, payloads,
fuzzing, credential workflows or automation are added.

## Bounded follow-up plan

1. Complete this composition/preset batch with offline desktop QA and independent review.
2. Consider an optional compact history column preset and more discoverable existing filters.
3. Refine message-view controls and empty states using the existing HTTP-panel views.
4. Check translated labels, high-DPI scaling, screen-reader use and other desktop platforms.

Later items are proposals, not implemented features. Keeping the first change
opt-in avoids overriding established profiles or add-on navigation. The classic
layouts remain available.

## Verification

The reproducible GUI harness is in `qa/workspace`. It runs the real core in an
Xvfb desktop and loopback-only network namespace with an existing disposable QA
configuration. It creates synthetic records directly in a disposable session;
no request is sent. API/proxy listeners, telemetry and update checks are disabled
for the QA process only. Host profiles, proxy settings and CA trust are untouched.

Final results and limitations are recorded in `qa/workspace/VERIFICATION.md`.
Comparison screenshots show classic and review layouts in the same implementation,
with the same synthetic records. Screenshots, test databases, certificates,
profiles, runtime binaries and build caches are outside Git.
