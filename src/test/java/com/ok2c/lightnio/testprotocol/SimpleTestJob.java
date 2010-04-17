/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ok2c.lightnio.testprotocol;

import java.util.Random;

public class SimpleTestJob {

    private static final Random RND = new Random();
    private static final String TEST_CHARS = "0123456789ABCDEF";

    private final int count;
    private final String pattern;

    private volatile boolean completed;
    private volatile SimpleTestState state;
    private volatile Exception exception;

    public SimpleTestJob(int maxCount) {
        super();
        this.count = RND.nextInt(maxCount - 1) + 1;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            char rndchar = TEST_CHARS.charAt(RND.nextInt(TEST_CHARS.length() - 1));
            buffer.append(rndchar);
        }
        this.pattern = buffer.toString();
    }

    public SimpleTestJob() {
        this(1000);
    }

    public SimpleTestJob(final String pattern, int count) {
        super();
        this.count = count;
        this.pattern = pattern;
    }

    public int getCount() {
        return this.count;
    }

    public String getPattern() {
        return this.pattern;
    }

    public String getExpected() {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < this.count; i++) {
            buffer.append(this.pattern);
        }
        return buffer.toString();
    }

    public SimpleTestState getTestState() {
        return this.state;
    }

    public boolean isSuccessful() {
        return this.exception == null && this.state != null;
    }

    public Exception getException() {
        return this.exception;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public synchronized void success(final SimpleTestState state) {
        if (this.completed) {
            return;
        }
        this.completed = true;
        this.state = state;
        notifyAll();
    }

    public synchronized void failure(final SimpleTestState state, final Exception exception) {
        if (this.completed) {
            return;
        }
        this.completed = true;
        this.state = state;
        this.exception = exception;
        notifyAll();
    }

    public synchronized void waitFor() throws InterruptedException {
        while (!this.completed) {
            wait();
        }
    }

}
