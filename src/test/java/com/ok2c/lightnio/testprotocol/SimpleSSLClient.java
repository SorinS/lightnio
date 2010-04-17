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

import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;
import com.ok2c.lightnio.impl.SSLMode;

public class SimpleSSLClient extends AbstractIOService<DefaultConnectingIOReactor> {

    private final SSLContext sslcontext;

    public SimpleSSLClient(final IOReactorConfig config) throws Exception {
        super(new DefaultConnectingIOReactor(
                config,
                new SimpleThreadFactory("SSL client")));
        this.sslcontext = createSSLContext();
    }

    private TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException {
        String algo = TrustManagerFactory.getDefaultAlgorithm();
        try {
            return TrustManagerFactory.getInstance(algo);
        } catch (NoSuchAlgorithmException ex) {
            return TrustManagerFactory.getInstance("SunX509");
        }
    }

    protected SSLContext createSSLContext() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("test.keystore");
        KeyStore keystore = KeyStore.getInstance("jks");
        keystore.load(url.openStream(), "nopassword".toCharArray());
        TrustManagerFactory tmfactory = createTrustManagerFactory();
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, trustmanagers, null);
        return sslcontext;
    }

    @Override
    protected IOEventDispatch createIOEventDispatch(final SimpleProtocolHandler handler) {
        return new SimpleSSLIOEventDispatch(
                "ssl-client", this.sslcontext, SSLMode.CLIENT, handler);
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        getIOReactor().setExceptionHandler(exceptionHandler);
    }

    public SessionRequest openConnection(final InetSocketAddress address, final Object attachment) {
         return getIOReactor().connect(address, null, attachment, null);
    }

    public List<ExceptionEvent> getAuditLog() {
        return getIOReactor().getAuditLog();
    }

}
