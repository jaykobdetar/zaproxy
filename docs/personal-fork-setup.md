# Personal fork setup and handoff

The verified fork is https://github.com/jaykobdetar/zaproxy, owned by the
authenticated GitHub account `jaykobdetar`. The checkout is in
`/home/jaykob/Documents/Codex/2026-10-12/task/zaproxy`. The reviewed implementation
was developed on `feature/private-evidence`, based on upstream commit
`38ffe34a1b03fd3241e0bb233c2a083df875ee33`.

The user subsequently authorized publication. The remote `feature/private-evidence`
branch now contains the initial implementation at
`efd5ffc70dca073705a43ab82dacc21b42575c57`; cloning that branch retrieves it.
No PR, merge or release publication occurred. The newer UI batch was subsequently
published on `feature/familiar-workspace` at
`beb8888fe67e9f2297e8232963ff6f12b8a8a6d1` on 2026-10-12; see
[its separate workspace guide](personal-fork-workspace.md). The packages and
validation described below concern the initial implementation. FE8's checkout
and paused work were left untouched.

## What to try

For the current UI branch, use the [builder and launcher](personal-fork-launcher.md).
The manual artifacts and checks below preserve the initial implementation's
handoff; the older archive does not contain the newer Traffic review layout.

**Private Evidence 0.1.2** is a standalone alpha add-on for ZAP 2.17.0+.
The ready local package is
`../outputs/privateevidence-alpha-0.1.2.zap` relative to this checkout. Its
SHA-256 is
`04d1fb20a8e364af0db0bd0e0e80d0559da98b94398f7d53c777f54912b15d22`.

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
plugin directory. A runnable personal cross-platform archive is prepared in
`../outputs/ZAP_personal_2.18.0-SNAPSHOT.zip`. It includes the core UI patch,
Private Evidence 0.1.2, exact runtime libraries, and unchanged Network,
Call Home and Common Library add-ons. A normal user-profile installation remains unrun.
The preserved Linux harness starts this core with disposable synthetic
sessions and the mandatory add-ons from a verified copied official runtime.

## Run the custom build

Extract `ZAP_personal_2.18.0-SNAPSHOT.zip` into a separate directory. On Linux,
use Java 17+ and a fresh profile; do not reuse a personal production profile:

```bash
cd ZAP_2.18.0-SNAPSHOT
bash zap.sh -Xmx640m -silent -dir /absolute/path/to/new-personal-zap-profile \
  -config view.mode=safe -config api.enabled=false \
  -config start.checkForUpdates=false -config start.checkAddonUpdates=false \
  -config start.installAddonUpdates=false \
  -config 'network.localServers[@version]=1' \
  -config network.localServers.mainProxy.enabled=false
```

These settings apply to that new profile. No browser/system CA trust installation
is needed for offline viewing. The archive includes README-personal.md,
BUILD-METADATA.json, the upstream license/notice and third-party licenses. The
standalone `.zap` installs only Private Evidence; core Literal Search requires
this custom build or a source build.

Later launcher verification found that Network 0.25 forces the main proxy on
despite the `mainProxy.enabled=false` setting above. The older archive/manual
command therefore does not guarantee absence of a listener. Prefer the current
builder/launcher, which opts out of extension command-line startup. Earlier
desktop verification ran in an isolated network namespace and recorded no traffic.

See [the final verification report](personal-fork-verification.md) for the code
commit, Java 17/21 check results, exact archive hashes and remaining limits.

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
