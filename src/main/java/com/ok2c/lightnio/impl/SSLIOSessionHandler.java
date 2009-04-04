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

import java.net.SocketAddress;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

/**
 * Callback interface that can be used to customize various aspects of
 * the TLS/SSl protocol.
 */
public interface SSLIOSessionHandler {

    /**
     * Triggered when the SSL connection is being initialized. Custom handlers
     * can use this callback to customize properties of the {@link SSLEngine}
     * used to establish the SSL session.
     *
     * @param sslengine the SSL engine.
     * @throws SSLException if case of SSL protocol error.
     */
    void initalize(SSLEngine sslengine)
        throws SSLException;

    /**
     * Triggered when the SSL connection has been established and initial SSL
     * handshake has been successfully completed. Custom handlers can use
     * this callback to verify properties of the {@link SSLSession}.
     * For instance this would be the right place to enforce SSL cipher
     * strength, validate certificate chain and do hostname checks.
     *
     * @param remoteAddress the remote address of the connection.
     * @param session newly created SSL session.
     * @throws SSLException if case of SSL protocol error.
     */
    void verify(SocketAddress remoteAddress, SSLSession session)
        throws SSLException;

}
