/*
 * Copyright 2026 jaykobdetar
 * Licensed under the Apache License, Version 2.0.
 * See the repository LICENSE for the full license text.
 */
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.ZAP;
import org.zaproxy.zap.extension.search.ExtensionSearch;
import org.zaproxy.zap.extension.search.SearchPanel;
import org.zaproxy.zap.extension.search.SearchResultsTable;
import org.zaproxy.zap.extension.search.SearchResultsTableModel;

/** Real fork desktop, disposable database and synthetic records; never sends traffic. */
@SuppressWarnings("deprecation")
public class OfflineSearchQa {
    static final Path RESULTS = Path.of(System.getProperty("qa.results"));
    static final List<String> checks = new ArrayList<>();
    static SearchPanel panel;
    static JCheckBox literal;
    static JCheckBox inverse;
    static JTextField query;
    static JComboBox<?> type;
    static JButton search;
    static SearchResultsTableModel results;

    static <T> T edt(Callable<T> work) throws Exception {
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
        List<Component> all = new ArrayList<>();
        for (Component child : root.getComponents()) {
            all.add(child);
            if (child instanceof Container) all.addAll(children((Container) child));
        }
        return all;
    }

    static <T> T find(Container root, Class<T> cls, Predicate<T> predicate) {
        return children(root).stream().filter(cls::isInstance).map(cls::cast)
                .filter(predicate).findFirst().orElseThrow();
    }

    static void waitFor(Callable<Boolean> condition, String description) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
        while (System.nanoTime() < deadline) {
            if (condition.call()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out: " + description);
    }

    static void complete() throws Exception {
        waitFor(() -> Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(t -> t.isAlive() && "ZAP-SearchThread".equals(t.getName()))
                        && edt(() -> search.isEnabled()
                        && children(panel).stream().filter(JLabel.class::isInstance)
                        .map(JLabel.class::cast).anyMatch(l -> "Complete".equals(l.getText()))),
                "offline search completion");
    }

    static void runSearch(String text, boolean asLiteral, String searchType, boolean invert)
            throws Exception {
        edt(() -> {
            if (literal.isSelected() != asLiteral) literal.doClick();
            if (inverse.isSelected() != invert) inverse.doClick();
            for (int i = 0; i < type.getItemCount(); i++) {
                if (searchType.equals(type.getItemAt(i).toString())) {
                    type.setSelectedIndex(i);
                    break;
                }
            }
            query.setText(text);
            search.doClick();
            return null;
        });
        complete();
    }

    static HistoryReference store(String path, String requestBody, String responseBody)
            throws Exception {
        HttpMessage message = new HttpMessage();
        message.setRequestHeader("POST https://synthetic.invalid/" + path
                + " HTTP/1.1\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n");
        message.setRequestBody(requestBody);
        message.setResponseHeader("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n");
        message.setResponseBody(responseBody);
        return new HistoryReference(Model.getSingleton().getSession(),
                HistoryReference.TYPE_PROXIED, message);
    }

    static void render(int width, String filename) throws Exception {
        edt(() -> {
            var frame = View.getSingleton().getMainFrame();
            frame.setSize(width, 850);
            frame.validate();
            List<String> bounds = new ArrayList<>();
            children(panel).stream().filter(JToolBar.class::isInstance)
                    .map(JToolBar.class::cast).forEach(toolbar -> {
                        bounds.add(toolbar.getName() + " " + toolbar.getSize());
                        for (Component c : toolbar.getComponents()) {
                            bounds.add(c.getClass().getSimpleName() + " " + c.getBounds()
                                    + (c instanceof JButton ? " " + ((JButton) c).getText() : ""));
                        }
                    });
            Files.write(RESULTS.resolve("bounds-" + width + ".txt"), bounds);
            check(children(panel).stream().filter(JToolBar.class::isInstance)
                    .map(JToolBar.class::cast).allMatch(toolbar ->
                    Arrays.stream(toolbar.getComponents()).filter(Component::isVisible)
                    .allMatch(c -> c.getX() >= 0
                            && c.getX() + c.getWidth() <= toolbar.getWidth())),
                    "Toolbar controls fit at frame width " + width);
            check(literal.getWidth() >= literal.getPreferredSize().width,
                    "Literal label fits at frame width " + width);
            BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
            ImageIO.write(image, "png", RESULTS.resolve(filename).toFile());
            return null;
        });
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RESULTS);
        new Thread(() -> {
            try { ZAP.main(args); } catch (Exception e) { e.printStackTrace(); }
        }, "Offline-QA-bootstrap").start();
        try {
            waitFor(() -> Control.getSingleton() != null
                    && Control.getSingleton().getExtensionLoader()
                    .getExtension(ExtensionSearch.class) != null
                    && edt(() -> View.getSingleton().getMainFrame().isShowing())
                    && Thread.getAllStackTraces().keySet().stream().noneMatch(
                            t -> t.isAlive() && "ZAP-BootstrapGUI".equals(t.getName())), "fork desktop");
            Control.getSingleton().runCommandLineNewSession(
                    RESULTS.resolve("synthetic-search.session").toString());
            var extension = Control.getSingleton().getExtensionLoader()
                    .getExtension(ExtensionSearch.class);
            edt(() -> {
                extension.search("offline-bootstrap", ExtensionSearch.Type.Request, true, false);
                panel = find(View.getSingleton().getMainFrame(), SearchPanel.class, p -> true);
                literal = find(panel, JCheckBox.class, c -> "Literal".equals(c.getText()));
                JLabel inverseLabel = find(panel, JLabel.class, l -> "Inverse:".equals(l.getText()));
                inverse = (JCheckBox) inverseLabel.getLabelFor();
                query = find(panel, JTextField.class,
                        f -> "Search term".equals(f.getAccessibleContext().getAccessibleName()));
                type = find(panel, JComboBox.class, c -> true);
                search = find(panel, JButton.class, b -> "Search".equals(b.getText()));
                results = (SearchResultsTableModel) find(panel, SearchResultsTable.class,
                        t -> true).getModel();
                return null;
            });
            complete();
            check("safe".equals(Control.getSingleton().getMode().name()), "Disposable Safe mode");
            check(SearchPanel.class.getProtectionDomain().getCodeSource().getLocation().toString()
                            .contains("2.18.0-SNAPSHOT"), "Search loads rebuilt fork core JAR");
            check(!edt(literal::isSelected), "Regex remains the default");
            check(edt(() -> literal.isFocusable() && "Literal".equals(
                    literal.getAccessibleContext().getAccessibleName())), "Literal control is named and focusable");
            check(edt(() -> {
                for (String key : List.of("pressed SPACE", "released SPACE")) {
                    Object binding = literal.getInputMap().get(KeyStroke.getKeyStroke(key));
                    literal.getActionMap().get(binding).actionPerformed(
                            new ActionEvent(literal, ActionEvent.ACTION_PERFORMED, key));
                }
                boolean toggled = literal.isSelected()
                        && query.getToolTipText().contains("plain text");
                literal.doClick();
                return toggled;
            }), "Space action binding toggles Literal and updates the query prompt");
            var first = store("first", "prefix [item]+? \\E 日本語 caseToken café suffix", "neutral response");
            store("second", "prefix itemmm CASETOKEN CAFÉ suffix", "neutral response");
            var third = store("third", "plain request", "[item]+? response");
            runSearch("[item]+?", true, "Request", false);
            check(edt(() -> results.getRowCount() == 1
                    && results.getEntry(0).getHistoryId() == first.getHistoryId()
                    && "[item]+?".equals(results.getEntry(0).getSearchResult().getStringFound())),
                    "Literal metacharacters match the stored request exactly");
            runSearch("\\E", true, "Request", false);
            check(edt(() -> results.getRowCount() == 1), "Embedded quote terminator is literal");
            runSearch("日本語", true, "Request", false);
            check(edt(() -> results.getRowCount() == 1), "Unicode text is retained");
            runSearch("casetoken", true, "Request", false);
            check(edt(() -> results.getRowCount() == 2), "Inherited A-Z case folding is retained");
            check(edt(() -> {
                SearchResultsTable table = find(panel, SearchResultsTable.class, t -> true);
                table.setRowSelectionInterval(0, 0);
                find(panel, JButton.class, b -> "Next".equals(b.getText())).doClick();
                boolean advanced = table.getSelectedRow() == 1;
                find(panel, JButton.class, b -> "Previous".equals(b.getText())).doClick();
                return advanced && table.getSelectedRow() == 0;
            }), "Moved Next/Previous controls retain result navigation");
            runSearch("café", true, "Request", false);
            check(edt(() -> results.getRowCount() == 1), "Inherited non-ASCII case limitation is retained");
            runSearch("[item]+?", true, "Response", false);
            check(edt(() -> results.getRowCount() == 1
                    && results.getEntry(0).getHistoryId() == third.getHistoryId()),
                    "Response filter retains existing semantics");
            runSearch("[item]+?", true, "Request", true);
            check(edt(() -> results.getRowCount() == 2), "Inverse literal search retains existing semantics");
            runSearch("[item]+?", true, "Request", false);
            SwingUtilities.invokeLater(() -> { query.setText(""); search.doClick(); });
            waitFor(() -> edt(() -> Arrays.stream(Window.getWindows())
                    .filter(Window::isShowing).filter(JDialog.class::isInstance)
                    .map(JDialog.class::cast).anyMatch(d -> children(d).stream()
                    .anyMatch(c -> (c instanceof JLabel
                            && "Enter text to search for.".equals(((JLabel) c).getText()))
                            || (c instanceof JTextComponent
                            && "Enter text to search for.".equals(
                                    ((JTextComponent) c).getText()))))), "empty literal warning");
            check(edt(() -> results.getRowCount() == 1), "Empty literal leaves previous results intact");
            edt(() -> {
                for (Window w : Window.getWindows()) if (w instanceof JDialog && w.isShowing())
                    find((Container) w, JButton.class, b -> "OK".equals(b.getText())).doClick();
                extension.search("item+", ExtensionSearch.Type.Request, true, false);
                return null;
            });
            complete();
            check(edt(() -> !literal.isSelected() && results.getRowCount() == 2),
                    "Programmatic regex search resets mode and retains regex matches");
            runSearch("[item]+?", true, "Request", false);
            check("prefix [item]+? \\E 日本語 caseToken café suffix".equals(
                    first.getHttpMessage().getRequestBody().toString()),
                    "Search leaves stored synthetic traffic unchanged");
            render(1200, "search-1200.png");
            render(1000, "search-1000.png");
            render(800, "search-800.png");
            Files.write(RESULTS.resolve("desktop-checks.txt"), checks);
            System.out.println("QA SUCCESS " + checks.size() + " checks");
            edt(() -> { Control.getSingleton().exit(false, null); return null; });
        } catch (Throwable e) {
            e.printStackTrace();
            Files.writeString(RESULTS.resolve("desktop-failure.txt"),
                    e + "\n" + String.join("\n", checks));
            System.exit(2);
        }
    }
}
