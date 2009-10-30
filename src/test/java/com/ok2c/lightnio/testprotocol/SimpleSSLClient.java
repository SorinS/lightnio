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
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;
import com.ok2c.lightnio.impl.SSLMode;

public class SimpleSSLClient {

    private final SSLContext sslcontext;
    private final DefaultConnectingIOReactor ioReactor;
    
    private volatile IOReactorThread thread;

    public SimpleSSLClient(final IOReactorConfig config) throws Exception {
        super();
        this.sslcontext = createSSLContext();
        this.ioReactor = new DefaultConnectingIOReactor(
                config, 
                new SimpleThreadFactory("SSL client"));
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
        KeyStore keystore  = KeyStore.getInstance("jks");
        keystore.load(url.openStream(), "nopassword".toCharArray());
        TrustManagerFactory tmfactory = createTrustManagerFactory();
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers(); 
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, trustmanagers, null);
        return sslcontext;
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    private void execute(final SimpleProtocolHandler handler) throws IOException {
        IOEventDispatch ioEventDispatch = new SimpleSSLIOEventDispatch(
                "SSL client", this.sslcontext, SSLMode.CLIENT, handler);
        this.ioReactor.execute(ioEventDispatch);
    }
    
    public SessionRequest openConnection(final InetSocketAddress address, final Object attachment) {
         return this.ioReactor.connect(address, null, attachment, null);
    }
 
    public void start(final SimpleProtocolHandler handler) {
        this.thread = new IOReactorThread(handler);
        this.thread.start();
    }

    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
    }
    
    public List<ExceptionEvent> getAuditLog() {
        return this.ioReactor.getAuditLog();
    }
    
    public void join(long timeout) throws InterruptedException {
        if (this.thread != null) {
            this.thread.join(timeout);
        }
    }
    
    public Exception getException() {
        if (this.thread != null) {
            return this.thread.getException();
        } else {
            return null;
        }
    }
    
    public void shutdown() throws IOException {
        this.ioReactor.shutdown();
        try {
            join(500);
        } catch (InterruptedException ignore) {
        }
    }
    
    private class IOReactorThread extends Thread {

        private final SimpleProtocolHandler handler;

        private volatile Exception ex;
        
        public IOReactorThread(final SimpleProtocolHandler handler) {
            super();
            this.handler = handler;
        }
        
        @Override
        public void run() {
            try {
                execute(this.handler);
            } catch (Exception ex) {
                this.ex = ex;
            }
        }
        
        public Exception getException() {
            return this.ex;
        }

    }    
    
}
