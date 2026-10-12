/*
 * Copyright 2026 jaykobdetar
 * Licensed under the Apache License, Version 2.0.
 * See the repository LICENSE for the full license text.
 */
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.extension.option.OptionsViewPanel;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.parosproxy.paros.view.WorkbenchPanel;
import org.zaproxy.zap.ZAP;
import org.zaproxy.zap.view.WorkbenchTabQaAccess;

/** Offline desktop coverage of real panel composition and selection contracts. */
public class WorkspaceQa extends OfflineSearchQa {
    static JToggleButton preset;

    static Point point(Component component) {
        return SwingUtilities.convertPoint(component, 0, 0, View.getSingleton().getMainFrame());
    }

    static void snapshot(int width, String filename) throws Exception {
        edt(() -> {
            var frame = View.getSingleton().getMainFrame();
            frame.setSize(width, 850);
            frame.validate();
            var content = frame.getRootPane();
            BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            content.printAll(graphics);
            graphics.dispose();
            ImageIO.write(image, "png", RESULTS.resolve(filename).toFile());
            Files.writeString(RESULTS.resolve(filename + ".bounds.txt"),
                    "preset=" + preset.getBounds() + " toolbar=" + preset.getParent().getBounds()
                    + " request=" + View.getSingleton().getRequestPanel().getBounds()
                    + " response=" + View.getSingleton().getResponsePanel().getBounds());
            return null;
        });
    }

    static void expectMessage(String suffix) throws Exception {
        waitFor(() -> edt(() -> View.getSingleton().getRequestPanel().getMessage()
                instanceof HttpMessage message && message.getRequestHeader().getURI().toString()
                .endsWith(suffix)), "selected synthetic message " + suffix);
        check(edt(() -> ((HttpMessage) View.getSingleton().getResponsePanel().getMessage())
                .getResponseBody().toString().contains(suffix)), "Both viewers track " + suffix);
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RESULTS);
        new Thread(() -> {
            try { ZAP.main(args); } catch (Exception e) { e.printStackTrace(); }
        }, "Offline-QA-bootstrap").start();
        try {
            waitFor(() -> Control.getSingleton() != null
                    && Control.getSingleton().getExtensionLoader()
                    .getExtension(ExtensionHistory.class) != null
                    && edt(() -> View.getSingleton().getMainFrame().isShowing())
                    && Thread.getAllStackTraces().keySet().stream().noneMatch(
                            t -> t.isAlive() && "ZAP-BootstrapGUI".equals(t.getName())), "fork desktop");
            if (Boolean.getBoolean("qa.restore")) {
                check(edt(() -> View.getSingleton().getMainFrame().getWorkbenchLayout()
                        == WorkbenchPanel.Layout.TRAFFIC_REVIEW), "Review layout restores on profile restart");
                check(edt(() -> View.getSingleton().getMainFrame().getResponsePanelPosition()
                        == WorkbenchPanel.ResponsePanelPosition.PANELS_SIDE_BY_SIDE),
                        "Simultaneous viewer arrangement restores on profile restart");
                check(Model.getSingleton().getOptionsParam().getViewParam().getShowTabNames(),
                        "Visible tab names restore on profile restart");
                Files.write(RESULTS.resolve("restore-checks.txt"), checks);
                edt(() -> { Control.getSingleton().exit(false, null); return null; });
                return;
            }
            Control.getSingleton().runCommandLineNewSession(
                    RESULTS.resolve("synthetic-workspace.session").toString());
            var history = Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.class);
            var first = store("projects", "workspace sample", "Offline projects");
            var second = store("notes", "another sample", "Offline notes");
            var third = store("reports", "third sample", "Offline reports");
            var wb = View.getSingleton().getWorkbench();
            var frame = View.getSingleton().getMainFrame();
            var table = history.getHistoryReferencesTable();
            edt(() -> {
                preset = find(frame, JToggleButton.class, b -> "trafficReviewLayout".equals(b.getName()));
                for (var ref : new org.parosproxy.paros.model.HistoryReference[]{first, second, third}) {
                    history.addHistory(ref);
                    Model.getSingleton().getSession().getSiteTree().addPath(ref);
                }
                var tree = View.getSingleton().getSiteTreePanel().getTreeSite();
                for (int i = 0; i < tree.getRowCount() && i < 50; i++) tree.expandRow(i);
                table.setRowSelectionInterval(0, 0);
                wb.getTabbedStatus().setSelectedComponent(SwingUtilities.getAncestorOfClass(AbstractPanel.class, table));
                return null;
            });
            expectMessage("projects");
            snapshot(1200, "before-classic-1200.png");
            edt(() -> { preset.doClick(); return null; });
            check(edt(() -> frame.getWorkbenchLayout() == WorkbenchPanel.Layout.TRAFFIC_REVIEW
                    && frame.getResponsePanelPosition() == WorkbenchPanel.ResponsePanelPosition.PANELS_SIDE_BY_SIDE),
                    "Labeled preset applies traffic review and simultaneous message viewers");
            check(edt(() -> preset.isFocusable() && "Traffic review".equals(
                    preset.getAccessibleContext().getAccessibleName())), "Preset has a keyboard focus target and accessible name");
            check(Model.getSingleton().getOptionsParam().getViewParam().getDisplayOption() == 3,
                    "Layout persists with new ID; classic IDs stay unchanged");
            edt(() -> {
                var options = new OptionsViewPanel();
                options.initParam(Model.getSingleton().getOptionsParam());
                var combo = find(options, JComboBox.class, c -> c.getItemCount() == 4
                        && c.getItemAt(3).toString().startsWith("Traffic review"));
                check(combo.getSelectedIndex() == 3, "Display options accept and select the persisted review layout");
                return null;
            });
            for (int width : new int[]{1200, 1000, 800}) {
                snapshot(width, "after-review-" + width + ".png");
                edt(() -> {
                    var request = View.getSingleton().getRequestPanel();
                    var response = View.getSingleton().getResponsePanel();
                    var sites = View.getSingleton().getSiteTreePanel();
                    check(point(table).y < point(request).y, "Traffic is above messages at " + width);
                    check(point(sites).x < point(request).x, "Sites are left of messages at " + width);
                    check(request.isShowing() && response.isShowing()
                            && point(request).x < point(response).x,
                            "Request and response are simultaneously visible at " + width);
                    check(point(preset).x + preset.getWidth() <= frame.getWidth(),
                            "Preset fits its toolbar at " + width);
                    return null;
                });
            }
            edt(() -> {
                table.requestFocusInWindow();
                table.getActionMap().get("selectNextRow").actionPerformed(
                        new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "keyboard-next"));
                return null;
            });
            expectMessage("notes");
            edt(() -> {
                var tree = View.getSingleton().getSiteTreePanel().getTreeSite();
                var selected = Model.getSingleton().getSession().getSiteTree().addPath(third);
                tree.setSelectionPath(new TreePath(selected.getPath()));
                return null;
            });
            expectMessage("reports");
            edt(() -> {
                var traffic = find(wb, JSplitPane.class, p -> "reviewTrafficAndMessages".equals(p.getName()));
                var navigator = find(wb, JSplitPane.class, p -> "reviewNavigatorAndTraffic".equals(p.getName()));
                traffic.setDividerLocation(230);
                navigator.setDividerLocation(180);
                for (var layout : WorkbenchPanel.Layout.values()) {
                    frame.setWorkbenchLayout(layout);
                    check(frame.getWorkbenchLayout() == layout, "Can switch to " + layout);
                }
                check(find(wb, JSplitPane.class, p -> "reviewTrafficAndMessages".equals(p.getName()))
                        .getDividerLocation() == 230, "Review traffic divider restores independently");
                check(find(wb, JSplitPane.class, p -> "reviewNavigatorAndTraffic".equals(p.getName()))
                        .getDividerLocation() == 180, "Review navigator divider restores independently");
                return null;
            });
            edt(() -> {
                var notes = new AbstractPanel();
                notes.setName("Offline notes");
                notes.setHideable(true);
                notes.add(new JTextArea("Synthetic review notes"));
                wb.addPanel(notes, WorkbenchPanel.PanelType.STATUS);
                wb.getTabbedStatus().setVisible(notes, true);
                WorkbenchTabQaAccess.pin(wb.getTabbedStatus(), notes, true);
                WorkbenchTabQaAccess.detach(wb.getTabbedStatus(), notes);
                check(wb.getTabbedStatus().getDetachedTabs().containsKey(notes), "Extension tab detaches in review layout");
                frame.setWorkbenchLayout(WorkbenchPanel.Layout.FULL);
                check(wb.getTabbedFull().getDetachedTabs().containsKey(notes), "Detached extension survives full layout");
                frame.setWorkbenchLayout(WorkbenchPanel.Layout.TRAFFIC_REVIEW);
                check(wb.getTabbedStatus().getDetachedTabs().containsKey(notes), "Detached extension returns to review ownership");
                wb.getTabbedStatus().closeAllDetachedTabs();
                check(wb.getTabbedStatus().indexOfComponent(notes) >= 0 && notes.isPinned(),
                        "Extension tab reattaches and retains pin state");
                WorkbenchTabQaAccess.pin(wb.getTabbedStatus(), notes, false);
                wb.getTabbedStatus().setVisible(notes, false);
                frame.setWorkbenchLayout(WorkbenchPanel.Layout.FULL);
                frame.setWorkbenchLayout(WorkbenchPanel.Layout.TRAFFIC_REVIEW);
                check(wb.getTabbedStatus().indexOfComponent(notes) == -1, "Explicitly hidden extension stays hidden across layouts");
                wb.removePanel(notes, WorkbenchPanel.PanelType.STATUS);
                return null;
            });
            edt(() -> {
                frame.setResponsePanelPosition(WorkbenchPanel.ResponsePanelPosition.TABS_SIDE_BY_SIDE);
                WorkbenchTabQaAccess.detach(wb.getTabbedWork(), View.getSingleton().getRequestPanel());
                return null;
            });
            SwingUtilities.invokeLater(() -> preset.doClick());
            waitFor(() -> edt(() -> java.util.Arrays.stream(Window.getWindows()).anyMatch(
                    w -> w instanceof JDialog && w.isShowing())), "detached viewer confirmation");
            edt(() -> {
                var dialog = java.util.Arrays.stream(Window.getWindows())
                        .filter(w -> w instanceof JDialog && w.isShowing()).findFirst().orElseThrow();
                find((JDialog) dialog, JButton.class, b -> "Cancel".equals(b.getText())).doClick();
                return null;
            });
            edt(() -> {
                check(wb.getTabbedWork().getDetachedTabs().containsKey(View.getSingleton().getRequestPanel())
                        && wb.getTabbedWork().getDetachedTabs().size() == 1,
                        "Canceling viewer rearrangement preserves the detached request exactly once");
                check(frame.getResponsePanelPosition() == WorkbenchPanel.ResponsePanelPosition.TABS_SIDE_BY_SIDE,
                        "Canceling viewer rearrangement retains its previous position");
                wb.getTabbedWork().closeAllDetachedTabs();
                preset.doClick();
                return null;
            });
            edt(() -> {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
                SwingUtilities.updateComponentTreeUI(frame);
                check(preset.isFocusable(), "Preset retains keyboard focus after dark theme update");
                return null;
            });
            snapshot(1200, "after-review-dark-1200.png");
            edt(() -> {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
                SwingUtilities.updateComponentTreeUI(frame);
                check(preset.isFocusable(), "Preset retains keyboard focus after light theme update");
                frame.setWorkbenchLayout(WorkbenchPanel.Layout.EXPAND_STATUS);
                check(wb.getTabbedStatus().getClientProperty("JTabbedPane.tabHeight") == null,
                        "Review tab spacing is removed when returning to classic layout");
                preset.doClick();
                return null;
            });
            expectMessage("reports");
            check("workspace sample".equals(first.getHttpMessage().getRequestBody().toString()),
                    "UI changes leave stored synthetic messages unchanged");
            edt(() -> { Model.getSingleton().getOptionsParam().getViewParam().getConfig().save(); return null; });
            snapshot(1200, "after-final-1200.png");
            Files.write(RESULTS.resolve("desktop-checks.txt"), checks);
            System.out.println("QA SUCCESS " + checks.size() + " checks");
            edt(() -> { Control.getSingleton().exit(false, null); return null; });
        } catch (Throwable e) {
            e.printStackTrace();
            Files.writeString(RESULTS.resolve("desktop-failure.txt"), e + "\n" + String.join("\n", checks));
            System.exit(2);
        }
    }
}
