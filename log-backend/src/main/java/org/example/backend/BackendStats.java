package org.example.backend;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class BackendStats {

    private final AtomicLong totalReceived = new AtomicLong();
    private final AtomicLong totalDropped  = new AtomicLong();
    private final AtomicLong totalInserted = new AtomicLong();
    private final AtomicLong totalRetries  = new AtomicLong();
    private final AtomicLong totalFailed   = new AtomicLong();

    private final AtomicLong snapReceived  = new AtomicLong();
    private final AtomicLong snapInserted  = new AtomicLong();
    private final AtomicLong snapDropped   = new AtomicLong();

    public void addReceived(long n)    { totalReceived.addAndGet(n); }
    public void addDropped(long n)     { totalDropped.addAndGet(n); }
    public void addInserted(long n)    { totalInserted.addAndGet(n); }
    public void incrementRetries()     { totalRetries.incrementAndGet(); }
    public void addFailed(long n)      { totalFailed.addAndGet(n); }

    public long getTotalReceived()     { return totalReceived.get(); }
    public long getTotalDropped()      { return totalDropped.get(); }
    public long getTotalInserted()     { return totalInserted.get(); }
    public long getTotalRetries()      { return totalRetries.get(); }
    public long getTotalFailed()       { return totalFailed.get(); }

    public long rateReceived() {
        long cur = totalReceived.get();
        return cur - snapReceived.getAndSet(cur);
    }

    public long rateInserted() {
        long cur = totalInserted.get();
        return cur - snapInserted.getAndSet(cur);
    }

    public long rateDropped() {
        long cur = totalDropped.get();
        return cur - snapDropped.getAndSet(cur);
    }
}
