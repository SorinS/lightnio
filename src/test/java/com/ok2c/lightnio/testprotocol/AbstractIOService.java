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
import java.io.InterruptedIOException;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactor;
import com.ok2c.lightnio.IOReactorStatus;
import com.ok2c.lightnio.ListenerEndpoint;

public abstract class AbstractIOService<R extends IOReactor> {

    private final R ioReactor;

    private volatile IOReactorThread thread;
    private ListenerEndpoint endpoint;
    
    public AbstractIOService(R ioreactor) throws IOException {
        super();
        this.ioReactor = ioreactor;
    }
    
    protected R getIOReactor() {
        return this.ioReactor;
    }
    
    protected abstract IOEventDispatch createIOEventDispatch(SimpleProtocolHandler handler); 
    
    public void start(final SimpleProtocolHandler handler) {
        this.thread = new IOReactorThread(handler);
        this.thread.start();
    }

    private void execute(final SimpleProtocolHandler handler) throws IOException {
        this.ioReactor.execute(createIOEventDispatch(handler));
    }
    
    public ListenerEndpoint getListenerEndpoint() {
        return this.endpoint;
    }

    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
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
    
    public void shutdown(long timeout) throws IOException {
        long start = System.currentTimeMillis();
        this.ioReactor.shutdown(timeout);
        timeout = System.currentTimeMillis() - start;
        if (timeout <= 0) {
            timeout = 1;
        }
        try {
            join(timeout);
        } catch (InterruptedException ex) {
            throw new InterruptedIOException(ex.getMessage());
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
