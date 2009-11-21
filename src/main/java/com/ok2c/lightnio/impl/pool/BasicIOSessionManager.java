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

import java.net.SocketAddress;
import java.util.concurrent.Future;

import com.ok2c.lightnio.ConnectingIOReactor;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.concurrent.BasicFuture;
import com.ok2c.lightnio.concurrent.FutureCallback;
import com.ok2c.lightnio.pool.IOSessionManager;
import com.ok2c.lightnio.pool.ManagedIOSession;

public class BasicIOSessionManager implements IOSessionManager<SocketAddress> {

    private final SessionPool<SocketAddress> pool;

    public BasicIOSessionManager(final ConnectingIOReactor ioreactor) {
        super();
        this.pool = new SessionPool<SocketAddress>(
                ioreactor, new InternalRouteResolver(), 20, 50);
    }

    public synchronized Future<ManagedIOSession> leaseSession(
            final SocketAddress route,
            final Object state,
            final FutureCallback<ManagedIOSession> callback) {
        BasicFuture<ManagedIOSession> future = new BasicFuture<ManagedIOSession>(callback);
        this.pool.lease(route, state, new InternalPoolEntryCallback(this.pool, future));
        return future;
    }

    public synchronized void releaseSession(final ManagedIOSession session) {
        session.releaseSession();
    }

    public synchronized void removeExpired(final IOSession iosession) {
        @SuppressWarnings("unchecked")
        PoolEntry<SocketAddress> entry = (PoolEntry<SocketAddress>) iosession.getAttribute(
                PoolEntry.ATTRIB);
        if (entry != null) {
            this.pool.release(entry, false);
        }
    }

    public synchronized void shutdown() {
        this.pool.shutdown();
    }

    static class InternalRouteResolver implements RouteResolver<SocketAddress> {

        public SocketAddress resolveLocalAddress(final SocketAddress route) {
            return null;
        }

        public SocketAddress resolveRemoteAddress(final SocketAddress route) {
            return route;
        }

    }
    
    static class InternalPoolEntryCallback implements PoolEntryCallback<SocketAddress> {

        private final SessionPool<SocketAddress> pool;
        private final BasicFuture<ManagedIOSession> future;
        
        public InternalPoolEntryCallback(
                final SessionPool<SocketAddress> pool,
                final BasicFuture<ManagedIOSession> future) {
            super();
            this.pool = pool;
            this.future = future;
        }
        
        public void completed(final PoolEntry<SocketAddress> entry) {
            ManagedIOSession result = new BasicManagedIOSession(this.pool, entry);
            this.future.completed(result);
        }

        public void failed(final Exception ex) {
            this.future.failed(ex);
        }

        public void cancelled() {
            this.future.cancel(true);
        }

    }

}
