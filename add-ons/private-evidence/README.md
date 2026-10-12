# Private Evidence (alpha)

A small offline review add-on for this personal ZAP fork. It uses ZAP's existing
HTTP message context menu and the official add-on packaging plugin. No core
runtime code is changed.

Select 1–100 existing messages in History (or another supported message view),
then choose **Review Private Evidence...**. Review the summary, optionally enable
URL paths, search literal text, compare exactly two selected messages, and use
**Save Preview...** to write the preview as UTF-8 text. Choose a new filename:
existing files and symlinks are never overwritten.

Default output retains only recognized HTTP methods, response status, body byte
counts and a small media-type allowlist. Bodies, all arbitrary headers/header
values, userinfo, hostnames, query strings, fragments, status reason phrases,
and unrecognized metadata are omitted. URL paths are omitted until explicitly
enabled. Media-type parameters (including multipart boundaries) are always
omitted. Oversized/invalid targets and unknown types are omitted completely.

This is a deliberately lossy summary, not replayable HTTP traffic. Equal summary
fields do not imply equal original messages. Byte counts and optional paths can
still reveal activity; review every export before sharing. The summary does not
export alert evidence, notes or findings. There is no claim of complete
anonymization.

The add-on registers no scanner, proxy, HTTP sender, automation or external
service hooks. Capture and file I/O use background workers. Preview/search/
comparison data is capped at 100 summaries. The original messages are never
edited. On POSIX filesystems, staged files are owner-readable/writable only.
Staging uses a sibling file and an atomic hard-link publication that also
preserves files created concurrently. The destination filesystem must support
hard links; unsupported filesystems fail without publishing a report.
Interrupted staging can leave a private
`.private-evidence-*.tmp` file, which can be removed after verifying no export
is running. Non-POSIX permissions follow the filesystem/OS defaults.

## Build and check

Requires Java 17+ and the existing core Gradle wrapper. From the fork root:

```bash
GRADLE_USER_HOME="$PWD/../.gradle-private-evidence" ./gradlew \
  -p add-ons/private-evidence --no-daemon --max-workers=1 --no-parallel \
  spotlessCheck test packageSmokeTest
```

The add-on has its own settings and dependencies; this command does not build
the core, other add-ons, or distributions. Its Gradle JVM is capped at 512 MiB
and metaspace at 256 MiB; tests use one 192 MiB JVM. Initial dependency downloads
need network access; cached test/build work can run offline.

The output is `build/zapAddOn/bin/privateevidence-alpha-0.1.0.zap` (inspect the build
output for the authoritative path). ZAP 2.17.0+ is required. Install manually
through ZAP's Manage Add-ons dialog and restart ZAP after installing or removing
this alpha add-on. It is not bundled or installed automatically. Tests only use
synthetic messages and local files, and run headlessly.

Preserve the parent Apache 2.0 license and attribution. The code is original and
uses public ZAP extension APIs. See [the fork development plan](../../docs/personal-fork-development.md)
for source verification, scope and follow-on priorities.
