#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 3 ]]; then
    printf '%s\n' "Usage: run-desktop-qa.sh MINIMAL_ZAP_RUNTIME FULL_JDK NEW_OUTPUT_DIRECTORY" >&2
    exit 1
fi

qa_base="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
qa_runtime="$(realpath -- "$1")"
qa_jdk="$(realpath -- "$2")"
qa_out="$(realpath -m -- "$3")"
qa_addon="$qa_base/../build/zapAddOn/bin/privateevidence-alpha-0.1.1.zap"

[[ -x "$qa_jdk/bin/javac" && -x "$qa_jdk/bin/java" && -f "$qa_addon" ]]
[[ -f "$qa_runtime/zap-2.17.0.jar" ]]
if [[ -e "$qa_out" || -L "$qa_out" ]]; then
    printf '%s\n' "Choose a new output directory; existing QA data is preserved." >&2
    exit 1
fi
for qa_plugin in "$qa_runtime"/plugin/*.zap; do
    case "$(basename -- "$qa_plugin")" in
        network-*.zap|callhome-*.zap) ;;
        *) printf '%s\n' "Use a disposable runtime with only Network and Call Home add-ons." >&2; exit 1 ;;
    esac
done
for qa_command in unshare xvfb-run ip timeout; do
    command -v "$qa_command" >/dev/null
done

mkdir -m 700 -- "$qa_out"
mkdir -p -- "$qa_out/classes" "$qa_out/profile/plugin" "$qa_out/results"
cp -- "$qa_addon" "$qa_out/profile/plugin/"
qa_classpath="$qa_out/classes:$qa_runtime/zap-2.17.0.jar:$qa_runtime/lib/*"
"$qa_jdk/bin/javac" -J-Xmx192m -cp "$qa_classpath" -d "$qa_out/classes" "$qa_base/DesktopQa.java"

unshare -Urn bash -c '
    set -euo pipefail
    qa_out="$1"; qa_jdk="$2"; qa_runtime="$3"; qa_classpath="$4"
    ip link set lo up
    cat /proc/net/dev > "$qa_out/network-namespace.txt"
    exec timeout 120s xvfb-run -a -s "-screen 0 1280x900x24 -nolisten tcp" \
        "$qa_jdk/bin/java" -Xmx640m -XX:ActiveProcessorCount=2 \
        "-Duser.home=$qa_out/home" "-Djava.util.prefs.userRoot=$qa_out/prefs" \
        "-Dqa.results=$qa_out/results" -cp "$qa_classpath" DesktopQa \
        -silent -dir "$qa_out/profile" -installdir "$qa_runtime" \
        -newsession "$qa_out/profile/synthetic-session" \
        -config view.mode=safe -config view.askOnExit=0 -config api.enabled=false \
        -config start.checkForUpdates=false -config start.checkAddonUpdates=false \
        -config start.installAddonUpdates=false \
        -config "network.localServers[@version]=1" \
        -config network.localServers.mainProxy.enabled=false
' _ "$qa_out" "$qa_jdk" "$qa_runtime" "$qa_classpath" > "$qa_out/runtime.log" 2>&1
tail -n 8 -- "$qa_out/runtime.log"
