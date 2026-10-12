/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2026 The ZAP Development Team
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
package org.zaproxy.zap.extension.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ViewDelegate;
import org.zaproxy.zap.WithConfigsTest;
import org.zaproxy.zap.utils.I18N;

class SearchPanelUnitTest extends WithConfigsTest {
    private SearchPanel panel;
    private ExtensionSearch extension;
    private ViewDelegate view;

    @BeforeEach
    void setUp() throws Exception {
        Constant.messages = new I18N(Locale.ENGLISH);
        extension = mock(ExtensionSearch.class);
        view = mock(ViewDelegate.class);
        SwingUtilities.invokeAndWait(
                () -> {
                    panel = new SearchPanel(view);
                    panel.setExtension(extension);
                });
    }

    private String submittedExpression(String query, boolean literal) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    panel.setLiteralSearch(literal);
                    panel.getRegExField().setText(query);
                    panel.getRegExField().postActionEvent();
                });
        var captor = ArgumentCaptor.forClass(String.class);
        verify(extension)
                .search(
                        captor.capture(),
                        eq(ExtensionSearch.Type.All),
                        isNull(),
                        eq(false),
                        eq(false));
        return captor.getValue();
    }

    @Test
    void shouldSearchMetacharactersAndEmbeddedQuoteTerminatorsAsLiteralText() throws Exception {
        String query = "[a]+?\\E 日本語";
        Pattern expression = Pattern.compile(submittedExpression(query, true));
        var matcher = expression.matcher("prefix " + query + " suffix aaa 日本語");
        assertThat(matcher.find(), is(true));
        assertThat(matcher.group(), is(equalTo(query)));
        assertThat(matcher.find(), is(false));
    }

    @Test
    void shouldPreserveRegularExpressionBehaviorWhenLiteralModeIsOff() throws Exception {
        Pattern expression = Pattern.compile(submittedExpression("ab+", false));
        var matcher = expression.matcher("abbb");
        assertThat(matcher.find(), is(true));
        assertThat(matcher.group(), is(equalTo("abbb")));
    }

    @Test
    void shouldRejectEmptyLiteralSearchWithoutStartingOrClearingSearch() throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    panel.setLiteralSearch(true);
                    panel.getRegExField().setText("");
                    panel.getRegExField().postActionEvent();
                });
        verify(view)
                .showWarningDialog(
                        Constant.messages.getString("search.toolbar.error.empty.literal"));
        verify(extension, never())
                .search(
                        anyString(),
                        eq(ExtensionSearch.Type.All),
                        isNull(),
                        anyBoolean(),
                        anyBoolean());
    }

    @Test
    void shouldKeepWhitespaceSignificantInLiteralMode() throws Exception {
        Pattern expression = Pattern.compile(submittedExpression(" ", true));
        var matcher = expression.matcher("a b");
        assertThat(matcher.find(), is(true));
        assertThat(matcher.group(), is(equalTo(" ")));
        assertThat(matcher.start(), is(1));
    }

    @Test
    void shouldRestoreRegexPromptWhenProgrammaticSearchResetsTheMode() throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    panel.setLiteralSearch(true);
                    assertThat(
                            panel.getRegExField().getToolTipText(),
                            is(Constant.messages.getString("search.toolbar.tooltip.literal")));
                    panel.setLiteralSearch(false);
                    assertThat(
                            panel.getRegExField().getToolTipText(),
                            is(Constant.messages.getString("search.toolbar.tooltip.regex")));
                    assertThat(
                            panel.getRegExField().getAccessibleContext().getAccessibleName(),
                            is(Constant.messages.getString("search.toolbar.accessible.query")));
                });
    }
}
