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

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.parosproxy.paros.network.HttpMessage;

/** A deliberately lossy snapshot. It retains no bodies, query strings, or raw headers. */
record EvidenceSummary(
        String method,
        String path,
        Integer status,
        boolean hasResponse,
        int requestBytes,
        int responseBytes,
        String requestMediaType,
        String responseMediaType) {

    private static final Set<String> METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH");
    private static final Set<String> MEDIA_TYPES =
            Set.of(
                    "application/json",
                    "application/xml",
                    "application/octet-stream",
                    "application/x-www-form-urlencoded",
                    "application/pdf",
                    "multipart/form-data",
                    "text/plain",
                    "text/html",
                    "text/css",
                    "text/xml",
                    "text/javascript",
                    "application/javascript",
                    "image/png",
                    "image/jpeg",
                    "image/gif",
                    "image/webp",
                    "image/svg+xml");

    static EvidenceSummary capture(HttpMessage message) {
        var request = message.getRequestHeader();
        var response = message.getResponseHeader();
        int code = response.getStatusCode();
        return new EvidenceSummary(
                METHODS.contains(request.getMethod()) ? request.getMethod() : null,
                request.getURI() == null || "CONNECT".equalsIgnoreCase(request.getMethod())
                        ? null
                        : pathOnly(request.getURI().getEscapedPath()),
                !response.isEmpty() && code >= 100 && code <= 599 ? code : null,
                !response.isEmpty(),
                message.getRequestBody().length(),
                message.getResponseBody().length(),
                mediaType(request.getHeader("Content-Type")),
                mediaType(response.getHeader("Content-Type")));
    }

    private static String mediaType(String value) {
        if (value == null || value.length() > 4096) {
            return null;
        }
        String type = value.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        return MEDIA_TYPES.contains(type) ? type : null;
    }

    private static String pathOnly(String value) {
        // Read the upstream URI's path component, never reinterpret an authority as a path.
        if (value == null || value.length() > 1024) {
            return null;
        }
        return value.isEmpty() ? "/" : value.startsWith("/") ? value : null;
    }

    Map<String, String> fields(boolean includePaths, ResourceBundle messages) {
        String omitted = text(messages, "document.omitted");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(text(messages, "document.method"), method == null ? omitted : method);
        fields.put(text(messages, "document.path"), includePaths && path != null ? path : omitted);
        fields.put(
                text(messages, "document.status"),
                status == null
                        ? hasResponse ? omitted : text(messages, "document.noresponse")
                        : status.toString());
        fields.put(text(messages, "document.requestbytes"), Integer.toString(requestBytes));
        fields.put(text(messages, "document.responsebytes"), Integer.toString(responseBytes));
        fields.put(
                text(messages, "document.requesttype"),
                requestMediaType == null ? omitted : requestMediaType);
        fields.put(
                text(messages, "document.responsetype"),
                responseMediaType == null ? omitted : responseMediaType);
        return fields;
    }

    String render(int index, boolean includePaths, ResourceBundle messages) {
        StringBuilder result =
                new StringBuilder(
                        MessageFormat.format(text(messages, "document.entry"), index) + "\n");
        fields(includePaths, messages)
                .forEach(
                        (name, value) ->
                                result.append(name).append(": ").append(value).append('\n'));
        return result.toString();
    }

    static String text(ResourceBundle messages, String suffix) {
        return messages.getString("privateevidence." + suffix);
    }
}
