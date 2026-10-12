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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionHook;
import org.zaproxy.zap.control.AddOn;
import org.zaproxy.zap.control.AddOnClassLoader;

class PackagedAddOnTest {
    @Test
    void zapCanParseAndLoadThePackagedExtensionAndItsTranslationsOffline() throws Exception {
        Path file = Path.of(System.getProperty("privateevidence.package"));
        AddOn addOn = new AddOn(file);
        assertEquals("privateevidence", addOn.getId());
        assertEquals(
                List.of("org.zaproxy.addon.privateevidence.ExtensionPrivateEvidence"),
                addOn.getExtensions());
        try (var archive = new ZipFile(file.toFile())) {
            assertTrue(archive.getEntry("META-INF/LICENSE") != null);
            assertTrue(archive.getEntry("META-INF/NOTICE") != null);
        }
        try (var loader =
                new AddOnClassLoader(file.toUri().toURL(), Extension.class.getClassLoader())) {
            Class<?> extensionClass = loader.loadClass(addOn.getExtensions().get(0));
            assertEquals(loader, extensionClass.getClassLoader());
            Extension extension = (Extension) extensionClass.getDeclaredConstructor().newInstance();
            assertEquals("ExtensionPrivateEvidence", extension.getName());
            assertEquals("privateevidence", extension.getI18nPrefix());
            ResourceBundle messages =
                    ResourceBundle.getBundle(
                            extensionClass.getPackageName() + ".resources.Messages",
                            Locale.ENGLISH,
                            loader);
            assertEquals("Private Evidence", messages.getString("privateevidence.name"));
            extension.setMessages(messages);
            var hook = new ExtensionHook(null, null);
            extension.hook(hook);
            assertTrue(hook.getProxyListenerList().isEmpty());
            assertTrue(hook.getSessionListenerList().isEmpty());
        }
    }
}
