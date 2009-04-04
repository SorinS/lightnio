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

public final class IOReactorConfig {

    private int workerCount;
    private long selectInterval;
    private boolean interestOpsQueueing;
    private long gracePeriod;
    private boolean tcpNoDelay;
    private int soLinger;

    public IOReactorConfig() {
        super();
        this.workerCount = 2;
        this.selectInterval = 1000;
        this.interestOpsQueueing = false;
        this.gracePeriod = 5000;
        this.tcpNoDelay = false;
        this.soLinger = 0;
    }

    public int getWorkerCount() {
        return this.workerCount;
    }

    public void setWorkerCount(int workerCount) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count may not be negative or zero");
        }
        this.workerCount = workerCount;
    }

    public long getSelectInterval() {
        return this.selectInterval;
    }

    public void setSelectInterval(long selectInterval) {
        if (selectInterval <= 0) {
            throw new IllegalArgumentException("Select interval may not be negative or zero");
        }
        this.selectInterval = selectInterval;
    }

    public boolean isInterestOpsQueueing() {
        return this.interestOpsQueueing;
    }

    public void setInterestOpsQueueing(boolean interestOpsQueueing) {
        this.interestOpsQueueing = interestOpsQueueing;
    }

    public long getGracePeriod() {
        return this.gracePeriod;
    }

    public void setGracePeriod(long gracePeriod) {
        if (selectInterval <= 0) {
            throw new IllegalArgumentException("Grace period may not be negative");
        }
        this.gracePeriod = gracePeriod;
    }

    public boolean isTcpNoDelay() {
        return this.tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getSoLinger() {
        return this.soLinger;
    }

    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

}
