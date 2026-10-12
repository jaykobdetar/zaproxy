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

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;
import org.parosproxy.paros.network.HttpMessage;

/** Prevents lazy selection retrieval from publishing a batch across a session transition. */
final class CaptureGuard {
    private long revision;
    private boolean changing;

    synchronized OptionalLong begin() {
        return changing ? OptionalLong.empty() : OptionalLong.of(revision);
    }

    synchronized boolean isChanging() {
        return changing;
    }

    synchronized void beforeSessionChange() {
        changing = true;
        revision++;
    }

    synchronized void afterSessionChange() {
        revision++;
        changing = false;
    }

    private synchronized void requireCurrent(long token) {
        if (changing || token != revision) {
            throw new CancellationException("Session changed during evidence preparation.");
        }
    }

    List<EvidenceSummary> capture(List<HttpMessage> messages, long token) {
        List<EvidenceSummary> result = new ArrayList<>(messages.size());
        for (int index = 0; index < messages.size(); index++) {
            requireCurrent(token);
            HttpMessage message = messages.get(index);
            requireCurrent(token);
            if (message == null) {
                throw new IllegalStateException(
                        "A selected stored message is no longer available.");
            }
            result.add(EvidenceSummary.capture(message));
        }
        requireCurrent(token);
        return List.copyOf(result);
    }

    /** Publication and session invalidation have a shared short critical section. */
    synchronized boolean publishIfCurrent(long token, Runnable publish) {
        if (changing || token != revision) {
            return false;
        }
        publish.run();
        return true;
    }
}
