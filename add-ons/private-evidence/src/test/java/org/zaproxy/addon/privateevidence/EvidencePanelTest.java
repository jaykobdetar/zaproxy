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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class EvidencePanelTest {
    @Test
    void previewStartsPrivateAndPathDisclosureRequiresAnExplicitCheckboxAction() throws Exception {
        var summary =
                EvidenceSummary.capture(
                        EvidenceSummaryTest.message(
                                "https://example.invalid/private-path?token=secret",
                                200,
                                "secret"));
        var messages =
                ResourceBundle.getBundle("org.zaproxy.addon.privateevidence.resources.Messages");
        SwingUtilities.invokeAndWait(
                () -> {
                    var panel = new EvidencePanel(List.of(summary), messages);
                    assertFalse(panel.previewText().contains("/private-path"));
                    assertFalse(panel.previewText().contains("secret"));
                    List<Component> components = descendants(panel);
                    var paths =
                            components.stream()
                                    .filter(JCheckBox.class::isInstance)
                                    .map(JCheckBox.class::cast)
                                    .findFirst()
                                    .orElseThrow();
                    assertFalse(paths.isSelected());
                    paths.doClick();
                    assertTrue(panel.previewText().contains("/private-path"));
                    assertFalse(panel.previewText().contains("secret"));
                    paths.doClick();
                    assertFalse(panel.previewText().contains("/private-path"));
                    var searchLabel =
                            components.stream()
                                    .filter(JLabel.class::isInstance)
                                    .map(JLabel.class::cast)
                                    .filter(
                                            label ->
                                                    label.getText()
                                                            .equals(
                                                                    messages.getString(
                                                                            "privateevidence.search")))
                                    .findFirst()
                                    .orElseThrow();
                    assertNotNull(searchLabel.getLabelFor());
                });
    }

    private static List<Component> descendants(Container parent) {
        List<Component> result = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            result.add(component);
            if (component instanceof Container container) {
                result.addAll(descendants(container));
            }
        }
        return result;
    }

    @Test
    void searchActionsSelectLiteralMatchesInTheCurrentlySelectedTab() throws Exception {
        var left =
                EvidenceSummary.capture(
                        EvidenceSummaryTest.message("https://example.invalid/one", 200, ""));
        var right =
                EvidenceSummary.capture(
                        EvidenceSummaryTest.message("https://example.invalid/two", 404, ""));
        var messages =
                ResourceBundle.getBundle("org.zaproxy.addon.privateevidence.resources.Messages");
        SwingUtilities.invokeAndWait(
                () -> {
                    var panel = new EvidencePanel(List.of(left, right), messages);
                    var components = descendants(panel);
                    var query =
                            components.stream()
                                    .filter(JTextField.class::isInstance)
                                    .map(JTextField.class::cast)
                                    .findFirst()
                                    .orElseThrow();
                    var button =
                            components.stream()
                                    .filter(JButton.class::isInstance)
                                    .map(JButton.class::cast)
                                    .filter(
                                            candidate ->
                                                    candidate
                                                            .getText()
                                                            .equals(
                                                                    messages.getString(
                                                                            "privateevidence.search.next")))
                                    .findFirst()
                                    .orElseThrow();
                    var tabs =
                            components.stream()
                                    .filter(JTabbedPane.class::isInstance)
                                    .map(JTabbedPane.class::cast)
                                    .findFirst()
                                    .orElseThrow();
                    var preview =
                            descendants((Container) tabs.getComponentAt(0)).stream()
                                    .filter(JTextArea.class::isInstance)
                                    .map(JTextArea.class::cast)
                                    .findFirst()
                                    .orElseThrow();
                    query.setText("[omitted]");
                    query.postActionEvent();
                    assertEquals("[omitted]", preview.getSelectedText());
                    int first = preview.getSelectionStart();
                    button.doClick();
                    assertTrue(preview.getSelectionStart() > first);
                    tabs.setSelectedIndex(1);
                    var comparison =
                            descendants((Container) tabs.getComponentAt(1)).stream()
                                    .filter(JTextArea.class::isInstance)
                                    .map(JTextArea.class::cast)
                                    .findFirst()
                                    .orElseThrow();
                    assertTrue(
                            comparison
                                    .getText()
                                    .contains(
                                            messages.getString("privateevidence.compare.notice")));
                    query.setText("404");
                    button.doClick();
                    assertEquals("404", comparison.getSelectedText());
                    assertFalse(panel.previewText().contains("/one"));
                });
    }
}
