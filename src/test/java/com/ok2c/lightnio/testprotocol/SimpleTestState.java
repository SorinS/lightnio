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

import java.nio.charset.Charset;

import com.ok2c.lightnio.SessionBufferStatus;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.ByteBufferAllocator;
import com.ok2c.lightnio.impl.SessionInputBufferImpl;
import com.ok2c.lightnio.impl.SessionOutputBufferImpl;

/**
 * Test session. This class is NOT thread-safe. It should not be 
 * accessed outside the I/O dispatch thread of the I/O reactor.
 */
public class SimpleTestState implements SessionBufferStatus {

	private static final int INIT_BUFFER_SIZE = 8 * 1024;
	private static final int LINE_BUFFER_SIZE = 1 * 1024;
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private final SessionInputBuffer inbuf;
	private final SessionOutputBuffer outbuf;
	
	private SimpleTestStatus status;
	private SimplePattern pattern;
	
	public SimpleTestState(final ByteBufferAllocator allocator) {
		super();
		this.inbuf = new SessionInputBufferImpl(
				INIT_BUFFER_SIZE, 
				LINE_BUFFER_SIZE,
				allocator,
				ASCII);
		this.outbuf = new SessionOutputBufferImpl(
				INIT_BUFFER_SIZE, 
				LINE_BUFFER_SIZE,
				allocator,
				ASCII);
		this.status = SimpleTestStatus.IDLE;
	}

	public SessionInputBuffer getInBuffer() {
		return this.inbuf;
	}

	public SessionOutputBuffer getOutBuffer() {
		return this.outbuf;
	}

	public boolean hasBufferedInput() {
		return this.inbuf.hasData();
	}

	public boolean hasBufferedOutput() {
		return this.outbuf.hasData();
	}

	public SimpleTestStatus getStatus() {
		return this.status;
	}

	public void setStatus(final SimpleTestStatus status) {
		this.status = status;
	}

	public SimplePattern getPattern() {
		return this.pattern;
	}

	public void setPattern(final SimplePattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("[state: ");
		buffer.append(this.status);
		buffer.append("]");
		buffer.append("[in buffer: ");
		buffer.append(this.inbuf.length());
		buffer.append("]");
		buffer.append("[out buffer: ");
		buffer.append(this.outbuf.length());
		buffer.append("]");
		buffer.append("[pattern: ");
		buffer.append(this.pattern);
		buffer.append("]");
		return buffer.toString();
	}
	
}
