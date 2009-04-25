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

import com.ok2c.lightnio.IOEventDispatch;
import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.buffer.HeapByteBufferAllocator;

public class SimpleIOEventDispatch implements IOEventDispatch {

	private static final String TEST_STATE = "test-state";
    private static final String TEST_SESSION = "test-session";
	
    private final String id;
	private final SimpleProtocolHandler handler;
	
	public SimpleIOEventDispatch(final String id, final SimpleProtocolHandler handler) {
		super();
		this.id = id;
		this.handler = handler;
	}
	
	public void connected(final IOSession session) {
		SimpleTestState state = new SimpleTestState(new HeapByteBufferAllocator());
		session.setBufferStatus(state);
        IOSession testSession = new LoggingIOSession(session, this.id);
		session.setAttribute(TEST_STATE, state);
        session.setAttribute(TEST_SESSION, testSession);
		
		try {
			this.handler.connected(testSession, state);
		} catch (IOException ex) {
			this.handler.exception(testSession, state, ex);
			session.close();
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
		try {
			this.handler.inputReady(testSession, state);
		} catch (IOException ex) {
			this.handler.exception(testSession, state, ex);
			session.close();
		}
	}

	public void outputReady(final IOSession session) {
		SimpleTestState state = (SimpleTestState) session.getAttribute(TEST_STATE);
        IOSession testSession = (IOSession) session.getAttribute(TEST_SESSION);
		try {
			this.handler.outputReady(testSession, state);
		} catch (IOException ex) {
			this.handler.exception(testSession, state, ex);
			session.close();
		}
	}

	public void timeout(final IOSession session) {
		session.close();
	}

}
