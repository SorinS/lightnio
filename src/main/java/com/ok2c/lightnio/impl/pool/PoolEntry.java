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
package com.ok2c.lightnio.impl.pool;

import com.ok2c.lightnio.IOSession;

public class PoolEntry<T> {

    public static final String ATTRIB = "com.ok2c.lightnio.pool-entry";

    private final T route;
    private final IOSession session;
    private Object state;

    public PoolEntry(final T route, final IOSession session) {
        super();
        this.route = route;
        this.session = session;
    }

    public T getRoute() {
        return this.route;
    }

    public IOSession getIOSession() {
        return this.session;
    }
    public Object getState() {
        return this.state;
    }

    public void setState(final Object state) {
        this.state = state;
    }

}
