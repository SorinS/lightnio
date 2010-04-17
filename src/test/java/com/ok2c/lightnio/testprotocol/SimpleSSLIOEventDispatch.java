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

import java.io.IOException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.buffer.HeapByteBufferAllocator;
import com.ok2c.lightnio.impl.SSLIOSession;
import com.ok2c.lightnio.impl.SSLMode;

public class SimpleSSLIOEventDispatch implements IOEventDispatch {

    private static final String TEST_STATE = "test-state";
    private static final String TEST_SESSION = "test-session";
    private static final String SSL_SESSION = "ssl-session";

    private final String id;
    private final SSLContext sslcontext;
    private final SSLMode mode;
    private final SimpleProtocolHandler handler;

    public SimpleSSLIOEventDispatch(
            final String id,
            final SSLContext sslcontext,
            final SSLMode mode,
            final SimpleProtocolHandler handler) {
        super();
        this.id = id;
        this.sslcontext = sslcontext;
        this.mode = mode;
        this.handler = handler;
    }

    public void connected(final IOSession session) {
        SimpleTestState state = new SimpleTestState(new HeapByteBufferAllocator());

        SSLIOSession sslSession = new SSLIOSession(session, this.sslcontext, null);
        session.setBufferStatus(state);

        IOSession testSession = new LoggingIOSession(sslSession, this.id);

        session.setAttribute(TEST_STATE, state);
        session.setAttribute(TEST_SESSION, testSession);
        session.setAttribute(SSL_SESSION, sslSession);

        try {
            this.handler.connected(testSession, state);
        } catch (IOException ex) {
            this.handler.exception(testSession, state, ex);
            session.close();
        }

        try {
            sslSession.bind(this.mode);
        } catch (SSLException ex) {
            this.handler.exception(testSession, state, ex);
            testSession.shutdown();
        }

    }

    public void disconnected(final IOSession session) {
        SimpleTestState state = (SimpleTestState) session.getAttribute(TEST_STATE);
        IOSession testSession = (IOSession) session.getAttribute(TEST_SESSION);
        try {
            this.handler.disconnected(testSession, state);
        } catch (IOException ex) {
        }
    }

    public void inputReady(final IOSession session) {
        SimpleTestState state = (SimpleTestState) session.getAttribute(TEST_STATE);
        IOSession testSession = (IOSession) session.getAttribute(TEST_SESSION);
        SSLIOSession sslSession = (SSLIOSession) session.getAttribute(SSL_SESSION);

        try {
            if (sslSession.isAppInputReady()) {
                this.handler.inputReady(testSession, state);
            }
            sslSession.inboundTransport();
        } catch (IOException ex) {
            this.handler.exception(testSession, state, ex);
            sslSession.shutdown();
        }
    }

    public void outputReady(final IOSession session) {
        SimpleTestState state = (SimpleTestState) session.getAttribute(TEST_STATE);
        IOSession testSession = (IOSession) session.getAttribute(TEST_SESSION);
        SSLIOSession sslSession = (SSLIOSession) session.getAttribute(SSL_SESSION);

        try {
            if (sslSession.isAppOutputReady()) {
                this.handler.outputReady(testSession, state);
            }
            sslSession.outboundTransport();
        } catch (IOException ex) {
            this.handler.exception(testSession, state, ex);
            sslSession.shutdown();
        }
    }

    public void timeout(final IOSession session) {
        SSLIOSession sslSession = (SSLIOSession) session.getAttribute(TEST_SESSION);

        synchronized (sslSession) {
            if (sslSession.getStatus() == IOSession.ACTIVE) {
                sslSession.close();
            } else if (sslSession.getStatus() == IOSession.CLOSING) {
                if (sslSession.isOutboundDone() && !sslSession.isInboundDone()) {
                    // The session failed to terminate cleanly
                    sslSession.shutdown();
                }
            }
        }
    }

}
