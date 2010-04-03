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

public class SimpleClientProtocolHandler implements SimpleProtocolHandler {

    public void connected(
            final IOSession session, 
            final SimpleTestState state) {
        SimpleTestJob job = (SimpleTestJob) session.getAttribute(IOSession.ATTACHMENT_KEY);
        if (job == null) {
            throw new IllegalStateException("Test job is null");
        }
        session.setAttribute("pattern", new SimplePattern(job.getPattern(), job.getCount()));
        session.setEventMask(SelectionKey.OP_WRITE);
    }

    public void outputReady(
            final IOSession session, 
            final SimpleTestState state) throws IOException {
        SessionOutputBuffer outbuf = state.getOutBuffer();
        switch (state.getStatus()) {
        case IDLE:
            SimplePattern pattern = (SimplePattern) session.getAttribute("pattern");
            outbuf.writeLine(pattern.toString());
            state.setStatus(SimpleTestStatus.REQUEST_SENDING);
        case REQUEST_SENDING:
            outbuf.flush(session.channel());
            if (outbuf.hasData()) {
                break;
            } else {
                state.setStatus(SimpleTestStatus.REQUEST_SENT);
            }
        case REQUEST_SENT:
            session.setEventMask(SelectionKey.OP_READ);
            break;
        default:
            throw new IllegalStateException("Unexpected state: " + state.getStatus());
        }
    }

    public void inputReady(
            final IOSession session, 
            final SimpleTestState state) throws IOException {
        SessionInputBuffer inbuf = state.getInBuffer();
        switch (state.getStatus()) {
        case REQUEST_SENT:
            state.setStatus(SimpleTestStatus.RESPONSE_RECEIVING);
        case RESPONSE_RECEIVING:
            int bytesRead = inbuf.fill(session.channel());
            if (bytesRead == -1) {
                state.setStatus(SimpleTestStatus.RESPONSE_RECEIVED);
                session.close();
            }
            break;
        default:
            throw new IllegalStateException("Unexpected state: " + state.getStatus());
        }
    }

    public void disconnected(
            final IOSession session, 
            final SimpleTestState state) {
        SimpleTestJob job = (SimpleTestJob) session.getAttribute(IOSession.ATTACHMENT_KEY);
        if (job == null) {
            throw new IllegalStateException("Test job is null");
        }
        if (state.getStatus().equals(SimpleTestStatus.RESPONSE_RECEIVING)) {
            state.setStatus(SimpleTestStatus.RESPONSE_RECEIVED);
        }
        job.success(state);
    }

    public void exception(final IOSession session, final SimpleTestState state, final Exception ex) {
        SimpleTestJob job = (SimpleTestJob) session.getAttribute(IOSession.ATTACHMENT_KEY);
        if (job == null) {
            throw new IllegalStateException("Test job is null");
        }
        state.setStatus(SimpleTestStatus.FAILURE);
        job.failure(state, ex);
    }
    
}
