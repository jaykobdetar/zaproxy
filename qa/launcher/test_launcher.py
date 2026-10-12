# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 jaykobdetar
"""Offline checks for launcher isolation, integrity and failure preservation."""

import importlib.machinery
import importlib.util
import io
import json
import os
from pathlib import Path
import shlex
import tempfile
import unittest
from unittest.mock import patch


SOURCE = Path(__file__).resolve().parents[2] / "zap-personal"
loader = importlib.machinery.SourceFileLoader("personal_launcher", str(SOURCE))
spec = importlib.util.spec_from_loader(loader.name, loader)
launcher = importlib.util.module_from_spec(spec)
loader.exec_module(launcher)


class ExecCaptured(Exception):
    pass


class LauncherTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="personal launcher ")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.state = self.root / ".personal-zap"
        self.patches = [patch.object(launcher, "ROOT", self.root), patch.object(launcher, "STATE", self.state)]
        for item in self.patches:
            item.start()
            self.addCleanup(item.stop)

    def runtime(self):
        runtime = self.state / "builds/runtime-test"
        runtime.mkdir(parents=True)
        (runtime / "zap.sh").write_text("#!/bin/bash\n")
        (runtime / "core.jar").write_bytes(b"synthetic runtime")
        files = {p.name: launcher.digest(p) for p in runtime.iterdir()}
        (runtime / "BUILD-METADATA.json").write_text(json.dumps({"sha256": files}))
        (self.state / "current").write_text(runtime.name + "\n")
        return runtime

    def capture_launch(self, fresh=False):
        captured = {}

        def capture(binary, command, env):
            captured.update(binary=binary, command=command, env=env)
            raise ExecCaptured()

        with patch.dict(os.environ, {"DISPLAY": ":test"}), patch.object(launcher, "check_headroom"), patch.object(launcher.os, "execvpe", side_effect=capture):
            with self.assertRaises(ExecCaptured):
                launcher.launch({"JAVA_HOME": "/synthetic jdk"}, fresh)
        return captured

    def test_missing_runtime_error(self):
        with self.assertRaisesRegex(launcher.LauncherError, "build first"):
            launcher.current_runtime()
        self.assertFalse(self.state.exists())

    def test_corrupted_runtime_preserves_bytes(self):
        runtime = self.runtime()
        (runtime / "core.jar").write_bytes(b"changed")
        with self.assertRaisesRegex(launcher.LauncherError, "integrity"):
            launcher.current_runtime()
        self.assertEqual(b"changed", (runtime / "core.jar").read_bytes())

    def test_added_plugin_is_not_silently_launched(self):
        runtime = self.runtime()
        (runtime / "unexpected.zap").write_bytes(b"synthetic extra plugin")
        with self.assertRaisesRegex(launcher.LauncherError, "integrity"):
            launcher.current_runtime()

    def test_runtime_pointer_cannot_escape_builds(self):
        self.state.mkdir()
        (self.state / "current").write_text("../../existing-data")
        with self.assertRaisesRegex(launcher.LauncherError, "pointer"):
            launcher.current_runtime()

    def test_default_launch_isolated_safe_and_no_target(self):
        self.runtime()
        captured = self.capture_launch()
        command = captured["command"]
        profile = self.state / "profiles/default"
        self.assertEqual(str(profile / "zap"), command[command.index("-dir") + 1])
        for value in launcher.SAFE_CONFIG:
            self.assertIn(value, command)
        self.assertIn("view.displayOption=3", command)
        self.assertIn("database.newsession=1", command)
        self.assertIn("database.newsessionprompt=false", command)
        self.assertIn("-silent", command)
        self.assertIn("-Xmx640m", command)
        jvm = shlex.split(captured["env"]["JDK_JAVA_OPTIONS"])
        self.assertIn(f"-Duser.home={profile / 'home'}", jvm)
        self.assertIn(f"-Djava.util.prefs.userRoot={profile / 'prefs'}", jvm)
        self.assertIn("-Dorg.zaproxy.zap.personal.offline=true", jvm)
        self.assertNotIn("HOME", captured["env"])
        self.assertFalse(any(arg.startswith("http") for arg in command))

    def test_repeat_launch_preserves_profile_and_ui_choice(self):
        self.runtime()
        self.capture_launch()
        config = self.state / "profiles/default/zap/config.xml"
        config.write_bytes(b"synthetic saved preferences")
        command = self.capture_launch()["command"]
        self.assertNotIn("view.displayOption=3", command)
        self.assertEqual(b"synthetic saved preferences", config.read_bytes())
        self.assertIn("view.mode=safe", command)

    def test_fresh_launch_preserves_default_and_previous_fresh(self):
        self.runtime()
        self.capture_launch()
        one = self.capture_launch(True)["command"]
        two = self.capture_launch(True)["command"]
        first = Path(one[one.index("-dir") + 1])
        second = Path(two[two.index("-dir") + 1])
        self.assertNotEqual(first, second)
        self.assertTrue(first.is_dir() and second.is_dir())
        self.assertTrue((self.state / "profiles/default").is_dir())

    def test_no_display_creates_no_profile(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(launcher.LauncherError, "DISPLAY"):
                launcher.launch({}, False)
        self.assertFalse(self.state.exists())

    def test_profile_name_cannot_escape_private_directory(self):
        self.runtime()
        with patch.dict(os.environ, {"DISPLAY": ":test"}), patch.object(launcher, "check_headroom"):
            with self.assertRaisesRegex(launcher.LauncherError, "Profile names"):
                launcher.launch({}, False, "../normal-profile")
        self.assertFalse((self.state / "normal-profile").exists())

    def test_concurrent_profile_refused(self):
        self.runtime()
        with launcher.locked(self.state / "profiles/default/launcher.lock", "locked"):
            with patch.dict(os.environ, {"DISPLAY": ":test"}), patch.object(launcher, "check_headroom"):
                with self.assertRaisesRegex(launcher.LauncherError, "already running"):
                    launcher.launch({}, False)

    def test_offline_missing_addon_never_downloads(self):
        with patch.object(launcher.urllib.request, "urlopen") as download:
            with self.assertRaisesRegex(launcher.LauncherError, "Offline cache"):
                launcher.cached_addon("https://github.com/zaproxy/zap-extensions/releases/download/example/example.zap", "0" * 64, True)
            download.assert_not_called()

    def test_corrupt_cached_addon_preserved(self):
        cache = self.state / "downloads"
        cache.mkdir(parents=True)
        package = cache / "example.zap"
        package.write_bytes(b"corrupt synthetic data")
        with self.assertRaisesRegex(launcher.LauncherError, "Checksum mismatch"):
            launcher.cached_addon("https://github.com/zaproxy/zap-extensions/releases/download/example/example.zap", "0" * 64, False)
        self.assertEqual(b"corrupt synthetic data", package.read_bytes())

    def test_download_checksum_failure_not_activated(self):
        url = "https://github.com/zaproxy/zap-extensions/releases/download/example/example.zap"
        with patch.object(launcher.urllib.request, "urlopen", return_value=io.BytesIO(b"bad synthetic package")):
            with self.assertRaisesRegex(launcher.LauncherError, "Downloaded checksum"):
                launcher.cached_addon(url, "0" * 64, False)
        self.assertEqual([], list((self.state / "downloads").iterdir()))

    def test_valid_download_reused_offline(self):
        url = "https://github.com/zaproxy/zap-extensions/releases/download/example/example.zap"
        expected = launcher.hashlib.sha256(b"synthetic package").hexdigest()
        with patch.object(launcher.urllib.request, "urlopen", return_value=io.BytesIO(b"synthetic package")) as download:
            package = launcher.cached_addon(url, expected, False)
            self.assertEqual(package, launcher.cached_addon(url, expected, True))
            self.assertEqual(1, download.call_count)
        self.assertEqual(b"synthetic package", package.read_bytes())

    def test_failed_build_preserves_current_and_profile(self):
        runtime = self.runtime()
        original = (self.state / "current").read_bytes()
        with patch.object(launcher, "check_headroom"), patch.object(launcher, "pinned_addons", return_value=[]), patch.object(launcher, "gradle", side_effect=launcher.LauncherError("synthetic failure")):
            with self.assertRaisesRegex(launcher.LauncherError, "synthetic failure"):
                launcher.build({}, False)
        self.assertEqual(original, (self.state / "current").read_bytes())
        self.assertTrue(runtime.is_dir())

    def test_untrusted_addon_pin_refused(self):
        manifest = self.root / "zap/src/main/main-add-ons.yml"
        manifest.parent.mkdir(parents=True)
        manifest.write_text('- id: "callhome"\n  url: "https://example.invalid/untrusted.zap"\n  hash: "SHA-256:' + "0" * 64 + '"\n')
        with self.assertRaisesRegex(launcher.LauncherError, "unsupported"):
            launcher.pinned_addons()

    def test_missing_full_jdk_clear_error(self):
        with patch.dict(os.environ, {"JAVA_HOME": str(self.root / "missing-jdk")}, clear=True):
            with self.assertRaisesRegex(launcher.LauncherError, "full JDK"):
                launcher.java_environment()


if __name__ == "__main__":
    unittest.main()
