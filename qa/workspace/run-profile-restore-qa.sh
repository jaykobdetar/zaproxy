#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 6 ]]; then
    printf '%s\n' 'Usage: run-profile-restore-qa.sh MINIMAL_ZAP_RUNTIME FULL_JDK FORK_CORE_JAR CORE_LIBS COMPLETED_DISPOSABLE_QA_DIRECTORY NEW_RESULT_DIRECTORY' >&2
    exit 1
fi
qa_runtime="$(realpath -- "$1")"
qa_jdk="$(realpath -- "$2")"
qa_core="$(realpath -- "$3")"
qa_libs="$(realpath -- "$4")"
qa_previous="$(realpath -- "$5")"
qa_out="$(realpath -m -- "$6")"
[[ -f "$qa_previous/results/desktop-checks.txt" && -f "$qa_previous/profile/config.xml" ]]
[[ -f "$qa_previous/classes/WorkspaceQa.class" && -f "$qa_core" && -x "$qa_jdk/bin/java" ]]
[[ ! -e "$qa_out" && ! -L "$qa_out" ]]
for qa_plugin in "$qa_runtime"/plugin/*.zap; do
    case "$(basename -- "$qa_plugin")" in
        network-*.zap|callhome-*.zap|commonlib-*.zap|privateevidence-*.zap) ;;
        *) printf '%s\n' 'Use the same minimal disposable runtime as workspace QA.' >&2; exit 1 ;;
    esac
done
mkdir -m 700 -- "$qa_out"
qa_classpath="$qa_previous/classes:$qa_core:$qa_libs/*"
unshare -Urn bash -c '
    set -euo pipefail
    qa_out="$1"; qa_jdk="$2"; qa_runtime="$3"; qa_classpath="$4"; qa_previous="$5"
    ip link set lo up
    cat /proc/net/dev > "$qa_out/network-before.txt"
    qa_record_network() { cat /proc/net/dev > "$qa_out/network-after.txt"; }
    trap qa_record_network EXIT
    timeout 90s xvfb-run -a -s "-screen 0 1280x900x24 -nolisten tcp" \
        "$qa_jdk/bin/java" -Xmx640m -XX:ActiveProcessorCount=2 \
        "-Duser.home=$qa_previous/home" "-Djava.util.prefs.userRoot=$qa_previous/prefs" \
        "-Dqa.results=$qa_out" -Dqa.restore=true -cp "$qa_classpath" WorkspaceQa \
        -silent -dir "$qa_previous/profile" -installdir "$qa_runtime" \
        -config view.mode=safe -config view.askOnExit=0 -config api.enabled=false \
        -config database.newsessionprompt=false -config database.newsession=3 \
        -config start.checkForUpdates=false -config start.checkAddonUpdates=false \
        -config start.installAddonUpdates=false \
        -config "network.localServers[@version]=1" \
        -config network.localServers.mainProxy.enabled=false
' _ "$qa_out" "$qa_jdk" "$qa_runtime" "$qa_classpath" "$qa_previous" > "$qa_out/runtime.log" 2>&1
tail -n 5 -- "$qa_out/runtime.log"
