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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.view.messagecontainer.http.DefaultSelectableHttpMessagesContainer;

class ReviewMenuTest {
    @Test
    void incompleteSessionOperationFailsClosedWithGuidanceAndSuccessfulRetryRestoresReview()
            throws Exception {
        var extension = new ExtensionPrivateEvidence();
        var messages =
                ResourceBundle.getBundle("org.zaproxy.addon.privateevidence.resources.Messages");
        extension.setMessages(messages);
        var source = EvidenceSummaryTest.message("https://example.invalid/", 200, "");
        var container =
                new DefaultSelectableHttpMessagesContainer(
                        "Synthetic QA", new JPanel(), List.of(source), List.of(source));
        SwingUtilities.invokeAndWait(
                () -> {
                    var menu = extension.new ReviewMenu();
                    extension.sessionAboutToChange(null);
                    assertTrue(menu.isEnableForMessageContainer(container));
                    assertFalse(menu.isEnabled());
                    assertEquals(
                            messages.getString("privateevidence.popup.sessionchanging"),
                            menu.getToolTipText());
                    extension.sessionChanged(null);
                    assertTrue(menu.isEnableForMessageContainer(container));
                    assertTrue(menu.isEnabled());
                    assertEquals(
                            messages.getString("privateevidence.popup.tooltip"),
                            menu.getToolTipText());
                });
    }

    @Test
    void openingAndDismissingContextMenuNeverLoadLazyStoredMessagesOnTheEventThread()
            throws Exception {
        var extension = new ExtensionPrivateEvidence();
        extension.setMessages(
                ResourceBundle.getBundle("org.zaproxy.addon.privateevidence.resources.Messages"));
        AtomicInteger reads = new AtomicInteger();
        List<HttpMessage> lazy =
                new AbstractList<>() {
                    @Override
                    public int size() {
                        return 2;
                    }

                    @Override
                    public HttpMessage get(int index) {
                        reads.incrementAndGet();
                        throw new AssertionError("Opening a context menu must not load bodies.");
                    }
                };
        var container =
                new DefaultSelectableHttpMessagesContainer("Synthetic QA", new JPanel()) {
                    @Override
                    public List<HttpMessage> getSelectedMessages() {
                        return lazy;
                    }

                    @Override
                    public int getNumberOfSelectedMessages() {
                        return lazy.size();
                    }

                    @Override
                    public boolean hasSelectedMessages() {
                        return true;
                    }
                };
        SwingUtilities.invokeAndWait(
                () -> {
                    var menu = extension.new ReviewMenu();
                    assertTrue(menu.isEnableForMessageContainer(container));
                    assertTrue(menu.isEnabled());
                    menu.dismissed(null);
                    assertTrue(menu.isEnableForMessageContainer(container));
                    extension.sessionAboutToChange(null);
                    extension.sessionChanged(null);
                    menu.performActions(lazy);
                });
        assertEquals(0, reads.get());
    }
}
