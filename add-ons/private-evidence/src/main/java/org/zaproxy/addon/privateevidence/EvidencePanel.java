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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

@SuppressWarnings("serial")
final class EvidencePanel extends JPanel {
    private final List<EvidenceSummary> summaries;
    private final ResourceBundle messages;
    private final JCheckBox paths;
    private final JTextArea preview = new JTextArea(24, 80);
    private final JTextArea comparison = new JTextArea(24, 80);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField query = new JTextField(22);
    private final JLabel status = new JLabel(" ");

    EvidencePanel(List<EvidenceSummary> summaries, ResourceBundle messages) {
        super(new BorderLayout(8, 8));
        this.summaries = List.copyOf(summaries);
        this.messages = messages;
        paths = new JCheckBox(text("paths"));
        paths.setMnemonic('P');
        paths.addActionListener(e -> refresh());

        JPanel options = new JPanel(new BorderLayout(4, 4));
        JTextArea notice = new JTextArea(text("notice"), 2, 60);
        notice.setEditable(false);
        notice.setLineWrap(true);
        notice.setWrapStyleWord(true);
        notice.setOpaque(false);
        notice.setFont(new JLabel().getFont());
        notice.getAccessibleContext().setAccessibleName(text("notice"));
        options.add(notice, BorderLayout.NORTH);
        options.add(paths, BorderLayout.SOUTH);
        add(options, BorderLayout.NORTH);

        preview.setEditable(false);
        comparison.setEditable(false);
        preview.getAccessibleContext().setAccessibleName(text("preview"));
        comparison.getAccessibleContext().setAccessibleName(text("comparison"));
        tabs.addTab(text("preview"), new JScrollPane(preview));
        tabs.addTab(text("comparison"), new JScrollPane(comparison));
        add(tabs, BorderLayout.CENTER);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JLabel queryLabel = new JLabel(text("search"));
        queryLabel.setLabelFor(query);
        queryLabel.setDisplayedMnemonic('F');
        query.getAccessibleContext().setAccessibleName(text("search"));
        JButton next = new JButton(text("search.next"));
        next.setMnemonic('N');
        next.addActionListener(e -> findNext());
        query.addActionListener(e -> findNext());
        search.add(queryLabel);
        search.add(query);
        search.add(next);
        search.add(status);
        add(search, BorderLayout.SOUTH);
        refresh();
    }

    private String text(String suffix) {
        return EvidenceSummary.text(messages, suffix);
    }

    private void refresh() {
        preview.setText(EvidenceDocument.render(summaries, paths.isSelected(), messages));
        comparison.setText(EvidenceDocument.compare(summaries, paths.isSelected(), messages));
        preview.setCaretPosition(0);
        comparison.setCaretPosition(0);
        status.setText(" ");
    }

    private void findNext() {
        JTextArea area = tabs.getSelectedIndex() == 0 ? preview : comparison;
        var match = LiteralSearch.findNext(area.getText(), query.getText(), area.getSelectionEnd());
        status.setText(text(match.isPresent() ? "search.found" : "search.absent"));
        if (match.isPresent()) {
            area.requestFocusInWindow();
            area.select(match.getAsInt(), match.getAsInt() + query.getText().length());
        }
    }

    String previewText() {
        return preview.getText();
    }
}
