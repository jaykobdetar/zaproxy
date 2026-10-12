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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LiteralSearchTest {
    @Test
    void metacharactersAreLiteralAndSearchWraps() {
        String text = "[omitted] POST [omitted]";
        assertEquals(0, LiteralSearch.findNext(text, "[omitted]", 0).orElseThrow());
        assertEquals(15, LiteralSearch.findNext(text, "[omitted]", 9).orElseThrow());
        assertEquals(0, LiteralSearch.findNext(text, "[omitted]", text.length()).orElseThrow());
        assertTrue(LiteralSearch.findNext(text, ".*", 0).isEmpty());
    }

    @Test
    void handlesUnicodeEmptyInputAndOutOfBoundsPositions() {
        assertEquals(0, LiteralSearch.findNext("日本語", "日本", -10).orElseThrow());
        assertEquals(0, LiteralSearch.findNext("日本語", "日本", 500).orElseThrow());
        assertTrue(LiteralSearch.findNext("", "a", 0).isEmpty());
        assertTrue(LiteralSearch.findNext("abc", "", 0).isEmpty());
        assertTrue(LiteralSearch.findNext("POST", "post", 0).isEmpty());
    }
}
