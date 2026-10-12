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

import java.util.OptionalInt;

final class LiteralSearch {
    private LiteralSearch() {}

    /** Literal, case-sensitive search, wrapping once after the end of the document. */
    static OptionalInt findNext(String text, String query, int start) {
        if (query.isEmpty()) {
            return OptionalInt.empty();
        }
        int position = Math.max(0, Math.min(start, text.length()));
        int match = text.indexOf(query, position);
        if (match < 0 && position > 0) {
            match = text.indexOf(query);
        }
        return match < 0 ? OptionalInt.empty() : OptionalInt.of(match);
    }
}
