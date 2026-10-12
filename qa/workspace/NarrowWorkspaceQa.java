/*
 * Copyright 2026 jaykobdetar
 * Licensed under the Apache License, Version 2.0.
 * See the repository LICENSE for the full license text.
 */
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.ZAP;
import org.zaproxy.zap.view.WorkbenchTabQaAccess;
import org.zaproxy.zap.view.table.HistoryReferencesTableModel.Column;

/** Focused follow-up: wrapping controls and scrollable columns; no requests are sent. */
public class NarrowWorkspaceQa extends WorkspaceQa {
    static JToolBar toolbar;

    static void resize(int width) throws Exception {
        edt(() -> { View.getSingleton().getMainFrame().setSize(width, 850); return null; });
        edt(() -> { View.getSingleton().getMainFrame().validate(); return null; });
    }

    static void toolbarFits(int width) throws Exception {
        edt(() -> {
            for (Component c : toolbar.getComponents()) {
                if (!c.isVisible()) continue;
                check(c.getX() >= 0 && c.getX() + c.getWidth() <= toolbar.getWidth()
                        && c.getY() >= 0 && c.getY() + c.getHeight() <= toolbar.getHeight(),
                        "Whole toolbar control fits at " + width + ": " + c.getClass().getSimpleName());
            }
            return null;
        });
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RESULTS);
        new Thread(() -> { try { ZAP.main(args); } catch (Exception e) { e.printStackTrace(); } },
                "Offline-QA-bootstrap").start();
        try {
            waitFor(() -> Control.getSingleton() != null
                    && Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.class) != null
                    && edt(() -> View.getSingleton().getMainFrame().isShowing())
                    && Thread.getAllStackTraces().keySet().stream().noneMatch(
                        t -> t.isAlive() && "ZAP-BootstrapGUI".equals(t.getName())), "desktop");
            Control.getSingleton().runCommandLineNewSession(RESULTS.resolve("synthetic-narrow.session").toString());
            var first = store("same-prefix/alpha", "first synthetic record", "Offline same-prefix/alpha");
            var second = store("same-prefix/beta", "second synthetic record", "Offline same-prefix/beta");
            var history = Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.class);
            var table = history.getHistoryReferencesTable();
            var frame = View.getSingleton().getMainFrame();
            edt(() -> {
                history.addHistory(first); history.addHistory(second);
                Model.getSingleton().getSession().getSiteTree().addPath(first);
                Model.getSingleton().getSession().getSiteTree().addPath(second);
                var tree = View.getSingleton().getSiteTreePanel().getTreeSite();
                for (int i=0; i<tree.getRowCount() && i<50; i++) tree.expandRow(i);
                toolbar = find(frame, JToolBar.class, t -> "Main Toolbar".equals(t.getName()));
                preset = find(frame, JToggleButton.class, b -> "trafficReviewLayout".equals(b.getName()));
                preset.doClick();
                View.getSingleton().getWorkbench().getTabbedStatus().setSelectedComponent(
                        SwingUtilities.getAncestorOfClass(AbstractPanel.class, table));
                table.setRowSelectionInterval(0, 0);
                return null;
            });
            expectMessage("same-prefix/alpha");
            var originals = edt(() -> List.of(toolbar.getComponents()));
            var columns = edt(() -> table.getColumns(false).stream().map(c -> c.getModelIndex()).toList());
            var toggleStates = edt(() -> originals.stream().filter(JToggleButton.class::isInstance)
                    .map(JToggleButton.class::cast).map(JToggleButton::isSelected).toList());
            for (int width : new int[]{800, 804, 820, 840, 900, 1000, 1200, 800}) {
                resize(width);
                toolbarFits(width);
                edt(() -> {
                    check(columns.equals(table.getColumns(false).stream().map(c -> c.getModelIndex()).toList()),
                            "Column visibility and order retained at " + width);
                    check(table.getColumns(false).stream().allMatch(c -> c.getWidth() >=
                            table.getTableHeader().getFontMetrics(table.getTableHeader().getFont())
                            .stringWidth(c.getHeaderValue().toString()) + 20),
                            "Enabled column headers remain readable at " + width);
                    return null;
                });
            }
            check(edt(() -> originals.equals(List.of(toolbar.getComponents()))
                    && toggleStates.equals(originals.stream().filter(JToggleButton.class::isInstance)
                    .map(JToggleButton.class::cast).map(JToggleButton::isSelected).toList())),
                    "Wrapping retains original component instances, order and toggle state");
            var scroll = edt(() -> (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table));
            edt(() -> {
                int url = table.convertColumnIndexToView(table.getModel().getColumnIndex(Column.URL));
                int code = table.convertColumnIndexToView(table.getModel().getColumnIndex(Column.STATUS_CODE));
                Rectangle visible = table.getCellRect(0, url, true);
                visible.add(table.getCellRect(0, code, true));
                table.scrollRectToVisible(visible);
                check(scroll.getHorizontalScrollBar().isShowing(), "History exposes horizontal scrollbar");
                check(scroll.getViewport().getViewRect().contains(visible),
                        "Full URL and status columns can be viewed together at 800 pixels");
                table.scrollRectToVisible(table.getCellRect(0, table.getColumnCount()-1, true));
                check(scroll.getViewport().getViewRect().contains(
                        table.getCellRect(0, table.getColumnCount()-1, true)),
                        "Remaining metadata is reachable by horizontal scrolling");
                table.scrollRectToVisible(visible);
                return null;
            });
            snapshot(800, "narrow-light-800.png");
            JButton extra = new JButton("Offline notes");
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            extra.addActionListener(e -> calls.incrementAndGet());
            edt(() -> { frame.getMainToolbarPanel().addButton(extra); return null; });
            resize(800); toolbarFits(800);
            edt(() -> { extra.doClick(); frame.getMainToolbarPanel().removeButton(extra); return null; });
            resize(800); toolbarFits(800);
            check(calls.get()==1 && edt(() -> originals.equals(List.of(toolbar.getComponents()))),
                    "Added control keeps one listener invocation; removal restores originals");
            edt(() -> {
                table.getActionMap().get("selectNextRow").actionPerformed(
                        new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "keyboard-next"));
                return null;
            });
            expectMessage("same-prefix/beta");
            edt(() -> {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
                SwingUtilities.updateComponentTreeUI(frame);
                return null;
            });
            resize(800); toolbarFits(800);
            check(edt(() -> preset.isFocusable() && "Traffic review".equals(
                    preset.getAccessibleContext().getAccessibleName())),
                    "Wrapped preset retains keyboard target and accessible name in dark theme");
            edt(() -> {
                int url = table.convertColumnIndexToView(table.getModel().getColumnIndex(Column.URL));
                int code = table.convertColumnIndexToView(table.getModel().getColumnIndex(Column.STATUS_CODE));
                Rectangle visible = table.getCellRect(1, url, true);
                visible.add(table.getCellRect(1, code, true));
                table.scrollRectToVisible(visible);
                check(scroll.getViewport().getViewRect().contains(visible),
                        "Full URL and status columns remain reachable in dark theme");
                return null;
            });
            snapshot(800, "narrow-dark-800.png");
            edt(() -> {
                WorkbenchTabQaAccess.resetColumns(table);
                var url = table.getColumns(true).stream().filter(c -> c.getModelIndex()
                        == table.getModel().getColumnIndex(Column.URL)).findFirst().orElseThrow();
                check(url.getMinWidth() >= 320, "Reset Columns to Default retains readable URL width");
                check(table.getColumns(false).stream().allMatch(c -> c.getMinWidth() >=
                        table.getTableHeader().getFontMetrics(table.getTableHeader().getFont())
                        .stringWidth(c.getHeaderValue().toString()) + 20),
                        "Reset Columns to Default retains readable header widths");
                return null;
            });
            expectMessage("same-prefix/beta");
            check("first synthetic record".equals(first.getHttpMessage().getRequestBody().toString()),
                    "Layout and column reset preserve stored synthetic records");
            Files.write(RESULTS.resolve("narrow-checks.txt"), checks);
            System.out.println("QA SUCCESS " + checks.size() + " affected checks");
            edt(() -> { Control.getSingleton().exit(false, null); return null; });
        } catch (Throwable e) {
            e.printStackTrace();
            Files.writeString(RESULTS.resolve("desktop-failure.txt"), e + "\n" + String.join("\n",checks));
            System.exit(2);
        }
    }
}
