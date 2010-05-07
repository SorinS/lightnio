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
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorException;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.ListeningIOReactor;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.testprotocol.NoOpSimpleProtocolHandler;
import com.ok2c.lightnio.testprotocol.OoopsieRuntimeException;
import com.ok2c.lightnio.testprotocol.SimpleClient;
import com.ok2c.lightnio.testprotocol.SimpleClientProtocolHandler;
import com.ok2c.lightnio.testprotocol.SimpleIOEventDispatch;
import com.ok2c.lightnio.testprotocol.SimpleTestJob;
import com.ok2c.lightnio.testprotocol.SimpleServer;
import com.ok2c.lightnio.testprotocol.SimpleServerProtocolHandler;
import com.ok2c.lightnio.testprotocol.SimpleTestState;
import com.ok2c.lightnio.testprotocol.SimpleTestStatus;

/**
 * Unit tests for {@link DefaultConnectingIOReactor} and {@link DefaultListeningIOReactor}.
 */
public class TestIOReactors {

    private SimpleClient testclient;
    private SimpleServer testserver;

    @Before
    public void setUp() throws Exception {
        IOReactorConfig config = new IOReactorConfig();
        config.setWorkerCount(2);
        this.testclient = new SimpleClient(config);
        this.testserver = new SimpleServer(config);
    }

    @After
    public void tearDown() throws Exception {
        try {
            this.testclient.shutdown(1000);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        List<ExceptionEvent> clogs = this.testclient.getAuditLog();
        if (clogs != null) {
            for (ExceptionEvent clog: clogs) {
                Throwable cause = clog.getCause();
                if (!(cause instanceof OoopsieRuntimeException)) {
                    cause.printStackTrace();
                }
            }
        }

        try {
            this.testserver.shutdown(1000);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        List<ExceptionEvent> slogs = this.testserver.getAuditLog();
        if (slogs != null) {
            for (ExceptionEvent slog: slogs) {
                Throwable cause = slog.getCause();
                if (!(cause instanceof OoopsieRuntimeException)) {
                    cause.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testBasicIO() throws Exception {
        this.testserver.start(new SimpleServerProtocolHandler());
        this.testclient.start(new SimpleClientProtocolHandler());

        ListenerEndpoint listenerEndpoint = this.testserver.getListenerEndpoint();
        listenerEndpoint.waitFor();

        InetSocketAddress address = (InetSocketAddress) listenerEndpoint.getAddress();
        InetSocketAddress target = new InetSocketAddress("localhost", address.getPort());

        SimpleTestJob[] testjobs = new SimpleTestJob[50];
        for (int i = 0; i < testjobs.length; i++) {
            testjobs[i] = new SimpleTestJob(1000);
        }
        for (int i = 0; i < testjobs.length; i++) {
            SimpleTestJob testjob = testjobs[i];
            SessionRequest sessionRequest = this.testclient.openConnection(target, testjob);
            sessionRequest.waitFor();
            if (sessionRequest.getException() != null) {
                throw sessionRequest.getException();
            }
            Assert.assertNotNull(sessionRequest.getSession());
        }
        for (int i = 0; i < testjobs.length; i++) {
            SimpleTestJob testjob = testjobs[i];
            testjob.waitFor();
            Exception ex = testjob.getException();
            if (ex != null) {
                throw ex;
            }
            SimpleTestState state = testjob.getTestState();
            Assert.assertNotNull(state);
            Assert.assertEquals(SimpleTestStatus.RESPONSE_RECEIVED, state.getStatus());

            String pattern = testjob.getPattern();
            int count = testjob.getCount();
            SessionInputBuffer inbuffer = state.getInBuffer();

            for (int n = 0; n < count; n++) {
                String line = inbuffer.readLine(true);
                Assert.assertEquals(pattern, line);
            }
            Assert.assertFalse(inbuffer.hasData());
        }
    }

    @Test
    public void testGracefulShutdown() throws Exception {
        // Open connections and do nothing
        final int connNo = 10;
        final AtomicInteger openServerConns = new AtomicInteger(0);
        final AtomicInteger closedServerConns = new AtomicInteger(0);
        final AtomicInteger openClientConns = new AtomicInteger(0);
        final AtomicInteger closedClientConns = new AtomicInteger(0);

        this.testserver.start(new NoOpSimpleProtocolHandler() {

            @Override
            public void connected(IOSession session, SimpleTestState state) throws IOException {
                openServerConns.incrementAndGet();
            }

            public void disconnected(IOSession session, SimpleTestState state) throws IOException {
                closedServerConns.incrementAndGet();
            }

        });
        this.testclient.start(new NoOpSimpleProtocolHandler() {

            @Override
            public void connected(IOSession session, SimpleTestState state) throws IOException {
                openClientConns.incrementAndGet();
                session.setEventMask(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }

            @Override
            public void outputReady(IOSession session, SimpleTestState state) throws IOException {
                byte[] tmp = new byte[] {'1', '2', '3', '4', '5'};
                ByteBuffer src = ByteBuffer.wrap(tmp);
                session.channel().write(src);
            }

            @Override
            public void disconnected(IOSession session, SimpleTestState state) throws IOException {
                closedClientConns.incrementAndGet();
            }

        });

        ListenerEndpoint listenerEndpoint = this.testserver.getListenerEndpoint();
        listenerEndpoint.waitFor();

        InetSocketAddress address = (InetSocketAddress) listenerEndpoint.getAddress();
        InetSocketAddress target = new InetSocketAddress("localhost", address.getPort());

        for (int i = 0; i < connNo; i++) {
            SessionRequest sessionRequest = this.testclient.openConnection(target, null);
            sessionRequest.waitFor();
            if (sessionRequest.getException() != null) {
                throw sessionRequest.getException();
            }
            Assert.assertNotNull(sessionRequest.getSession());
        }

        // Make sure all connections go down
        this.testclient.shutdown(1000);
        this.testserver.shutdown(1000);

        Assert.assertEquals(openServerConns.get(), closedServerConns.get());
        Assert.assertEquals(openClientConns.get(), closedClientConns.get());
    }

    @Test
    public void testRuntimeException() throws Exception {
        this.testserver.start(new NoOpSimpleProtocolHandler() {

            public void connected(IOSession session, SimpleTestState state) throws IOException {
                session.setEventMask(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }

            public void outputReady(IOSession session, SimpleTestState state) throws IOException {
                session.setEventMask(SelectionKey.OP_READ);
                throw new OoopsieRuntimeException();
            }

        });
        this.testclient.start(new NoOpSimpleProtocolHandler());

        ListenerEndpoint listenerEndpoint = this.testserver.getListenerEndpoint();
        listenerEndpoint.waitFor();

        InetSocketAddress address = (InetSocketAddress) listenerEndpoint.getAddress();
        InetSocketAddress target = new InetSocketAddress("localhost", address.getPort());

        SessionRequest sessionRequest = this.testclient.openConnection(target, null);
        sessionRequest.waitFor();
        if (sessionRequest.getException() != null) {
            throw sessionRequest.getException();
        }
        Assert.assertNotNull(sessionRequest.getSession());

        this.testserver.join(20000);

        Exception ex = this.testserver.getException();
        Assert.assertNotNull(ex);
        Assert.assertTrue(ex instanceof IOReactorException);
        Assert.assertNotNull(ex.getCause());
        Assert.assertTrue(ex.getCause() instanceof OoopsieRuntimeException);

        List<ExceptionEvent> auditlog = this.testserver.getAuditLog();
        Assert.assertNotNull(auditlog);
        Assert.assertEquals(1, auditlog.size());

        // I/O reactor shut down itself
        Assert.assertEquals(IOReactorStatus.SHUT_DOWN, this.testserver.getStatus());
    }

    @Test
    public void testUnhandledRuntimeException() throws Exception {

        final AtomicInteger requestConns = new AtomicInteger(0);

        this.testserver.setExceptionHandler(new IOReactorExceptionHandler() {

            public boolean handle(final IOException ex) {
                return false;
            }

            public boolean handle(final RuntimeException ex) {
                requestConns.incrementAndGet();
                return false;
            }

        });

        this.testserver.start(new NoOpSimpleProtocolHandler() {

            public void connected(IOSession session, SimpleTestState state) throws IOException {
                session.setEventMask(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }

            public void outputReady(IOSession session, SimpleTestState state) throws IOException {
                session.setEventMask(SelectionKey.OP_READ);
                throw new OoopsieRuntimeException();
            }

        });
        this.testclient.start(new NoOpSimpleProtocolHandler());

        ListenerEndpoint listenerEndpoint = this.testserver.getListenerEndpoint();
        listenerEndpoint.waitFor();

        InetSocketAddress address = (InetSocketAddress) listenerEndpoint.getAddress();
        InetSocketAddress target = new InetSocketAddress("localhost", address.getPort());

        SessionRequest sessionRequest = this.testclient.openConnection(target, null);
        sessionRequest.waitFor();
        if (sessionRequest.getException() != null) {
            throw sessionRequest.getException();
        }
        Assert.assertNotNull(sessionRequest.getSession());

        this.testserver.join(20000);

        Assert.assertEquals(1, requestConns.get());

        Exception ex = this.testserver.getException();
        Assert.assertNotNull(ex);
        Assert.assertTrue(ex instanceof IOReactorException);
        Assert.assertNotNull(ex.getCause());
        Assert.assertTrue(ex.getCause() instanceof OoopsieRuntimeException);

        List<ExceptionEvent> auditlog = this.testserver.getAuditLog();
        Assert.assertNotNull(auditlog);
        Assert.assertEquals(1, auditlog.size());

        // I/O reactor shut down itself
        Assert.assertEquals(IOReactorStatus.SHUT_DOWN, this.testserver.getStatus());
    }

    @Test
    public void testHandledRuntimeException() throws Exception {

        final CountDownLatch requestConns = new CountDownLatch(1);

        this.testserver.setExceptionHandler(new IOReactorExceptionHandler() {

            public boolean handle(final IOException ex) {
                return false;
            }

            public boolean handle(final RuntimeException ex) {
                requestConns.countDown();
                return true;
            }

        });

        this.testserver.start(new NoOpSimpleProtocolHandler() {

            public void connected(IOSession session, SimpleTestState state) throws IOException {
                session.setEventMask(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }

            public void outputReady(IOSession session, SimpleTestState state) throws IOException {
                session.setEventMask(SelectionKey.OP_READ);
                throw new OoopsieRuntimeException();
            }

        });
        this.testclient.start(new NoOpSimpleProtocolHandler());

        ListenerEndpoint listenerEndpoint = this.testserver.getListenerEndpoint();
        listenerEndpoint.waitFor();

        InetSocketAddress address = (InetSocketAddress) listenerEndpoint.getAddress();
        InetSocketAddress target = new InetSocketAddress("localhost", address.getPort());

        SessionRequest sessionRequest = this.testclient.openConnection(target, null);
        sessionRequest.waitFor();
        if (sessionRequest.getException() != null) {
            throw sessionRequest.getException();
        }
        Assert.assertNotNull(sessionRequest.getSession());

        requestConns.await();

        Assert.assertEquals(IOReactorStatus.ACTIVE, this.testserver.getStatus());
        Assert.assertNull(this.testserver.getException());
    }

    @Test
    public void testEndpointUpAndDown() throws Exception {
        final IOEventDispatch eventDispatch = new SimpleIOEventDispatch(
                "server",
                new NoOpSimpleProtocolHandler());

        final ListeningIOReactor ioreactor = new DefaultListeningIOReactor(
                new IOReactorConfig());

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    ioreactor.execute(eventDispatch);
                } catch (IOException ex) {
                }
            }

        });

        t.start();

        Set<ListenerEndpoint> endpoints = ioreactor.getEndpoints();
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(0, endpoints.size());

        ListenerEndpoint endpoint9998 = ioreactor.listen(new InetSocketAddress(9998));
        endpoint9998.waitFor();

        ListenerEndpoint endpoint9999 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint9999.waitFor();

        endpoints = ioreactor.getEndpoints();
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(2, endpoints.size());

        endpoint9998.close();

        endpoints = ioreactor.getEndpoints();
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(1, endpoints.size());

        ListenerEndpoint endpoint = endpoints.iterator().next();

        Assert.assertEquals(9999, ((InetSocketAddress) endpoint.getAddress()).getPort());

        ioreactor.shutdown(1000);
        t.join(1000);

        Assert.assertEquals(IOReactorStatus.SHUT_DOWN, ioreactor.getStatus());
    }

    @Test
    public void testEndpointAlreadyBoundFatal() throws Exception {
        final IOEventDispatch eventDispatch = new SimpleIOEventDispatch(
                "server",
                new NoOpSimpleProtocolHandler());

        final ListeningIOReactor ioreactor = new DefaultListeningIOReactor(
                new IOReactorConfig());

        final CountDownLatch latch = new CountDownLatch(1);

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    ioreactor.execute(eventDispatch);
                    Assert.fail("IOException should have been thrown");
                } catch (IOException ex) {
                    latch.countDown();
                }
            }

        });

        t.start();

        ListenerEndpoint endpoint1 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint1.waitFor();

        ListenerEndpoint endpoint2 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint2.waitFor();
        Assert.assertNotNull(endpoint2.getException());

        // I/O reactor is now expected to be shutting down
        latch.await(2000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ioreactor.getStatus().compareTo(IOReactorStatus.SHUTTING_DOWN) >= 0);

        Set<ListenerEndpoint> endpoints = ioreactor.getEndpoints();
        Assert.assertNotNull(endpoints);
        Assert.assertEquals(0, endpoints.size());

        ioreactor.shutdown(1000);
        t.join(1000);

        Assert.assertTrue(ioreactor.getStatus().compareTo(IOReactorStatus.SHUTTING_DOWN) >= 0);
    }

    @Test
    public void testEndpointAlreadyBoundNonFatal() throws Exception {
        final IOEventDispatch eventDispatch = new SimpleIOEventDispatch(
                "server",
                new NoOpSimpleProtocolHandler());

        final DefaultListeningIOReactor ioreactor = new DefaultListeningIOReactor(
                new IOReactorConfig());
        ioreactor.setExceptionHandler(new IOReactorExceptionHandler() {

            public boolean handle(final IOException ex) {
                return (ex instanceof BindException);
            }

            public boolean handle(final RuntimeException ex) {
                return false;
            }

        });

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    ioreactor.execute(eventDispatch);
                } catch (IOException ex) {
                }
            }

        });

        t.start();

        ListenerEndpoint endpoint1 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint1.waitFor();

        ListenerEndpoint endpoint2 = ioreactor.listen(new InetSocketAddress(9999));
        endpoint2.waitFor();
        Assert.assertNotNull(endpoint2.getException());

        // Sleep a little to make sure the I/O reactor is not shutting down
        Thread.sleep(500);

        Assert.assertEquals(IOReactorStatus.ACTIVE, ioreactor.getStatus());

        ioreactor.shutdown(1000);
        t.join(1000);

        Assert.assertEquals(IOReactorStatus.SHUT_DOWN, ioreactor.getStatus());
    }

}

