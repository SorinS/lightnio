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
import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ok2c.lightnio.ListenerEndpoint;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionRequest;
import com.ok2c.lightnio.testprotocol.SimpleClient;
import com.ok2c.lightnio.testprotocol.SimpleClientProtocolHandler;
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
            this.testclient.shutdown();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        List<ExceptionEvent> clogs = this.testclient.getAuditLog();
        if (clogs != null) {
            for (ExceptionEvent clog: clogs) {
                Throwable cause = clog.getCause();
                if (cause != null) {
                    cause.printStackTrace();
                }
            }
        }

        try {
            this.testserver.shutdown();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        List<ExceptionEvent> slogs = this.testserver.getAuditLog();
        if (slogs != null) {
            for (ExceptionEvent slog: slogs) {
                Throwable cause = slog.getCause();
                if (cause != null) {
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

}