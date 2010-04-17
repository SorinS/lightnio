package com.ok2c.lightnio.testprotocol;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleThreadFactory implements ThreadFactory {

    private final String id;
    private final AtomicInteger count = new AtomicInteger(0);

    public SimpleThreadFactory(String id) {
        super();
        this.id = id;
    }

    public Thread newThread(final Runnable r) {
        return new Thread(r, this.id + " I/O dispatcher " + (this.count.incrementAndGet()));
    }

}
