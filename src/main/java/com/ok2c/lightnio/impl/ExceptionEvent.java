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
package com.ok2c.lightnio.impl;

import java.util.Date;

/**
 * A {@link Throwable} instance along with a time stamp.
 */
public class ExceptionEvent {

    private final Throwable ex;
    private final Date timestamp;

    public ExceptionEvent(final Throwable ex, final Date timestamp) {
        super();
        this.ex = ex;
        this.timestamp = timestamp;
    }

    public ExceptionEvent(final Exception ex) {
        this(ex, new Date());
    }

    public Throwable getCause() {
        return ex;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.timestamp);
        buffer.append(" ");
        buffer.append(this.ex);
        return buffer.toString();
    }

}
