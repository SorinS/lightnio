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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;


import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.buffer.ByteBufferAllocator;
import com.ok2c.lightnio.buffer.CharArrayBuffer;
import com.ok2c.lightnio.buffer.ExpandableBuffer;
import com.ok2c.lightnio.buffer.HeapByteBufferAllocator;

/**
 * Default implementation of {@link SessionInputBuffer} based on
 * the {@link ExpandableBuffer} class.
 */
public class SessionInputBufferImpl extends ExpandableBuffer implements SessionInputBuffer {

    private final CharBuffer charbuffer;
    private CharsetDecoder chardecoder;

    public SessionInputBufferImpl(
            int buffersize,
            int linebuffersize,
            final ByteBufferAllocator allocator,
            final Charset charset) {
        super(buffersize, allocator);
        if (charset == null) {
            throw new IllegalArgumentException("Charset may not be null");
        }
        this.charbuffer = CharBuffer.allocate(linebuffersize);
        this.chardecoder = charset.newDecoder();
    }

    public SessionInputBufferImpl(
            int buffersize,
            int linebuffersize,
            final Charset charset) {
        this(buffersize, linebuffersize, new HeapByteBufferAllocator(), charset);
    }

    public void resetCharset(final Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("Charset may not be null");
        }
        if (!this.chardecoder.charset().equals(charset)) {
            this.chardecoder = charset.newDecoder();
        }
    }

    public int fill(final ReadableByteChannel channel) throws IOException {
        if (channel == null) {
            throw new IllegalArgumentException("Channel may not be null");
        }
        setInputMode();
        if (!this.buffer.hasRemaining()) {
            expand();
        }
        int readNo = channel.read(this.buffer);
        return readNo;
    }

    public int read() {
        setOutputMode();
        return this.buffer.get() & 0xff;
    }

    public int read(final ByteBuffer dst, int maxLen) {
        if (dst == null) {
            return 0;
        }
        setOutputMode();
        int len = Math.min(dst.remaining(), maxLen);
        int chunk = Math.min(this.buffer.remaining(), len);
        for (int i = 0; i < chunk; i++) {
            dst.put(this.buffer.get());
        }
        return chunk;
    }

    public int read(final ByteBuffer dst) {
        if (dst == null) {
            return 0;
        }
        return read(dst, dst.remaining());
    }

    public int read(final WritableByteChannel dst, int maxLen) throws IOException {
        if (dst == null) {
            return 0;
        }
        setOutputMode();
        int bytesRead;
        if (this.buffer.remaining() > maxLen) {
            int oldLimit = this.buffer.limit();
            int newLimit = oldLimit - (this.buffer.remaining() - maxLen);
            this.buffer.limit(newLimit);
            bytesRead = dst.write(this.buffer);
            this.buffer.limit(oldLimit);
        } else {
            bytesRead = dst.write(this.buffer);
        }
        return bytesRead;
    }

    public int read(final WritableByteChannel dst) throws IOException {
        if (dst == null) {
            return 0;
        }
        setOutputMode();
        return dst.write(this.buffer);
    }

    protected int findLineDelim() {
        for (int i = this.buffer.position(); i < this.buffer.limit(); i++) {
            int b = this.buffer.get(i);
            if (b == TextConsts.LF) {
                return i + 1;
            }
        }
        return -1;
    }

    public boolean readLine(
            final CharArrayBuffer linebuffer,
            boolean endOfStream) throws CharacterCodingException {

        setOutputMode();
        int pos = findLineDelim();
        if (pos == -1) {
            if (endOfStream && this.buffer.hasRemaining()) {
                // No more data. Get the rest
                pos = this.buffer.limit();
            } else {
                // Either no complete line present in the buffer
                // or no more data is expected
                return false;
            }
        }
        int origLimit = this.buffer.limit();
        this.buffer.limit(pos);

        int len = this.buffer.limit() - this.buffer.position();
        // Ensure capacity of len assuming ASCII as the most likely charset
        linebuffer.ensureCapacity(len);

        this.chardecoder.reset();

        for (;;) {
            CoderResult result = this.chardecoder.decode(
                    this.buffer,
                    this.charbuffer,
                    true);
            if (result.isError()) {
                result.throwException();
            }
            if (result.isOverflow()) {
                this.charbuffer.flip();
                linebuffer.append(
                        this.charbuffer.array(),
                        this.charbuffer.position(),
                        this.charbuffer.remaining());
                this.charbuffer.clear();
            }
            if (result.isUnderflow()) {
                break;
            }
        }
        this.buffer.limit(origLimit);

        // flush the decoder
        this.chardecoder.flush(this.charbuffer);
        this.charbuffer.flip();
        // append the decoded content to the line buffer
        if (this.charbuffer.hasRemaining()) {
            linebuffer.append(
                    this.charbuffer.array(),
                    this.charbuffer.position(),
                    this.charbuffer.remaining());
        }

        // discard LF if found
        int l = linebuffer.length();
        if (l > 0) {
            if (linebuffer.charAt(l - 1) == TextConsts.LF) {
                l--;
                linebuffer.setLength(l);
            }
            // discard CR if found
            if (l > 0) {
                if (linebuffer.charAt(l - 1) == TextConsts.CR) {
                    l--;
                    linebuffer.setLength(l);
                }
            }
        }
        return true;
    }

    public String readLine(boolean endOfStream) throws CharacterCodingException {
        CharArrayBuffer charbuffer = new CharArrayBuffer(64);
        boolean found = readLine(charbuffer, endOfStream);
        if (found) {
            return charbuffer.toString();
        } else {
            return null;
        }
    }

}
