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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;


import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.ByteBufferAllocator;
import com.ok2c.lightnio.buffer.CharArrayBuffer;
import com.ok2c.lightnio.buffer.ExpandableBuffer;
import com.ok2c.lightnio.buffer.HeapByteBufferAllocator;

/**
 * Default implementation of {@link SessionOutputBuffer} based on
 * the {@link ExpandableBuffer} class.
 */
public class SessionOutputBufferImpl extends ExpandableBuffer implements SessionOutputBuffer {

    private static final byte[] CRLF = new byte[] {TextConsts.CR, TextConsts.LF};

    private final CharBuffer charbuffer;
    private CharsetEncoder charencoder;

    public SessionOutputBufferImpl(
            int buffersize,
            int linebuffersize,
            final ByteBufferAllocator allocator,
            final Charset charset) {
        super(buffersize, allocator);
        if (charset == null) {
            throw new IllegalArgumentException("Charset may not be null");
        }
        this.charbuffer = CharBuffer.allocate(linebuffersize);
        this.charencoder = charset.newEncoder();
    }

    public SessionOutputBufferImpl(
            int buffersize,
            int linebuffersize,
            final Charset charset) {
        this(buffersize, linebuffersize, new HeapByteBufferAllocator(), charset);
    }

    public void resetCharset(final Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("Charset may not be null");
        }
        if (!this.charencoder.charset().equals(charset)) {
            this.charencoder = charset.newEncoder();
        }
    }
    
    public int flush(final WritableByteChannel channel) throws IOException {
        if (channel == null) {
            throw new IllegalArgumentException("Channel may not be null");
        }
        setOutputMode();
        int noWritten = channel.write(this.buffer);
        return noWritten;
    }

    public void write(final ByteBuffer src) {
        if (src == null) {
            return;
        }
        setInputMode();
        int requiredCapacity = this.buffer.position() + src.remaining();
        ensureCapacity(requiredCapacity);
        this.buffer.put(src);
    }

    public void write(final ReadableByteChannel src) throws IOException {
        if (src == null) {
            return;
        }
        setInputMode();
        src.read(this.buffer);
    }

    private void write(final byte[] b) {
        if (b == null) {
            return;
        }
        setInputMode();
        int off = 0;
        int len = b.length;
        int requiredCapacity = this.buffer.position() + len;
        ensureCapacity(requiredCapacity);
        this.buffer.put(b, off, len);
    }

    private void writeCRLF() {
        write(CRLF);
    }

    public void writeLine(final CharArrayBuffer linebuffer) throws CharacterCodingException {
        if (linebuffer == null) {
            return;
        }
        // Do not bother if the buffer is empty
        if (linebuffer.length() > 0 ) {
            setInputMode();
            this.charencoder.reset();
            // transfer the string in small chunks
            int remaining = linebuffer.length();
            int offset = 0;
            while (remaining > 0) {
                int l = this.charbuffer.remaining();
                boolean eol = false;
                if (remaining <= l) {
                    l = remaining;
                    // terminate the encoding process
                    eol = true;
                }
                this.charbuffer.put(linebuffer.buffer(), offset, l);
                this.charbuffer.flip();

                boolean retry = true;
                while (retry) {
                    CoderResult result = this.charencoder.encode(this.charbuffer, this.buffer, eol);
                    if (result.isError()) {
                        result.throwException();
                    }
                    if (result.isOverflow()) {
                        expand();
                    }
                    retry = !result.isUnderflow();
                }
                this.charbuffer.compact();
                offset += l;
                remaining -= l;
            }
            // flush the encoder
            boolean retry = true;
            while (retry) {
                CoderResult result = this.charencoder.flush(this.buffer);
                if (result.isError()) {
                    result.throwException();
                }
                if (result.isOverflow()) {
                    expand();
                }
                retry = !result.isUnderflow();
            }
        }
        writeCRLF();
    }

    public void writeLine(final String s) throws IOException {
        if (s == null) {
            return;
        }
        if (s.length() > 0) {
            CharArrayBuffer tmp = new CharArrayBuffer(s.length());
            tmp.append(s);
            writeLine(tmp);
        } else {
            write(CRLF);
        }
    }

}
