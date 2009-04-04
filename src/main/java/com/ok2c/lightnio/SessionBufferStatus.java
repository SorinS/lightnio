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
package com.ok2c.lightnio;

/**
 * SessionBufferStatus interface is intended to query the status of session
 * I/O buffers.
 */
public interface SessionBufferStatus {

    /**
     * Determines if the session input buffer contains data.
     *
     * @return <code>true</code> if the session input buffer contains data,
     *   <code>false</code> otherwise.
     */
    boolean hasBufferedInput();

    /**
     * Determines if the session output buffer contains data.
     *
     * @return <code>true</code> if the session output buffer contains data,
     *   <code>false</code> otherwise.
     */
    boolean hasBufferedOutput();

}
