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

class LeaseRequest<T> {

    private final T route;
    private final Object state;
    private final PoolEntryFuture<T> future;

    public LeaseRequest(
            final T route,
            final Object state,
            final PoolEntryFuture<T> future) {
        super();
        this.route = route;
        this.state = state;
        this.future = future;
    }

    public T getRoute() {
        return this.route;
    }

    public Object getState() {
        return this.state;
    }

    public PoolEntryFuture<T> getFuture() {
        return this.future;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        buffer.append(this.route);
        buffer.append("][");
        buffer.append(this.state);
        buffer.append("]");
        return super.toString();
    }

}
