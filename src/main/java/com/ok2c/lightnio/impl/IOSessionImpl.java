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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionBufferStatus;

/**
 * Default implementation of {@link IOSession}.
 */
public class IOSessionImpl implements IOSession {

    private volatile int status;

    private final SelectionKey key;
    private final ByteChannel channel;
    private final Map<String, Object> attributes;
    private final InterestOpsCallback interestOpsCallback;
    private final SessionClosedCallback sessionClosedCallback;

    private SessionBufferStatus bufferStatus;
    private int socketTimeout;
    private volatile int currentEventMask;

    public IOSessionImpl(
            final SelectionKey key,
            final InterestOpsCallback interestOpsCallback,
            final SessionClosedCallback sessionClosedCallback) {
        super();
        if (key == null) {
            throw new IllegalArgumentException("Selection key may not be null");
        }
        this.key = key;
        this.channel = (ByteChannel) this.key.channel();
        this.interestOpsCallback = interestOpsCallback;
        this.sessionClosedCallback = sessionClosedCallback;
        this.attributes = Collections.synchronizedMap(new HashMap<String, Object>());
        this.currentEventMask = 0;
        this.socketTimeout = 0;
        this.status = ACTIVE;
    }

    public IOSessionImpl(
            final SelectionKey key,
            final SessionClosedCallback sessionClosedCallback) {
        this(key, null, sessionClosedCallback);
    }

    public ByteChannel channel() {
        return this.channel;
    }

    public SocketAddress getLocalAddress() {
        Channel channel = this.channel;
        if (channel instanceof SocketChannel) {
            return ((SocketChannel)channel).socket().getLocalSocketAddress();
        } else {
            return null;
        }
    }

    public SocketAddress getRemoteAddress() {
        Channel channel = this.channel;
        if (channel instanceof SocketChannel) {
            return ((SocketChannel)channel).socket().getRemoteSocketAddress();
        } else {
            return null;
        }
    }

    public synchronized int getEventMask() {
        return this.interestOpsCallback != null ? this.currentEventMask : this.key.interestOps();
    }

    public synchronized void setEventMask(int ops) {
        if (this.status == CLOSED) {
            return;
        }
        if (this.interestOpsCallback != null) {
            // update the current event mask
            this.currentEventMask = ops;

            // local variable
            InterestOpEntry entry = new InterestOpEntry(this.key, this.currentEventMask);

            // add this operation to the interestOps() queue
            this.interestOpsCallback.addInterestOps(entry);
        } else {
            this.key.interestOps(ops);
        }
        this.key.selector().wakeup();
    }

    public synchronized void setEvent(int op) {
        if (this.status == CLOSED) {
            return;
        }
        if (this.interestOpsCallback != null) {
            // update the current event mask
            this.currentEventMask |= op;

            // local variable
            InterestOpEntry entry = new InterestOpEntry(this.key, this.currentEventMask);

            // add this operation to the interestOps() queue
            this.interestOpsCallback.addInterestOps(entry);
        } else {
            int ops = this.key.interestOps();
            this.key.interestOps(ops | op);
        }
        this.key.selector().wakeup();
    }

    public synchronized void clearEvent(int op) {
        if (this.status == CLOSED) {
            return;
        }
        if (this.interestOpsCallback != null) {
            // update the current event mask
            this.currentEventMask &= ~op;

            // local variable
            InterestOpEntry entry = new InterestOpEntry(this.key, this.currentEventMask);

            // add this operation to the interestOps() queue
            this.interestOpsCallback.addInterestOps(entry);
        } else {
            int ops = this.key.interestOps();
            this.key.interestOps(ops & ~op);
        }
        this.key.selector().wakeup();
    }

    public synchronized int getSocketTimeout() {
        return this.socketTimeout;
    }

    public synchronized void setSocketTimeout(int timeout) {
        this.socketTimeout = timeout;
    }

    public synchronized void close() {
        if (this.status == CLOSED) {
            return;
        }
        this.status = CLOSED;
        this.key.cancel();
        try {
            this.key.channel().close();
        } catch (IOException ex) {
            // Munching exceptions is not nice
            // but in this case it is justified
        }
        if (this.sessionClosedCallback != null) {
            this.sessionClosedCallback.sessionClosed(this);
        }
        if (this.key.selector().isOpen()) {
            this.key.selector().wakeup();
        }
    }

    public int getStatus() {
        return this.status;
    }

    public boolean isClosed() {
        return this.status == CLOSED;
    }

    public void shutdown() {
        // For this type of session, a close() does exactly
        // what we need and nothing more.
        close();
    }

    public boolean hasBufferedInput() {
        SessionBufferStatus bufferStatus = this.bufferStatus;
        return bufferStatus != null && bufferStatus.hasBufferedInput();
    }

    public boolean hasBufferedOutput() {
        SessionBufferStatus bufferStatus = this.bufferStatus;
        return bufferStatus != null && bufferStatus.hasBufferedOutput();
    }

    public void setBufferStatus(final SessionBufferStatus bufferStatus) {
        this.bufferStatus = bufferStatus;
    }

    public Object getAttribute(final String name) {
        return this.attributes.get(name);
    }

    public Object removeAttribute(final String name) {
        return this.attributes.remove(name);
    }

    public void setAttribute(final String name, final Object obj) {
        this.attributes.put(name, obj);
    }

    private static void formatOps(final StringBuffer buffer, int ops) {
        buffer.append('[');
        if ((ops & SelectionKey.OP_READ) > 0) {
            buffer.append('r');
        }
        if ((ops & SelectionKey.OP_WRITE) > 0) {
            buffer.append('w');
        }
        if ((ops & SelectionKey.OP_ACCEPT) > 0) {
            buffer.append('a');
        }
        if ((ops & SelectionKey.OP_CONNECT) > 0) {
            buffer.append('c');
        }
        buffer.append(']');
    }

    @Override
    public synchronized String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        if (this.key.isValid()) {
            buffer.append("interested ops: ");
            formatOps(buffer, this.interestOpsCallback != null ?
                    this.currentEventMask : this.key.interestOps());
            buffer.append("; ready ops: ");
            formatOps(buffer, this.key.readyOps());
        } else {
            buffer.append("invalid");
        }
        buffer.append("]");
        return buffer.toString();
    }

}
