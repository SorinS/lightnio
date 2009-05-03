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
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.impl.DefaultListeningIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;
import com.ok2c.lightnio.impl.SSLMode;

public class SimpleSSLServer {

    private final DefaultListeningIOReactor ioReactor;

    private volatile IOReactorThread thread;
    private ListenerEndpoint endpoint;
    
    public SimpleSSLServer(IOReactorConfig config) throws IOException {
        super();
        this.ioReactor = new DefaultListeningIOReactor(
                config, 
                new SimpleThreadFactory("SSL server"));
    }

    public void setExceptionHandler(final IOReactorExceptionHandler exceptionHandler) {
        this.ioReactor.setExceptionHandler(exceptionHandler);
    }

    private void execute(final SimpleProtocolHandler handler) throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("test.keystore");
        KeyStore keystore  = KeyStore.getInstance("jks");
        keystore.load(url.openStream(), "nopassword".toCharArray());
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, "nopassword".toCharArray());
        KeyManager[] keymanagers = kmfactory.getKeyManagers(); 
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(keymanagers, null, null);
        
        IOEventDispatch ioEventDispatch = new SimpleSSLIOEventDispatch(
                "SSL server", sslcontext, SSLMode.SERVER, handler);
        this.ioReactor.execute(ioEventDispatch);
    }
    
    public ListenerEndpoint getListenerEndpoint() {
        return this.endpoint;
    }

    public void start(final SimpleProtocolHandler handler) {
        this.endpoint = this.ioReactor.listen(new InetSocketAddress(0));
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
