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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.network.HttpMessage;

class EvidenceSummaryTest {
    private static final ResourceBundle MESSAGES =
            ResourceBundle.getBundle("org.zaproxy.addon.privateevidence.resources.Messages");

    static HttpMessage message(String target, int status, String body) throws Exception {
        HttpMessage message = new HttpMessage();
        message.setRequestHeader(
                "POST "
                        + target
                        + " HTTP/1.1\r\n"
                        + "Authorization: Bearer authorization-secret\r\n"
                        + "Cookie: cookie-secret\r\n"
                        + "X-name-secret: custom-secret\r\n"
                        + "Content-Type: multipart/form-data; boundary=boundary-secret\r\n\r\n");
        message.setRequestBody(body);
        message.setResponseHeader(
                "HTTP/1.1 "
                        + status
                        + " reason-secret\r\n"
                        + "Set-Cookie: session=cookie-secret\r\n"
                        + "Location: https://redirect-secret.invalid/?key=query-secret\r\n"
                        + "Content-Type: application/json; charset=charset-secret\r\n\r\n");
        message.setResponseBody(body);
        return message;
    }

    @Test
    void defaultsExcludeEveryRawTrafficSecretAndLeaveSourceUnchanged() throws Exception {
        var source =
                message(
                        "https://user-secret:password-secret@host-secret.invalid/path-secret?key=query-secret#fragment-secret",
                        200,
                        "body-secret");
        String requestBefore = source.getRequestHeader().toString();
        String responseBefore = source.getResponseHeader().toString();
        String output =
                EvidenceDocument.render(List.of(EvidenceSummary.capture(source)), false, MESSAGES);
        for (String secret :
                List.of(
                        "user-secret",
                        "password-secret",
                        "host-secret",
                        "path-secret",
                        "query-secret",
                        "fragment-secret",
                        "body-secret",
                        "authorization-secret",
                        "cookie-secret",
                        "custom-secret",
                        "name-secret",
                        "reason-secret",
                        "redirect-secret",
                        "boundary-secret",
                        "charset-secret")) {
            assertFalse(output.contains(secret), secret);
        }
        assertTrue(output.contains("multipart/form-data"));
        assertTrue(output.contains("application/json"));
        assertTrue(output.contains("Response status: 200"));
        assertEquals(requestBefore, source.getRequestHeader().toString());
        assertEquals(responseBefore, source.getResponseHeader().toString());
        assertEquals("body-secret", source.getRequestBody().toString());
        assertEquals("body-secret", source.getResponseBody().toString());
    }

    @Test
    void pathOptInNeverIncludesAuthorityQueryOrFragment() throws Exception {
        var source =
                message(
                        "https://user-secret:password-secret@host-secret.invalid/path-secret?key=query-secret#fragment-secret",
                        200,
                        "body-secret");
        String output = EvidenceSummary.capture(source).render(1, true, MESSAGES);
        assertTrue(output.contains("URL path: /path-secret"));
        for (String secret :
                List.of(
                        "user-secret",
                        "password-secret",
                        "host-secret",
                        "query-secret",
                        "fragment-secret")) {
            assertFalse(output.contains(secret), secret);
        }
    }

    @Test
    void unrecognisedMediaTypeAndOversizedPathAreOmittedCompletely() throws Exception {
        var source = message("https://example.invalid/" + "secret".repeat(200), 200, "");
        source.getRequestHeader().setHeader("Content-Type", "application/customer-secret");
        source.getResponseHeader().setHeader("Content-Type", "text/plain; token=secret");
        var summary = EvidenceSummary.capture(source);
        assertNull(summary.path());
        assertNull(summary.requestMediaType());
        assertEquals("text/plain", summary.responseMediaType());
        assertFalse(summary.render(1, true, MESSAGES).contains("secret"));
    }

    @Test
    void absentResponseIsNotPresentedAsAStatusCode() throws Exception {
        HttpMessage source = new HttpMessage();
        source.setRequestHeader("GET / HTTP/1.1\r\nHost: example.invalid\r\n\r\n");
        var summary = EvidenceSummary.capture(source);
        assertNull(summary.status());
        assertTrue(summary.render(1, false, MESSAGES).contains("[no response]"));
    }

    @Test
    void snapshotsDoNotChangeWhenOriginalMessagesAreEditedLater() throws Exception {
        var source = message("https://example.invalid/", 200, "first");
        var snapshot = EvidenceSummary.capture(source);
        source.setRequestBody("longer second body");
        source.getResponseHeader().setStatusCode(404);
        assertEquals(5, snapshot.requestBytes());
        assertEquals(200, snapshot.status());
    }

    @Test
    void comparisonRevealsSafeMetadataDifferencesAndKeepsHiddenFieldsHidden() throws Exception {
        var left =
                EvidenceSummary.capture(
                        message("https://one.invalid/secret-left?key=one", 200, "a"));
        var right =
                EvidenceSummary.capture(
                        message("https://two.invalid/secret-right?key=two", 404, "bb"));
        String comparison = EvidenceDocument.compare(List.of(left, right), false, MESSAGES);
        assertTrue(comparison.contains("Response status: Changed"));
        assertTrue(comparison.contains("First: 200"));
        assertTrue(comparison.contains("Second: 404"));
        assertTrue(comparison.contains("Method: Equal"));
        assertFalse(comparison.contains("secret"));
        assertTrue(
                EvidenceDocument.compare(List.of(left, right), true, MESSAGES)
                        .contains("/secret-left"));
        assertEquals(
                MESSAGES.getString("privateevidence.compare.needtwo"),
                EvidenceDocument.compare(List.of(left), false, MESSAGES));
    }
}
