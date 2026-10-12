#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 jaykobdetar
set -euo pipefail
if [[ ${1:-} == --inside && $# == 4 ]]; then
    qa_repo="$2"; qa_jdk="$3"; qa_out="$4"
    qa_name="qa-$(basename -- "$qa_out")"
    qa_profile="$qa_repo/.personal-zap/profiles/$qa_name"
    [[ ! -e "$qa_profile" ]]
    openbox --sm-disable > "$qa_out/window-manager.log" 2>&1 &
    qa_wm=$!
    trap 'kill "$qa_wm" 2>/dev/null || true' EXIT
    for qa_round in 1 2; do
        JAVA_HOME="$qa_jdk" "$qa_repo/zap-personal" run --profile "$qa_name" > "$qa_out/launch-$qa_round.log" 2>&1 &
        qa_pid=$!
        qa_window=""
        for ((qa_poll=0; qa_poll<40; qa_poll++)); do
            if ! kill -0 "$qa_pid" 2>/dev/null; then cat "$qa_out/launch-$qa_round.log"; exit 1; fi
            qa_window="$(xdotool search --onlyvisible --name 'ZAP [0-9]' 2>/dev/null | tail -1 || true)"
            if [[ -n "$qa_window" ]] && rg -q 'Personal offline review: extension command-line startup skipped' "$qa_out/launch-$qa_round.log"; then break; fi
            sleep 1
        done
        [[ -n "$qa_window" ]]
        sleep 4
        xdotool search --onlyvisible --name '.' getwindowname > "$qa_out/windows-$qa_round.txt"
        import -window root "$qa_out/desktop-$qa_round.png"
        cat /proc/net/tcp /proc/net/tcp6 > "$qa_out/tcp-running-$qa_round.txt"
        # Headers only: no TCP socket, including listeners, may be present.
        if awk 'NF && $1 != "sl" { found=1 } END { exit !found }' "$qa_out/tcp-running-$qa_round.txt"; then
            printf '%s\n' 'Unexpected TCP socket during offline startup.' >&2
            kill "$qa_pid"
            exit 1
        fi
        if [[ "$qa_round" == 1 ]]; then
            if JAVA_HOME="$qa_jdk" "$qa_repo/zap-personal" run --profile "$qa_name" > "$qa_out/duplicate.log" 2>&1; then exit 1; fi
            rg -q 'already running' "$qa_out/duplicate.log"
        fi
        xdotool windowactivate --sync "$qa_window" key --clearmodifiers alt+F4
        sleep 1
        xdotool key --clearmodifiers Return
        wait "$qa_pid"
        cp -- "$qa_profile/zap/config.xml" "$qa_out/config-$qa_round.xml"
        printf '%s\n' "Round $qa_round closed normally" >> "$qa_out/checks.txt"
    done
    printf '%s\n' "$qa_profile" > "$qa_out/profile-path.txt"
    python3 - "$qa_out" <<'PY'
import hashlib
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
out = Path(sys.argv[1])
ca_hashes = []
for number in (1, 2):
    config = ET.parse(out / f"config-{number}.xml").getroot()
    for key, expected in (("view/mode", "safe"), ("view/displayOption", "3"),
                          ("api/enabled", "false"), ("database/newsession", "1"),
                          ("database/newsessionprompt", "false"),
                          ("start/checkForUpdates", "false"),
                          ("start/checkAddonUpdates", "false"),
                          ("start/installAddonUpdates", "false")):
        assert config.findtext(key) == expected, (number, key)
    ca = config.findtext("network/serverCertificates/rootCa/ks")
    assert ca
    ca_hashes.append(hashlib.sha256(ca.encode()).hexdigest())
assert ca_hashes[0] == ca_hashes[1], "Profile-local CA changed on reopening"
with (out / "checks.txt").open("a") as report:
    report.write("Safe/UI/update/session settings retained; profile-local CA unchanged.\n")
PY
    exit 0
fi
if [[ $# != 3 ]]; then
    printf '%s\n' 'Usage: run-desktop-qa.sh BUILT_FORK_ROOT FULL_JDK NEW_OUTPUT_DIRECTORY' >&2
    exit 1
fi
qa_repo="$(realpath -- "$1")"
qa_jdk="$(realpath -- "$2")"
qa_out="$(realpath -m -- "$3")"
[[ -x "$qa_repo/zap-personal" && -x "$qa_jdk/bin/java" && ! -e "$qa_out" && ! -L "$qa_out" ]]
for qa_tool in unshare xvfb-run ip xdotool openbox import timeout rg; do command -v "$qa_tool" >/dev/null; done
mkdir -m 700 -- "$qa_out"
# Use a new QA-only named profile, then reopen it. No existing configuration is seeded.
unshare -Urn bash -c '
    set -euo pipefail
    qa_repo="$1"; qa_jdk="$2"; qa_out="$3"
    ip link set lo up
    cat /proc/net/dev > "$qa_out/network-before.txt"
    timeout 110s xvfb-run -a -s "-screen 0 1280x900x24 -nolisten tcp" \
        bash "$qa_repo/qa/launcher/run-desktop-qa.sh" --inside "$qa_repo" "$qa_jdk" "$qa_out"
    cat /proc/net/dev > "$qa_out/network-after.txt"
    cat /proc/net/tcp /proc/net/tcp6 > "$qa_out/tcp-after.txt"
' _ "$qa_repo" "$qa_jdk" "$qa_out"
