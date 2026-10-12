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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

final class EvidenceWriter {
    private EvidenceWriter() {}

    /** Stages UTF-8 bytes in a private sibling file and never replaces an existing destination. */
    static void saveNew(Path destination, String preview) throws IOException {
        Path target = destination.toAbsolutePath();
        Path parent = target.getParent();
        Path temporary;
        if (Files.getFileStore(parent).supportsFileAttributeView("posix")) {
            temporary =
                    Files.createTempFile(
                            parent,
                            ".private-evidence-",
                            ".tmp",
                            PosixFilePermissions.asFileAttribute(
                                    PosixFilePermissions.fromString("rw-------")));
        } else {
            temporary = Files.createTempFile(parent, ".private-evidence-", ".tmp");
        }
        try {
            Files.writeString(temporary, preview, StandardCharsets.UTF_8);
            // A hard link publishes complete bytes with atomic CREATE_NEW semantics. A plain
            // move can race with another writer after checking whether the target exists.
            // Unsupported filesystems fail closed instead of risking replacement/partial output.
            Files.createLink(target, temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
