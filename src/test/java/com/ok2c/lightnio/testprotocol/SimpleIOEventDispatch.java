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

	private static final String ATTRIB = "test-state";
	
	private final SimpleProtocolHandler handler;
	
	public SimpleIOEventDispatch(final SimpleProtocolHandler handler) {
		super();
		this.handler = handler;
	}
	
	public void connected(final IOSession session) {
		SimpleTestState state = new SimpleTestState(new HeapByteBufferAllocator());
		session.setBufferStatus(state);
		session.setAttribute(ATTRIB, state);
		try {
			this.handler.connected(session, state);
		} catch (IOException ex) {
			this.handler.exception(session, state, ex);
			session.close();
		}
	}

	public void disconnected(final IOSession session) {
		SimpleTestState state = (SimpleTestState) session.getAttribute(ATTRIB);
		try {
			this.handler.disconnected(session, state);
		} catch (IOException ex) {
		}
	}

	public void inputReady(final IOSession session) {
		SimpleTestState state = (SimpleTestState) session.getAttribute(ATTRIB);
		try {
			this.handler.inputReady(session, state);
		} catch (IOException ex) {
			this.handler.exception(session, state, ex);
			session.close();
		}
	}

	public void outputReady(final IOSession session) {
		SimpleTestState state = (SimpleTestState) session.getAttribute(ATTRIB);
		try {
			this.handler.outputReady(session, state);
		} catch (IOException ex) {
			this.handler.exception(session, state, ex);
			session.close();
		}
	}

	public void timeout(final IOSession session) {
		session.close();
	}

}
