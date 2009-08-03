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
package com.ok2c.lightnio.pool;

import com.ok2c.lightnio.IOSession;

public interface ManagedIOSession {

    IOSession getSession();

    Object getState();

    void setState(Object state);

    void markReusable();

    void markNonReusable();

    boolean isReusable();

    void releaseSession();

    void abortSession();

}
