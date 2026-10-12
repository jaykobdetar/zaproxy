#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 6 ]]; then
    printf '%s\n' 'Usage: run-desktop-qa.sh MINIMAL_ZAP_RUNTIME FULL_JDK FORK_CORE_JAR CORE_LIBS DISPOSABLE_QA_CONFIG NEW_OUTPUT_DIRECTORY' >&2
    exit 1
fi
qa_base="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
qa_runtime="$(realpath -- "$1")"
qa_jdk="$(realpath -- "$2")"
qa_core="$(realpath -- "$3")"
qa_libs="$(realpath -- "$4")"
qa_config="$(realpath -- "$5")"
qa_out="$(realpath -m -- "$6")"
[[ -x "$qa_jdk/bin/javac" && -x "$qa_jdk/bin/java" && -f "$qa_core" ]]
[[ -d "$qa_libs" && -f "$qa_config" ]]
if [[ -e "$qa_out" || -L "$qa_out" ]]; then
    printf '%s\n' 'Choose a new output directory; existing QA data is preserved.' >&2
    exit 1
fi
for qa_plugin in "$qa_runtime"/plugin/*.zap; do
    case "$(basename -- "$qa_plugin")" in
        network-*.zap|callhome-*.zap) ;;
        *) printf '%s\n' 'Use a disposable runtime with only Network and Call Home add-ons.' >&2; exit 1 ;;
    esac
done
for qa_command in unshare xvfb-run ip timeout; do
    command -v "$qa_command" >/dev/null
done
mkdir -m 700 -- "$qa_out"
mkdir -p -- "$qa_out/classes" "$qa_out/profile" "$qa_out/results"
# Reuse only our earlier disposable QA configuration/CA. Never use a user's profile.
cp -- "$qa_config" "$qa_out/profile/config.xml"
qa_classpath="$qa_out/classes:$qa_core:$qa_libs/*"
"$qa_jdk/bin/javac" -J-Xmx192m -cp "$qa_classpath" -d "$qa_out/classes" "$qa_base/OfflineSearchQa.java"
unshare -Urn bash -c '
    set -euo pipefail
    qa_out="$1"; qa_jdk="$2"; qa_runtime="$3"; qa_classpath="$4"
    ip link set lo up
    cat /proc/net/dev > "$qa_out/network-namespace.txt"
    exec timeout 120s xvfb-run -a -s "-screen 0 1280x900x24 -nolisten tcp" \
        "$qa_jdk/bin/java" -Xmx640m -XX:ActiveProcessorCount=2 \
        "-Duser.home=$qa_out/home" "-Djava.util.prefs.userRoot=$qa_out/prefs" \
        "-Dqa.results=$qa_out/results" -cp "$qa_classpath" OfflineSearchQa \
        -silent -dir "$qa_out/profile" -installdir "$qa_runtime" \
        -config view.mode=safe -config view.askOnExit=0 -config api.enabled=false \
        -config database.newsessionprompt=false -config database.newsession=3 \
        -config start.checkForUpdates=false -config start.checkAddonUpdates=false \
        -config start.installAddonUpdates=false \
        -config "network.localServers[@version]=1" \
        -config network.localServers.mainProxy.enabled=false
' _ "$qa_out" "$qa_jdk" "$qa_runtime" "$qa_classpath" > "$qa_out/runtime.log" 2>&1
tail -n 8 -- "$qa_out/runtime.log"
