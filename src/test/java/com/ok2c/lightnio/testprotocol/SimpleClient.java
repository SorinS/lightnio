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
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.impl.DefaultConnectingIOReactor;
import com.ok2c.lightnio.impl.ExceptionEvent;
import com.ok2c.lightnio.impl.IOReactorConfig;

public class SimpleClient extends AbstractIOService<DefaultConnectingIOReactor> {

    public SimpleClient(final IOReactorConfig config) throws IOException {
        super(new DefaultConnectingIOReactor(
                config,
                new SimpleThreadFactory("Client")));
    }

    @Override
    protected IOEventDispatch createIOEventDispatch(final SimpleProtocolHandler handler) {
        return new SimpleIOEventDispatch("client", handler);
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
