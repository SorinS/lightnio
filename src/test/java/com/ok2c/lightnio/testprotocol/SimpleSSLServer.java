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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.impl.DefaultListeningIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;
import com.ok2c.lightnio.impl.SSLMode;

public class SimpleSSLServer extends AbstractIOService<DefaultListeningIOReactor>{

    private final SSLContext sslcontext;

    private ListenerEndpoint endpoint;
    
    public SimpleSSLServer(IOReactorConfig config) throws Exception {
        super(new DefaultListeningIOReactor(
                config, 
                new SimpleThreadFactory("SSL server")));
        this.sslcontext = createSSLContext();
    }

    private KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException {
        String algo = KeyManagerFactory.getDefaultAlgorithm();
        try {
            return KeyManagerFactory.getInstance(algo);
        } catch (NoSuchAlgorithmException ex) {
            return KeyManagerFactory.getInstance("SunX509");
        }
    }
    
    protected SSLContext createSSLContext() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("test.keystore");
        KeyStore keystore  = KeyStore.getInstance("jks");
        keystore.load(url.openStream(), "nopassword".toCharArray());
        KeyManagerFactory kmfactory = createKeyManagerFactory();
        kmfactory.init(keystore, "nopassword".toCharArray());
        KeyManager[] keymanagers = kmfactory.getKeyManagers(); 
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(keymanagers, null, null);
        return sslcontext;
    }

    @Override
    protected IOEventDispatch createIOEventDispatch(final SimpleProtocolHandler handler) {
        return new SimpleSSLIOEventDispatch(
                "ssl-server", this.sslcontext, SSLMode.SERVER, handler);
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        getIOReactor().setExceptionHandler(exceptionHandler);
    }

    public ListenerEndpoint getListenerEndpoint() {
        return this.endpoint;
    }

    @Override
    public void start(final SimpleProtocolHandler handler) {
        this.endpoint = getIOReactor().listen(new InetSocketAddress(0));
        super.start(handler);
    }
    
    public List<ExceptionEvent> getAuditLog() {
        return getIOReactor().getAuditLog();
    }
    
}
