package org.example.backend;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class LogQueue {

    private final LinkedBlockingQueue<LogRequest> queue;
    private final int capacity;

    public LogQueue(ConsumerProperties props) {
        this.capacity = props.getQueueCapacity();
        this.queue    = new LinkedBlockingQueue<>(capacity);
    }

    /** Non-blocking offer. Returns false if queue is full (backpressure). */
    public boolean offer(LogRequest log) {
        return queue.offer(log);
    }

    public int drainTo(List<LogRequest> buffer, int maxElements) {
        return queue.drainTo(buffer, maxElements);
    }

    public int size()          { return queue.size(); }
    public int capacity()      { return capacity; }
    public double fillRatio()  { return (double) queue.size() / capacity; }
}
