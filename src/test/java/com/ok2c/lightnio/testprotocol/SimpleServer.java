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
import java.util.List;

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOReactorExceptionHandler;
import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.impl.DefaultListeningIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class SimpleServer extends AbstractIOService<DefaultListeningIOReactor>{

    private ListenerEndpoint endpoint;
    
    public SimpleServer(IOReactorConfig config) throws IOException {
        super(new DefaultListeningIOReactor(
                config, 
                new SimpleThreadFactory("Server")));
    }

    @Override
    protected IOEventDispatch createIOEventDispatch(final SimpleProtocolHandler handler) {
        return new SimpleIOEventDispatch("server", handler);
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
