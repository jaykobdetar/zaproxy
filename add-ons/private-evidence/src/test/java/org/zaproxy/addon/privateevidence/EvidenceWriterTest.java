/*
 * Copyright 2026 jaykobdetar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.addon.privateevidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvidenceWriterTest {
    @TempDir Path directory;

    @Test
    void savesExactUnicodePreviewAndRemovesStagingFiles() throws Exception {
        Path target = directory.resolve("evidence.txt");
        String preview = "Private Evidence\n日本語\n";
        EvidenceWriter.saveNew(target, preview);
        assertEquals(preview, Files.readString(target));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
        if (Files.getFileStore(directory).supportsFileAttributeView("posix")) {
            assertEquals(
                    PosixFilePermissions.fromString("rw-------"),
                    Files.getPosixFilePermissions(target));
        }
    }

    @Test
    void existingFileIsPreservedAndStagingFileIsRemoved() throws Exception {
        Path target = directory.resolve("existing.txt");
        Files.writeString(target, "existing report");
        assertThrows(FileAlreadyExistsException.class, () -> EvidenceWriter.saveNew(target, "new"));
        assertEquals("existing report", Files.readString(target));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void existingSymbolicLinkIsPreservedWithoutFollowingIt() throws Exception {
        Path original = directory.resolve("original.txt");
        Files.writeString(original, "keep");
        Path link = directory.resolve("link.txt");
        Files.createSymbolicLink(link, original);
        assertThrows(FileAlreadyExistsException.class, () -> EvidenceWriter.saveNew(link, "new"));
        assertEquals("keep", Files.readString(original));
        assertEquals(original, Files.readSymbolicLink(link));
    }

    @Test
    void missingParentCreatesNoDestination() {
        Path target = directory.resolve("missing").resolve("report.txt");
        assertThrows(java.io.IOException.class, () -> EvidenceWriter.saveNew(target, "preview"));
        assertFalse(Files.exists(target));
    }

    @Test
    void concurrentExportsPublishExactlyOneCompletePreview() throws Exception {
        Path target = directory.resolve("raced.txt");
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var candidates = List.of("first complete preview", "second complete preview");
            var futures =
                    candidates.stream()
                            .map(
                                    candidate ->
                                            executor.submit(
                                                    () -> {
                                                        start.await();
                                                        try {
                                                            EvidenceWriter.saveNew(
                                                                    target, candidate);
                                                            return candidate;
                                                        } catch (FileAlreadyExistsException e) {
                                                            return null;
                                                        }
                                                    }))
                            .toList();
            start.countDown();
            String winner = null;
            int successes = 0;
            for (var future : futures) {
                String result = future.get(5, TimeUnit.SECONDS);
                if (result != null) {
                    winner = result;
                    successes++;
                }
            }
            assertEquals(1, successes);
            assertEquals(winner, Files.readString(target));
            try (var files = Files.list(directory)) {
                assertEquals(1, files.count());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
