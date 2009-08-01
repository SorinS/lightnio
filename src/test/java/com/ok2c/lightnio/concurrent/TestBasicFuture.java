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
package com.ok2c.lightnio.concurrent;

import java.nio.channels.IllegalSelectorException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link BasicFuture}.
 */
public class TestBasicFuture {

    static class FutureResult<T> implements FutureCallback<T> {

        private volatile boolean completed;
        private volatile boolean failed;
        private volatile boolean cancelled;

        public void completed(final Future<T> future) {
            if (this.completed) {
                return;
            }
            this.completed = true;
        }

        public void cancelled(final Future<T> future) {
            if (this.completed) {
                return;
            }
            this.completed = true;
            this.cancelled = true;
        }

        public void failed(final Future<T> future) {
            if (this.completed) {
                return;
            }
            this.completed = true;
            this.failed = true;
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public boolean isFailed() {
            return this.failed;
        }

        public boolean isCancelled() {
            return this.cancelled;
        }

    }

    @Test
    public void testFutureCompleted() throws Exception {
        FutureResult<String> result = new FutureResult<String>();
        BasicFuture<String> future = new BasicFuture<String>(result);
        future.completed("done");
        Assert.assertTrue(result.isCompleted());
        Assert.assertFalse(result.isCancelled());
        Assert.assertFalse(result.isFailed());
        String s = future.get();
        Assert.assertEquals("done", s);
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());
    }

    @Test
    public void testFutureCancelled() throws Exception {
        FutureResult<String> result = new FutureResult<String>();
        BasicFuture<String> future = new BasicFuture<String>(result);
        future.cancel(true);
        Assert.assertTrue(result.isCompleted());
        Assert.assertTrue(result.isCancelled());
        Assert.assertFalse(result.isFailed());
        String s = future.get();
        Assert.assertNull(s);
        Assert.assertTrue(future.isDone());
        Assert.assertTrue(future.isCancelled());
    }

    @Test(expected=ExecutionException.class)
    public void testFutureFailed() throws Exception {
        FutureResult<String> result = new FutureResult<String>();
        BasicFuture<String> future = new BasicFuture<String>(result);
        future.failed(new IllegalStateException());
        Assert.assertTrue(result.isCompleted());
        Assert.assertFalse(result.isCancelled());
        Assert.assertTrue(result.isFailed());
        String s = future.get();
        Assert.assertNull(s);
    }

    @Test
    public void testFutureCompletedThreaded() throws Exception {
        final FutureResult<String> result = new FutureResult<String>();
        final BasicFuture<String> future = new BasicFuture<String>(result);

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                    future.completed("done");
                } catch (InterruptedException ex) {
                }
            }

        };
        t.start();
        String s = future.get();
        t.join();
        Assert.assertEquals("done", s);
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());
        Assert.assertTrue(result.isCompleted());
        Assert.assertFalse(result.isCancelled());
        Assert.assertFalse(result.isFailed());
    }

    @Test
    public void testFutureCancelledThreaded() throws Exception {
        final FutureResult<String> result = new FutureResult<String>();
        final BasicFuture<String> future = new BasicFuture<String>(result);

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                    future.cancel(true);
                } catch (InterruptedException ex) {
                }
            }

        };
        t.start();
        String s = future.get();
        t.join();
        Assert.assertTrue(result.isCompleted());
        Assert.assertTrue(result.isCancelled());
        Assert.assertFalse(result.isFailed());
        Assert.assertNull(s);
        Assert.assertTrue(future.isDone());
        Assert.assertTrue(future.isCancelled());
    }

    @Test(expected=ExecutionException.class)
    public void testFutureFailedThreaded() throws Exception {
        final FutureResult<String> result = new FutureResult<String>();
        final BasicFuture<String> future = new BasicFuture<String>(result);

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                    future.failed(new IllegalSelectorException());
                } catch (InterruptedException ex) {
                }
            }

        };
        t.start();
        future.get();
    }

    @Test
    public void testFutureTimeout() throws Exception {
        final FutureResult<String> result = new FutureResult<String>();
        final BasicFuture<String> future = new BasicFuture<String>(result);

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    future.completed("done");
                } catch (InterruptedException ex) {
                }
            }

        };
        t.start();
        try {
            future.get(250, TimeUnit.MILLISECONDS);
        } catch (TimeoutException expected) {
        }
        try {
            future.get(250, TimeUnit.MILLISECONDS);
        } catch (TimeoutException expected) {
        }
        String s = future.get();
        Assert.assertEquals("done", s);
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());
        Assert.assertTrue(result.isCompleted());
        Assert.assertFalse(result.isCancelled());
        Assert.assertFalse(result.isFailed());
    }

}
