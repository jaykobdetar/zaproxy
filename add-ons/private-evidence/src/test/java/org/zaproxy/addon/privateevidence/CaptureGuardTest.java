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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.network.HttpMessage;

class CaptureGuardTest {
    @Test
    void sessionChangeDuringLazyRetrievalDiscardsTheWholeBatch() throws Exception {
        var guard = new CaptureGuard();
        long token = guard.begin().orElseThrow();
        var entered = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        var message = EvidenceSummaryTest.message("https://example.invalid/", 200, "");
        var lazy =
                new AbstractList<HttpMessage>() {
                    @Override
                    public int size() {
                        return 1;
                    }

                    @Override
                    public HttpMessage get(int index) {
                        entered.countDown();
                        try {
                            assertTrue(resume.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                        return message;
                    }
                };
        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> guard.capture(lazy, token));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            guard.beforeSessionChange();
            assertTrue(guard.begin().isEmpty());
            resume.countDown();
            ExecutionException failure =
                    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof CancellationException);
            var published = new AtomicBoolean();
            assertFalse(guard.publishIfCurrent(token, () -> published.set(true)));
            assertFalse(published.get());
        } finally {
            resume.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void newSessionAllowsAFreshBatchWithoutRevivingTheOldTicket() throws Exception {
        var guard = new CaptureGuard();
        long old = guard.begin().orElseThrow();
        guard.beforeSessionChange();
        guard.afterSessionChange();
        long fresh = guard.begin().orElseThrow();
        assertFalse(guard.publishIfCurrent(old, () -> {}));
        var message = EvidenceSummaryTest.message("https://example.invalid/", 404, "");
        var summaries = guard.capture(List.of(message), fresh);
        assertEquals(404, summaries.get(0).status());
        var published = new AtomicBoolean();
        assertTrue(guard.publishIfCurrent(fresh, () -> published.set(true)));
        assertTrue(published.get());
    }

    @Test
    void transitionAfterCaptureBeforePublicationStillDiscardsTheBatch() throws Exception {
        var guard = new CaptureGuard();
        long token = guard.begin().orElseThrow();
        var message = EvidenceSummaryTest.message("https://example.invalid/", 200, "");
        assertEquals(1, guard.capture(List.of(message), token).size());
        guard.beforeSessionChange();
        guard.afterSessionChange();
        assertFalse(guard.publishIfCurrent(token, () -> {}));
    }

    @Test
    void missingStoredMessageFailsRatherThanReturningAPartialBatch() throws Exception {
        var guard = new CaptureGuard();
        long token = guard.begin().orElseThrow();
        var message = EvidenceSummaryTest.message("https://example.invalid/", 200, "");
        assertThrows(
                IllegalStateException.class,
                () -> guard.capture(Arrays.asList(message, null), token));
    }
}
