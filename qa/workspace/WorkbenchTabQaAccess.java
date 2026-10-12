/*
 * Copyright 2026 jaykobdetar
 * Licensed under the Apache License, Version 2.0.
 * See the repository LICENSE for the full license text.
 */
package org.zaproxy.zap.view;

import org.parosproxy.paros.extension.AbstractPanel;

/** QA-only access to the existing detach operation; excluded from production builds. */
public final class WorkbenchTabQaAccess {
    private WorkbenchTabQaAccess() {}
    public static void pin(TabbedPanel2 pane, AbstractPanel panel, boolean pinned) {
        ((TabbedPanelTab) pane.getTabComponentAt(pane.indexOfComponent(panel))).setPinned(pinned);
        pane.saveTabState(panel);
    }
    public static void detach(TabbedPanel2 pane, AbstractPanel panel) {
        pane.detachTab(panel);
    }
}
