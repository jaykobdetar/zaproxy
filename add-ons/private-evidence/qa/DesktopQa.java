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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.SessionListener;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.ZAP;
import org.zaproxy.zap.view.messagecontainer.http.DefaultSelectableHttpMessagesContainer;
import org.zaproxy.zap.view.popup.PopupMenuItemHttpMessageContainer;

/** Disposable process-local QA: starts the real app, drives only synthetic message UI. */
@SuppressWarnings("deprecation")
public class DesktopQa {
    static final Path RESULTS = Path.of(System.getProperty("qa.results"));
    static final List<String> checks = new ArrayList<>();
    static JFrame fixture;
    static JPanel fixturePanel;

    static <T> T edt(Callable<T> work) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return work.call();
        FutureTask<T> task = new FutureTask<>(work);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks.add("PASS " + description);
        System.out.println("QA PASS " + description);
    }

    static List<Component> children(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            result.add(child);
            if (child instanceof Container) result.addAll(children((Container) child));
            if (child instanceof JMenu) result.addAll(children(((JMenu) child).getPopupMenu()));
        }
        return result;
    }

    static <T> T find(Container parent, Class<T> type, Predicate<T> predicate) {
        return children(parent).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .filter(predicate)
                .findFirst()
                .orElseThrow();
    }

    static JDialog dialog(String title) throws Exception {
        return edt(
                () ->
                        Arrays.stream(Window.getWindows())
                                .filter(Window::isShowing)
                                .filter(JDialog.class::isInstance)
                                .map(JDialog.class::cast)
                                .filter(w -> title.equals(w.getTitle()))
                                .findFirst()
                                .orElse(null));
    }

    static void waitFor(Callable<Boolean> condition, String description) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (System.nanoTime() < deadline) {
            if (condition.call()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out: " + description);
    }

    static HttpMessage message(String target, int code, String body) throws Exception {
        HttpMessage msg = new HttpMessage();
        msg.setRequestHeader(
                "POST "
                        + target
                        + " HTTP/1.1\r\nAuthorization: Bearer auth-secret\r\nCookie: cookie-secret\r\nContent-Type: application/json\r\n\r\n");
        msg.setRequestBody(body);
        msg.setResponseHeader(
                "HTTP/1.1 "
                        + code
                        + " reason-secret\r\nSet-Cookie: cookie-secret\r\nContent-Type: application/json; charset=charset-secret\r\n\r\n");
        msg.setResponseBody(body);
        return msg;
    }

    static JMenuItem menu(List<HttpMessage> messages) throws Exception {
        return edt(
                () -> {
                    var container =
                            new DefaultSelectableHttpMessagesContainer(
                                    "Synthetic QA", fixturePanel) {
                                @Override
                                public List<HttpMessage> getSelectedMessages() {
                                    return messages;
                                }

                                @Override
                                public int getNumberOfSelectedMessages() {
                                    return messages.size();
                                }

                                @Override
                                public boolean hasSelectedMessages() {
                                    return !messages.isEmpty();
                                }
                            };
                    var popup = View.getSingleton().getPopupMenu();
                    popup.show(container, 10, 10);
                    var item =
                            find(
                                    popup,
                                    JMenuItem.class,
                                    m -> "Review Private Evidence...".equals(m.getText()));
                    return item;
                });
    }

    static void hidePopup() throws Exception {
        edt(
                () -> {
                    View.getSingleton().getPopupMenu().setVisible(false);
                    return null;
                });
    }

    static JDialog review(List<HttpMessage> messages) throws Exception {
        final JMenuItem[] selected = new JMenuItem[1];
        waitFor(
                () -> {
                    try {
                        selected[0] = menu(messages);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                },
                "registered Private Evidence menu");
        var item = selected[0];
        check(edt(item::isEnabled), "Synthetic selection enables Private Evidence in Safe mode");
        edt(
                () -> {
                    item.doClick();
                    return null;
                });
        hidePopup();
        waitFor(() -> dialog("Private Evidence") != null, "Private Evidence dialog");
        return dialog("Private Evidence");
    }

    static JTextArea preview(JDialog dialog) throws Exception {
        return edt(
                () ->
                        find(
                                dialog,
                                JTextArea.class,
                                t ->
                                        "Evidence Preview"
                                                .equals(
                                                        t.getAccessibleContext()
                                                                .getAccessibleName())));
    }

    static JButton button(Container parent, String text) throws Exception {
        return edt(() -> find(parent, JButton.class, b -> text.equals(b.getText())));
    }

    static JFileChooser chooser() throws Exception {
        return edt(
                () -> {
                    for (Window w : Window.getWindows())
                        if (w.isShowing() && w instanceof Container) {
                            for (Component c : children((Container) w))
                                if (c instanceof JFileChooser) return (JFileChooser) c;
                        }
                    return null;
                });
    }

    static void startSave(JDialog dialog) throws Exception {
        JButton save = button(dialog, "Save Preview...");
        SwingUtilities.invokeLater(save::doClick);
        waitFor(() -> chooser() != null, "save chooser");
    }

    static void dismissMessage(String fragment) throws Exception {
        waitFor(
                () ->
                        edt(
                                () -> {
                                    for (Window w : Window.getWindows())
                                        if (w.isShowing() && w instanceof JDialog) {
                                            for (Component c : children((Container) w))
                                                if ((c instanceof JLabel
                                                                && ((JLabel) c).getText() != null
                                                                && ((JLabel) c)
                                                                        .getText()
                                                                        .contains(fragment))
                                                        || (c instanceof JTextComponent
                                                                && ((JTextComponent) c)
                                                                        .getText()
                                                                        .contains(fragment))) {
                                                    find(
                                                                    (Container) w,
                                                                    JButton.class,
                                                                    b -> "OK".equals(b.getText()))
                                                            .doClick();
                                                    return true;
                                                }
                                        }
                                    return false;
                                }),
                "message " + fragment);
    }

    static void close(JDialog dialog) throws Exception {
        edt(
                () -> {
                    find(dialog, JButton.class, b -> "Close".equals(b.getText())).doClick();
                    return null;
                });
        waitFor(() -> !edt(dialog::isShowing), "review close");
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RESULTS);
        Thread boot =
                new Thread(
                        () -> {
                            try {
                                ZAP.main(args);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        },
                        "QA-ZAP-bootstrap");
        boot.start();
        try {
            waitFor(
                    () ->
                            edt(
                                    () ->
                                            Arrays.stream(Window.getWindows())
                                                    .anyMatch(
                                                            w ->
                                                                    w instanceof JFrame
                                                                            && w.isShowing()
                                                                            && ((JFrame) w)
                                                                                    .getTitle()
                                                                                    .contains(
                                                                                            "ZAP"))),
                    "ZAP main frame");
            waitFor(
                    () ->
                            Control.getSingleton() != null
                                    && Control.getSingleton()
                                                    .getExtensionLoader()
                                                    .getExtension("ExtensionPrivateEvidence")
                                            != null,
                    "packaged add-on loaded");
            check(
                    "safe".equals(Control.getSingleton().getMode().name()),
                    "ZAP uses disposable Safe-mode profile");
            check(
                    Control.getSingleton()
                                    .getExtensionLoader()
                                    .getExtension("ExtensionPrivateEvidence")
                                    .getMessages()
                            != null,
                    "Real app loads packaged extension and translations");
            edt(
                    () -> {
                        fixture = new JFrame("Synthetic Stored Traffic QA");
                        fixturePanel = new JPanel();
                        fixturePanel.add(
                                new JLabel("Synthetic messages only; no requests are sent."));
                        fixture.add(fixturePanel);
                        fixture.setSize(460, 120);
                        fixture.setLocation(10, 10);
                        fixture.setVisible(true);
                        return null;
                    });
            var left =
                    message(
                            "https://user-secret:password-secret@host-secret.invalid/private-left?token=query-secret#fragment-secret",
                            200,
                            "body-secret");
            var right =
                    message(
                            "https://other.invalid/private-right?token=query-secret",
                            404,
                            "second-body-secret");
            JDialog review = review(List.of(left, right));
            JTextArea preview = preview(review);
            String output = edt(preview::getText);
            for (String secret :
                    List.of(
                            "user-secret",
                            "password-secret",
                            "host-secret",
                            "private-left",
                            "query-secret",
                            "fragment-secret",
                            "auth-secret",
                            "cookie-secret",
                            "body-secret",
                            "reason-secret",
                            "charset-secret")) {
                check(!output.contains(secret), "Default preview omits " + secret);
            }
            edt(
                    () -> {
                        JTextField query =
                                find(
                                        review,
                                        JTextField.class,
                                        t ->
                                                "Find Literal Text:"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        query.setText("[omitted]");
                        query.postActionEvent();
                        return null;
                    });
            check(
                    "[omitted]".equals(edt(preview::getSelectedText)),
                    "Literal search selects preview match");
            edt(
                    () -> {
                        find(review, JTabbedPane.class, t -> true).setSelectedIndex(1);
                        return null;
                    });
            JTextArea comparison =
                    edt(
                            () ->
                                    find(
                                            review,
                                            JTextArea.class,
                                            t ->
                                                    "Comparison of First Two Messages"
                                                            .equals(
                                                                    t.getAccessibleContext()
                                                                            .getAccessibleName())));
            check(
                    edt(comparison::getText).contains("Second: 404"),
                    "Comparison shows status difference");
            edt(
                    () -> {
                        JCheckBox paths =
                                find(
                                        review,
                                        JCheckBox.class,
                                        c -> c.getText().startsWith("Include URL Paths"));
                        paths.doClick();
                        return null;
                    });
            String withPaths = edt(preview::getText);
            check(
                    withPaths.contains("/private-left")
                            && !withPaths.contains("host-secret")
                            && !withPaths.contains("query-secret"),
                    "Path opt-in excludes host and query");
            edt(
                    () -> {
                        find(
                                        review,
                                        JCheckBox.class,
                                        c -> c.getText().startsWith("Include URL Paths"))
                                .doClick();
                        return null;
                    });
            startSave(review);
            edt(
                    () -> {
                        chooser().cancelSelection();
                        return null;
                    });
            waitFor(() -> chooser() == null, "cancel save chooser");
            check(
                    !Files.exists(RESULTS.resolve("private-evidence.txt")),
                    "Cancel leaves no exported file");
            startSave(review);
            Path target = RESULTS.resolve("evidence.txt");
            edt(
                    () -> {
                        chooser().setSelectedFile(target.toFile());
                        chooser().approveSelection();
                        return null;
                    });
            waitFor(() -> Files.exists(target), "export complete");
            dismissMessage("Preview saved");
            check(
                    output.equals(Files.readString(target)),
                    "Desktop export equals exact private preview");
            startSave(review);
            edt(
                    () -> {
                        chooser().setSelectedFile(target.toFile());
                        chooser().approveSelection();
                        return null;
                    });
            dismissMessage("Choose a new filename");
            check(
                    output.equals(Files.readString(target)),
                    "Repeated save preserves existing bytes");
            edt(
                    () -> {
                        review.toFront();
                        review.requestFocus();
                        return null;
                    });
            Thread.sleep(150);
            ImageIO.write(
                    edt(
                            () -> {
                                BufferedImage image =
                                        new BufferedImage(
                                                review.getWidth(),
                                                review.getHeight(),
                                                BufferedImage.TYPE_INT_RGB);
                                Graphics graphics = image.getGraphics();
                                review.printAll(graphics);
                                graphics.dispose();
                                return image;
                            }),
                    "png",
                    RESULTS.resolve("private-evidence-panel.png").toFile());
            close(review);
            check(dialog("Private Evidence") == null, "Close disposes review dialog");
            var reopened = review(List.of(left));
            check(
                    !edt(preview(reopened)::getText).contains("/private-left"),
                    "Reopening resets optional path disclosure");
            close(reopened);
            var hundredOne = Collections.nCopies(101, left);
            var tooMany = menu(hundredOne);
            check(!edt(tooMany::isEnabled), "101-message selection is disabled");
            hidePopup();
            Control.getSingleton()
                    .runCommandLineNewSession(RESULTS.resolve("session-a.session").toString());
            var savedA =
                    new HistoryReference(
                            Model.getSingleton().getSession(), HistoryReference.TYPE_MANUAL, left);
            var oldReference = new HistoryReference(savedA.getHistoryId());
            Control.getSingleton()
                    .runCommandLineNewSession(RESULTS.resolve("session-b.session").toString());
            var savedB =
                    new HistoryReference(
                            Model.getSingleton().getSession(), HistoryReference.TYPE_MANUAL, right);
            check(
                    savedA.getHistoryId() == savedB.getHistoryId(),
                    "Disposable sessions reuse history row IDs");
            check(
                    oldReference.getHttpMessage().getResponseHeader().getStatusCode() == 404,
                    "Old lazy reference reads replacement-session row: capture needs session-change guard");
            var malformed = new HttpMessage();
            malformed.setRequestHeader("CONNECT private-host.invalid HTTP/1.1\r\n\r\n");
            malformed.setResponseHeader("HTTP/1.1 799 Unrecognised\r\n\r\n");
            var missing = new HttpMessage();
            missing.setRequestHeader("GET / HTTP/1.1\r\n\r\n");
            var unusual = review(List.of(malformed, missing));
            edt(
                    () -> {
                        find(
                                        unusual,
                                        JCheckBox.class,
                                        c -> c.getText().startsWith("Include URL Paths"))
                                .doClick();
                        return null;
                    });
            String unusualText = edt(preview(unusual)::getText);
            check(
                    !unusualText.contains("private-host"),
                    "CONNECT authority stays omitted with path disclosure enabled");
            check(
                    unusualText.contains("Response status: [omitted]")
                            && unusualText.contains("Response status: [no response]"),
                    "Unrecognised response status and absent response are distinct");
            close(unusual);
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var reads = new java.util.concurrent.atomic.AtomicInteger();
            var eventThreadRead = new java.util.concurrent.atomic.AtomicBoolean();
            List<HttpMessage> lazy =
                    new AbstractList<>() {
                        @Override
                        public int size() {
                            return 1;
                        }

                        @Override
                        public HttpMessage get(int index) {
                            reads.incrementAndGet();
                            if (SwingUtilities.isEventDispatchThread()) eventThreadRead.set(true);
                            entered.countDown();
                            try {
                                if (!release.await(10, TimeUnit.SECONDS))
                                    throw new AssertionError("QA lazy retrieval timed out");
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            try {
                                return oldReference.getHttpMessage();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
            var pendingMenu = (PopupMenuItemHttpMessageContainer) menu(List.of(left));
            hidePopup();
            var lazyContainer =
                    new DefaultSelectableHttpMessagesContainer("Synthetic QA", fixturePanel) {
                        @Override
                        public List<HttpMessage> getSelectedMessages() {
                            return lazy;
                        }

                        @Override
                        public int getNumberOfSelectedMessages() {
                            return 1;
                        }

                        @Override
                        public boolean hasSelectedMessages() {
                            return true;
                        }
                    };
            edt(
                    () -> {
                        pendingMenu.isEnableForMessageContainer(lazyContainer);
                        pendingMenu.dismissed(null);
                        return null;
                    });
            check(
                    reads.get() == 0,
                    "Packaged Private Evidence menu enablement/cancel does not retrieve lazy bodies");
            edt(
                    () -> {
                        pendingMenu.isEnableForMessageContainer(lazyContainer);
                        return null;
                    });
            JMenuItem clickedMenu = pendingMenu;
            edt(
                    () -> {
                        clickedMenu.doClick();
                        return null;
                    });
            hidePopup();
            check(
                    entered.await(5, TimeUnit.SECONDS),
                    "Evidence worker begins lazy retrieval in background");
            Control.getSingleton()
                    .runCommandLineNewSession(RESULTS.resolve("session-c.session").toString());
            var replacement =
                    new HistoryReference(
                            Model.getSingleton().getSession(),
                            HistoryReference.TYPE_MANUAL,
                            message(
                                    "https://example.invalid/replacement",
                                    201,
                                    "replacement-secret"));
            release.countDown();
            waitFor(
                    () -> {
                        var item = menu(List.of(left));
                        boolean enabled = edt(item::isEnabled);
                        hidePopup();
                        return enabled;
                    },
                    "pending capture completion after session switch");
            check(!eventThreadRead.get(), "Stored message retrieval stays off the UI thread");
            check(
                    dialog("Private Evidence") == null,
                    "Session switch discards in-flight evidence instead of displaying replacement-session data");
            var afterSwitch = review(List.of(left));
            check(
                    !edt(preview(afterSwitch)::getText).contains("replacement-secret"),
                    "Fresh review remains usable and private after session switch");
            close(afterSwitch);
            Path blocker = RESULTS.resolve("not-a-directory");
            Files.writeString(blocker, "synthetic blocker");
            CountDownLatch failedSave = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Exception> failedSaveError =
                    new java.util.concurrent.atomic.AtomicReference<>();
            SessionListener failedCallback =
                    new SessionListener() {
                        public void sessionOpened(java.io.File f, Exception e) {}

                        public void sessionSnapshot(Exception e) {}

                        public void sessionSaved(Exception e) {
                            failedSaveError.set(e);
                            failedSave.countDown();
                        }
                    };
            Control.getSingleton()
                    .saveSession(blocker.resolve("bad.session").toString(), failedCallback);
            check(
                    failedSave.await(5, TimeUnit.SECONDS) && failedSaveError.get() != null,
                    "Disposable failed Save Session produces an ordinary file error");
            var failedMenu = menu(List.of(left));
            check(
                    !edt(failedMenu::isEnabled)
                            && edt(failedMenu::getToolTipText).contains("successfully save"),
                    "Incomplete session notification fails closed with explicit recovery guidance");
            hidePopup();
            CountDownLatch savedAgain = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Exception> retryError =
                    new java.util.concurrent.atomic.AtomicReference<>();
            SessionListener retryCallback =
                    new SessionListener() {
                        public void sessionOpened(java.io.File f, Exception e) {}

                        public void sessionSnapshot(Exception e) {}

                        public void sessionSaved(Exception e) {
                            retryError.set(e);
                            savedAgain.countDown();
                        }
                    };
            Control.getSingleton()
                    .saveSession(RESULTS.resolve("recovered.session").toString(), retryCallback);
            check(
                    savedAgain.await(5, TimeUnit.SECONDS) && retryError.get() == null,
                    "Successful ordinary Save Session is a verified recovery path");
            var recovered = review(List.of(left));
            check(
                    !edt(preview(recovered)::getText).contains("replacement-secret"),
                    "Review is restored after successful session retry");
            close(recovered);
            Files.write(RESULTS.resolve("desktop-checks.txt"), checks);
            System.out.println("QA SUCCESS " + checks.size() + " checks");
            edt(
                    () -> {
                        Control.getSingleton().exit(false, null);
                        return null;
                    });
        } catch (Throwable e) {
            e.printStackTrace();
            Files.writeString(
                    RESULTS.resolve("desktop-failure.txt"),
                    e.toString() + "\n" + String.join("\n", checks));
            System.exit(2);
        }
    }
}
