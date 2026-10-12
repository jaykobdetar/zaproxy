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

import java.util.List;
import java.util.ResourceBundle;

final class EvidenceDocument {
    private EvidenceDocument() {}

    static String render(List<EvidenceSummary> summaries, boolean paths, ResourceBundle messages) {
        StringBuilder result = new StringBuilder(EvidenceSummary.text(messages, "document.title"));
        result.append("\n")
                .append(EvidenceSummary.text(messages, "document.policy"))
                .append("\n\n");
        for (int i = 0; i < summaries.size(); i++) {
            result.append(summaries.get(i).render(i + 1, paths, messages)).append('\n');
        }
        return result.toString();
    }

    static String compare(List<EvidenceSummary> summaries, boolean paths, ResourceBundle messages) {
        if (summaries.size() != 2) {
            return EvidenceSummary.text(messages, "compare.needtwo");
        }
        var left = summaries.get(0).fields(paths, messages);
        var right = summaries.get(1).fields(paths, messages);
        StringBuilder result =
                new StringBuilder(EvidenceSummary.text(messages, "compare.notice") + "\n\n");
        left.forEach(
                (field, value) -> {
                    String other = right.get(field);
                    result.append(field).append(": ");
                    if (value.equals(other)) {
                        result.append(EvidenceSummary.text(messages, "document.same"));
                        result.append(" (").append(value).append(")\n");
                    } else {
                        result.append(EvidenceSummary.text(messages, "document.changed"))
                                .append('\n');
                        result.append("  ").append(EvidenceSummary.text(messages, "document.left"));
                        result.append(": ").append(value).append('\n');
                        result.append("  ")
                                .append(EvidenceSummary.text(messages, "document.right"));
                        result.append(": ").append(other).append('\n');
                    }
                });
        return result.toString();
    }
}
