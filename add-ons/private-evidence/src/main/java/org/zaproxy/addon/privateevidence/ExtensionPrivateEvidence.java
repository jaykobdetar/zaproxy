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
import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control.Mode;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.SessionChangedListener;
import org.parosproxy.paros.model.Session;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.view.messagecontainer.http.HttpMessageContainer;
import org.zaproxy.zap.view.popup.PopupMenuItemHttpMessageContainer;

/** Reads only existing messages. Registers no proxy, scanner, sender, or API hooks. */
public final class ExtensionPrivateEvidence extends ExtensionAdaptor
        implements SessionChangedListener {
    static final int MAX_MESSAGES = 100;
    private final CaptureGuard captureGuard = new CaptureGuard();

    public ExtensionPrivateEvidence() {
        super("ExtensionPrivateEvidence");
    }

    @Override
    public String getDescription() {
        return Constant.messages.getString("privateevidence.desc");
    }

    @Override
    public String getUIName() {
        return Constant.messages.getString("privateevidence.name");
    }

    @Override
    public void hook(ExtensionHook hook) {
        super.hook(hook);
        hook.addSessionListener(this);
        if (hasView()) {
            hook.getHookMenu().addPopupMenuItem(new ReviewMenu());
        }
    }

    @Override
    public void sessionAboutToChange(Session session) {
        captureGuard.beforeSessionChange();
    }

    @Override
    public void sessionChanged(Session session) {
        captureGuard.afterSessionChange();
    }

    @Override
    public void sessionScopeChanged(Session session) {}

    @Override
    public void sessionModeChanged(Mode mode) {}

    private String text(String suffix) {
        return EvidenceSummary.text(getMessages(), suffix);
    }

    private void showEvidence(List<EvidenceSummary> summaries) {
        JDialog dialog = new JDialog(getView().getMainFrame(), text("name"), false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        EvidencePanel panel = new EvidencePanel(summaries, getMessages());
        dialog.add(panel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton save = new JButton(text("export"));
        save.setMnemonic('S');
        save.addActionListener(e -> savePreview(dialog, panel.previewText(), save));
        JButton close = new JButton(text("close"));
        close.setMnemonic('C');
        close.addActionListener(e -> dialog.dispose());
        buttons.add(save);
        buttons.add(close);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(getView().getMainFrame());
        dialog.setVisible(true);
    }

    private void savePreview(JDialog dialog, String preview, JButton save) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(text("save.filename")));
        if (chooser.showSaveDialog(dialog) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        var target = chooser.getSelectedFile().toPath();
        save.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                EvidenceWriter.saveNew(target, preview);
                return null;
            }

            @Override
            protected void done() {
                save.setEnabled(true);
                try {
                    get();
                    if (dialog.isShowing()) {
                        JOptionPane.showMessageDialog(
                                dialog,
                                text("save.success"),
                                text("name"),
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    String key =
                            e.getCause() instanceof FileAlreadyExistsException
                                    ? "save.exists"
                                    : "error";
                    if (dialog.isShowing()) {
                        JOptionPane.showMessageDialog(
                                dialog, text(key), text("name"), JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    @SuppressWarnings("serial")
    final class ReviewMenu extends PopupMenuItemHttpMessageContainer {
        private boolean preparing;
        private OptionalLong selectionToken = OptionalLong.empty();

        ReviewMenu() {
            super(text("popup"), true);
            setToolTipText(text("popup.tooltip"));
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        protected boolean isButtonEnabledForNumberOfSelectedMessages(int count) {
            boolean changing = captureGuard.isChanging();
            setToolTipText(text(changing ? "popup.sessionchanging" : "popup.tooltip"));
            return !preparing && !changing && count > 0 && count <= MAX_MESSAGES;
        }

        @Override
        protected boolean isButtonEnabledForSelectedMessages(HttpMessageContainer container) {
            // Selection-count validation above needs no message/body retrieval on the EDT.
            selectionToken = captureGuard.begin();
            return selectionToken.isPresent();
        }

        @Override
        protected void performActions(List<HttpMessage> messages) {
            if (preparing || messages.isEmpty() || messages.size() > MAX_MESSAGES) {
                return;
            }
            var token = selectionToken;
            if (token.isEmpty() || !token.equals(captureGuard.begin())) {
                return;
            }
            preparing = true;
            new SwingWorker<List<EvidenceSummary>, Void>() {
                @Override
                protected List<EvidenceSummary> doInBackground() {
                    return captureGuard.capture(messages, token.getAsLong());
                }

                @Override
                protected void done() {
                    preparing = false;
                    try {
                        var summaries = get();
                        captureGuard.publishIfCurrent(
                                token.getAsLong(), () -> showEvidence(summaries));
                    } catch (Exception e) {
                        if (!(e.getCause() instanceof CancellationException)) {
                            getView().showWarningDialog(text("error"));
                        }
                    }
                }
            }.execute();
        }

        @Override
        public void performAction(HttpMessage message) {
            // Handled together so the user reviews exactly one selected batch.
        }
    }
}
