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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;

import com.ok2c.lightnio.IOReactorException;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.ListeningIOReactor;

/**
 * Default implementation of {@link ListeningIOReactor}. This class extends
 * {@link AbstractMultiworkerIOReactor} with capability to listen for incoming
 * connections.
 */
public class DefaultListeningIOReactor extends AbstractMultiworkerIOReactor
        implements ListeningIOReactor {

    private final Queue<ListenerEndpointImpl> requestQueue;
    private final Set<ListenerEndpointImpl> endpoints;
    private final Set<SocketAddress> pausedEndpoints;

    private volatile boolean paused;

    public DefaultListeningIOReactor(
            final IOReactorConfig config,
            final ThreadFactory threadFactory) throws IOReactorException {
        super(config, threadFactory);
        this.requestQueue = new ConcurrentLinkedQueue<ListenerEndpointImpl>();
        this.endpoints = Collections.synchronizedSet(new HashSet<ListenerEndpointImpl>());
        this.pausedEndpoints = new HashSet<SocketAddress>();
    }

    public DefaultListeningIOReactor(final IOReactorConfig config) throws IOReactorException {
        this(config, null);
    }

    @Override
    protected void cancelRequests() throws IOReactorException {
        ListenerEndpointImpl request;
        while ((request = this.requestQueue.poll()) != null) {
            request.cancel();
        }
    }

    @Override
    protected void processEvents(int readyCount) throws IOReactorException {
        if (!this.paused) {
            processSessionRequests();
        }

        if (readyCount > 0) {
            Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
            for (Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext(); ) {

                SelectionKey key = it.next();
                processEvent(key);

            }
            selectedKeys.clear();
        }
    }

    private void processEvent(final SelectionKey key)
            throws IOReactorException {
        try {

            if (key.isAcceptable()) {

                ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                SocketChannel socketChannel = null;
                try {
                    socketChannel = serverChannel.accept();
                } catch (IOException ex) {
                    if (this.exceptionHandler == null ||
                            !this.exceptionHandler.handle(ex)) {
                        throw new IOReactorException(
                                "Failure accepting connection", ex);
                    }
                }

                if (socketChannel != null) {
                    try {
                        prepareSocket(socketChannel.socket());
                    } catch (IOException ex) {
                        if (this.exceptionHandler == null ||
                                !this.exceptionHandler.handle(ex)) {
                            throw new IOReactorException(
                                    "Failure initalizing socket", ex);
                        }
                    }
                    ChannelEntry entry = new ChannelEntry(socketChannel);
                    addChannel(entry);
                }
            }

        } catch (CancelledKeyException ex) {
            ListenerEndpoint endpoint = (ListenerEndpoint) key.attachment();
            this.endpoints.remove(endpoint);
            key.attach(null);
        }
    }

    private ListenerEndpointImpl createEndpoint(final SocketAddress address) {
        ListenerEndpointImpl endpoint = new ListenerEndpointImpl(
                address,
                new ListenerEndpointClosedCallback() {

                    public void endpointClosed(final ListenerEndpoint endpoint) {
                        endpoints.remove(endpoint);
                    }

                });
        return endpoint;
    }

    public ListenerEndpoint listen(final SocketAddress address) {
        if (this.status.compareTo(IOReactorStatus.ACTIVE) > 0) {
            throw new IllegalStateException("I/O reactor has been shut down");
        }
        ListenerEndpointImpl request = createEndpoint(address);
        this.requestQueue.add(request);
        this.selector.wakeup();
        return request;
    }

    private void processSessionRequests() throws IOReactorException {
        ListenerEndpointImpl request;
        while ((request = this.requestQueue.poll()) != null) {
            SocketAddress address = request.getAddress();
            ServerSocketChannel serverChannel;
            try {
                serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false);
            } catch (IOException ex) {
                throw new IOReactorException("Failure opening server socket", ex);
            }
            try {
                serverChannel.socket().bind(address);
            } catch (IOException ex) {
                request.failed(ex);
                if (this.exceptionHandler == null || !this.exceptionHandler.handle(ex)) {
                    throw new IOReactorException("Failure binding socket to address "
                            + address, ex);
                } else {
                    return;
                }
            }
            try {
                SelectionKey key = serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
                key.attach(request);
                request.setKey(key);
            } catch (IOException ex) {
                throw new IOReactorException("Failure registering channel " +
                        "with the selector", ex);
            }

            this.endpoints.add(request);
            request.completed(serverChannel.socket().getLocalSocketAddress());
        }
    }

    public Set<ListenerEndpoint> getEndpoints() {
        Set<ListenerEndpoint> set = new HashSet<ListenerEndpoint>();
        synchronized (this.endpoints) {
            Iterator<ListenerEndpointImpl> it = this.endpoints.iterator();
            while (it.hasNext()) {
                ListenerEndpoint endpoint = it.next();
                if (!endpoint.isClosed()) {
                    set.add(endpoint);
                } else {
                    it.remove();
                }
            }
        }
        return set;
    }

    public void pause() throws IOException {
        if (this.paused) {
            return;
        }
        this.paused = true;
        synchronized (this.endpoints) {
            Iterator<ListenerEndpointImpl> it = this.endpoints.iterator();
            while (it.hasNext()) {
                ListenerEndpoint endpoint = it.next();
                if (!endpoint.isClosed()) {
                    endpoint.close();
                    this.pausedEndpoints.add(endpoint.getAddress());
                }
            }
            this.endpoints.clear();
        }
    }

    public void resume() throws IOException {
        if (!this.paused) {
            return;
        }
        this.paused = false;
        for (SocketAddress address: this.pausedEndpoints) {
            ListenerEndpointImpl request = createEndpoint(address);
            this.requestQueue.add(request);
        }
        this.pausedEndpoints.clear();
        this.selector.wakeup();
    }

}
