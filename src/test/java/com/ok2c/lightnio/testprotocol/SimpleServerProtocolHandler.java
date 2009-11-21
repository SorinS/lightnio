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
import java.nio.channels.SelectionKey;

import com.ok2c.lightnio.IOSession;
import com.ok2c.lightnio.SessionInputBuffer;
import com.ok2c.lightnio.SessionOutputBuffer;
import com.ok2c.lightnio.buffer.CharArrayBuffer;

public class SimpleServerProtocolHandler implements SimpleProtocolHandler {

    public void connected(
            final IOSession session, 
            final SimpleTestState state) {
        session.setEventMask(SelectionKey.OP_READ);
    }

    public void inputReady(
            final IOSession session, 
            final SimpleTestState state) throws IOException {
        SessionInputBuffer inbuf = state.getInBuffer();
        switch (state.getStatus()) {
        case IDLE:
            state.setStatus(SimpleTestStatus.REQUEST_RECEIVING);
        case REQUEST_RECEIVING:
            int bytesRead = inbuf.fill(session.channel());
            CharArrayBuffer linebuffer = new CharArrayBuffer(64);
            if (inbuf.readLine(linebuffer, bytesRead == -1)) {
                int i = linebuffer.indexOf('x');
                if (i == -1) {
                    throw new IOException("Protocol violation");
                }
                String p = linebuffer.substring(0, i);
                String n = linebuffer.substring(i + 1, linebuffer.length());
                int count;
                try {
                    count = Integer.parseInt(n);
                } catch (NumberFormatException ex) {
                    throw new IOException("Protocol violation");
                }
                session.setAttribute("pattern", new SimplePattern(p, count));
                state.setStatus(SimpleTestStatus.REQUEST_RECEIVED);
                session.setEventMask(SelectionKey.OP_WRITE);
            }
            if (bytesRead == -1) {
                session.close();
            }
            break;
        default:
            throw new IllegalStateException("Unexpected state: " + state.getStatus());
        }
    }

    public void outputReady(
            final IOSession session, 
            final SimpleTestState state) throws IOException {
        SessionOutputBuffer outbuf = state.getOutBuffer();
        switch (state.getStatus()) {
        case REQUEST_RECEIVED:
            SimplePattern pattern = (SimplePattern) session.getAttribute("pattern");
            CharArrayBuffer buffer = new CharArrayBuffer(32); 
            buffer.append(pattern.getText());
            for (int i = 0; i < pattern.getCount(); i++) {
                outbuf.writeLine(buffer);
            }
            state.setStatus(SimpleTestStatus.RESPONSE_SENDING);
        case RESPONSE_SENDING:
            outbuf.flush(session.channel());
            if (!outbuf.hasData()) {
                state.setStatus(SimpleTestStatus.RESPONSE_SENT);
            } else {
                break;
            }
        case RESPONSE_SENT:
            session.close();
            break;
        default:
            throw new IllegalStateException("Unexpected state: " + state.getStatus());
        }
    }

    public void disconnected(
            final IOSession session, 
            final SimpleTestState state) {
    }

    public void exception(final IOSession session, final SimpleTestState state, final Exception ex) {
        state.setStatus(SimpleTestStatus.FAILURE);
    }
    
}
