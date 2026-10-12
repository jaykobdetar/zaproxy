/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2017 The ZAP Development Team
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
package org.parosproxy.paros.extension.history;

import org.parosproxy.paros.Constant;
import org.zaproxy.zap.utils.DisplayUtils;
import org.zaproxy.zap.view.table.HistoryReferencesTable;
import org.zaproxy.zap.view.table.HistoryReferencesTableModel.Column;

/** A {@code HistoryReferencesTable} for History tab. */
class HistoryTable extends HistoryReferencesTable {

    private static final long serialVersionUID = 1L;

    /** Constructs a {@code HistoryTable}. */
    public HistoryTable() {
        super(new HistoryTableModel());

        setAutoCreateColumnsFromModel(false);

        setName("History Table");
        // Retain every enabled column at a readable width rather than squeezing all metadata
        // into the viewport. The existing horizontal scrollbar exposes the remaining fields.
        setAutoResizeMode(AUTO_RESIZE_OFF);
        super.loadColumnConfiguration("HistoryTable");
        if (this.config == null) {
            this.applyDefaultColumnConfigurations();
        } else {
            applyReadableColumnWidths();
        }
    }

    private void applyReadableColumnWidths() {
        for (var column : getColumns(true)) {
            int minimum =
                    Math.max(
                            DisplayUtils.getScaledSize(40),
                            getTableHeader()
                                            .getFontMetrics(getTableHeader().getFont())
                                            .stringWidth(column.getHeaderValue().toString())
                                    + DisplayUtils.getScaledSize(20));
            if (getModel().getColumnIndex(Column.URL) == column.getModelIndex()) {
                minimum = Math.max(minimum, DisplayUtils.getScaledSize(320));
            }
            column.setMinWidth(Math.max(column.getMinWidth(), minimum));
            column.setPreferredWidth(Math.max(column.getPreferredWidth(), column.getMinWidth()));
        }
    }

    @Override
    protected void applyDefaultColumnConfigurations() {
        getColumnExt(Constant.messages.getString("view.href.table.header.timestamp.response"))
                .setVisible(false);
        getColumnExt(Constant.messages.getString("view.href.table.header.size.requestheader"))
                .setVisible(false);
        getColumnExt(Constant.messages.getString("view.href.table.header.size.requestbody"))
                .setVisible(false);
        getColumnExt(Constant.messages.getString("view.href.table.header.size.responseheader"))
                .setVisible(false);
        getColumnExt(Constant.messages.getString("view.href.table.header.hostname"))
                .setVisible(false);
        getColumnExt(Constant.messages.getString("view.href.table.header.pathandquery"))
                .setVisible(false);
        applyReadableColumnWidths();
    }

    /**
     * Sets whether or not the selected message should be displayed in Request/Response tabs.
     *
     * @param display {@code true} if the selected message should be displayed, {@code false}
     *     otherwise.
     */
    void setDisplaySelectedMessage(boolean display) {
        getDefaultSelectionListener().setEnabled(display);
    }
}
