# Personal fork setup and handoff

The verified fork is https://github.com/jaykobdetar/zaproxy, owned by the
authenticated GitHub account `jaykobdetar`. Local implementation is in
`/home/jaykob/Documents/Codex/2026-10-12/task/zaproxy`, on
`feature/private-evidence`, based on upstream commit
`38ffe34a1b03fd3241e0bb233c2a083df875ee33`.

The remote fork contains upstream history. The implementation commits remain
local; cloning the remote alone will not retrieve them. Inspect the local branch
or the supplied patch. No implementation push, PR, merge or release occurred.
FE8's checkout and paused work were left untouched.

## What to try

**Private Evidence 0.1.1** is a standalone alpha add-on for ZAP 2.17.0+.
The ready local package is
`../outputs/privateevidence-alpha-0.1.1.zap` relative to this checkout. Its
SHA-256 is
`4fd18ac342260455b7cadc179dd0214f8ac78ed448734feebdd6913acbfc4f80`.

For a manual trial, use a separate ZAP profile and install the local `.zap`
package through Manage Add-ons, then restart. The official
[add-on documentation](https://www.zaproxy.org/docs/desktop/start/features/addons/)
also describes the profile's plugin directory. No user installation has been
changed by this task. See [the add-on README](../add-ons/private-evidence/README.md)
for use, default omissions and export limits.

Select 1–100 existing messages, choose **Review Private Evidence...**, inspect
the summary and save to a new filename. Paths are omitted by default. If a
session operation fails, complete a successful ordinary session save/reopen
before starting a new review. Preview equality is equality of summaries, not
of the omitted traffic.

**Literal Search** is a separate core UI patch. In a fork build, enable
**Literal** in Search to treat punctuation as text. Regex remains the default;
the existing type/scope/inverse controls still apply. Navigation, count/status
and export/options sit below the results. Search works on stored records and
does not send requests.

The built core is `zap/build/libs/zap-2.18.0-SNAPSHOT.jar`; exact cached runtime
dependencies are in `zap/build/offlineSearchQa/lib`. The JAR is a development
artifact, not an add-on or a complete installer. Do not put it in an add-on
plugin directory. A normal desktop distribution build/install remains unrun.
The preserved Linux harness starts this exact core with disposable synthetic
sessions and the mandatory add-ons from a verified copied official runtime.

## Reproduce the bounded checks

Use a full JDK 17+, sufficient host memory, a task-local cache, one Gradle worker
and serial jobs. On this host, the verified portable JDK is
`../toolchain/jdk-17.0.20.1+1`. Set `JAVA_HOME` to that directory and
`GRADLE_USER_HOME` to `../.gradle-private-evidence` using absolute paths. Check
memory before starting and pause heavy work when the host lacks headroom.

From the fork root, the add-on is independent of the core build:

```bash
./gradlew -p add-ons/private-evidence --offline --no-daemon \
  --max-workers=1 --no-parallel spotlessCheck test packageSmokeTest
```

For the core UI patch, run only the focused offline tests and build the JAR:

```bash
./gradlew --offline --no-daemon --max-workers=1 --no-parallel \
  -Dorg.gradle.jvmargs='-Xmx1024m -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=2' \
  -Pkotlin.compiler.execution.strategy=in-process \
  -I qa/offline-search/bounded.gradle \
  :zap:spotlessJavaCheck :zap:spotlessLicenseCheck \
  :zap:test --tests org.zaproxy.zap.extension.search.SearchPanelUnitTest \
  :zap:jar :zap:offlineSearchQaDependencies
```

Initial dependency downloads require authorized network access; `--offline`
requires the already populated cache. Gradle also needs local IPC sockets.
The QA init script caps the test JVM at 192 MiB and one fork; it does not run
the full core suite, distributions or scanners.

For disposable desktop QA, see [the verification record](../qa/offline-search/VERIFICATION.md).
The prepared official runtime and previous disposable QA configuration are
outside this Git checkout, under `../qa/`. The harness requires a new output
directory, isolates home/profile/preferences and network, and preserves earlier
evidence. Never supply a personal ZAP configuration as its seed.

Keep Apache-2.0 licensing and existing attribution. The private reference guide,
credentials, caches, QA profile/CA data and runtime binaries are outside the
checkout. No proprietary Burp code or assets were used. Pushes, merges and
publication require separate user approval.
