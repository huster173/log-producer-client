package org.example.backend;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Prints backend stats dashboard to stdout every second and emits structured metrics log for ELK.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackendStatsReporter {

    private final BackendStats stats;
    private final LogQueue     queue;

    private final long startTime = System.currentTimeMillis();

    @PostConstruct
    void start() {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "stats-reporter");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::report, 1, 1, TimeUnit.SECONDS);
    }

    private void report() {
        long elapsedSec   = (System.currentTimeMillis() - startTime) / 1000;
        long receivedRate = stats.rateReceived();
        long insertedRate = stats.rateInserted();
        long droppedRate  = stats.rateDropped();

        double fillPct = queue.fillRatio() * 100;
        String bar     = fillBar(queue.fillRatio());

        Runtime rt   = Runtime.getRuntime();
        long usedMB  = (rt.totalMemory() - rt.freeMemory()) / 1_048_576;
        long maxMB   = rt.maxMemory() / 1_048_576;

        String dropAlert = stats.getTotalDropped() > 0 ? " [!!!]" : "";

        System.out.printf(
            "%n+----------------------------------------------------------+%n" +
            "| LOG BACKEND  elapsed=%-5ds  JVM mem: %dMB / %dMB        |%n" +
            "+----------------------------------------------------------+%n" +
            "| Received  : %,10d total  | %,7d /s               |%n" +
            "| Inserted  : %,10d total  | %,7d /s               |%n" +
            "| Dropped   : %,10d total  | %,7d /s%-6s          |%n" +
            "| Retries   : %,10d total                               |%n" +
            "| Failed    : %,10d total                               |%n" +
            "| Queue     : %,7d / %,d  %s %.1f%%      |%n" +
            "+----------------------------------------------------------+%n",
            elapsedSec, usedMB, maxMB,
            stats.getTotalReceived(), receivedRate,
            stats.getTotalInserted(), insertedRate,
            stats.getTotalDropped(),  droppedRate, dropAlert,
            stats.getTotalRetries(),
            stats.getTotalFailed(),
            queue.size(), queue.capacity(), bar, fillPct
        );

        // Structured metrics log → picked up by Filebeat → Kibana
        log.info("type=metrics received_tps={} inserted_tps={} dropped_tps={} " +
                 "total_received={} total_inserted={} total_dropped={} total_failed={} " +
                 "queue_size={} queue_capacity={} queue_fill_pct={} jvm_used_mb={} jvm_max_mb={}",
                receivedRate, insertedRate, droppedRate,
                stats.getTotalReceived(), stats.getTotalInserted(),
                stats.getTotalDropped(), stats.getTotalFailed(),
                queue.size(), queue.capacity(), String.format("%.1f", fillPct),
                usedMB, maxMB);
    }

    private String fillBar(double ratio) {
        int filled = (int) Math.min(ratio * 10, 10);
        return "[" + "#".repeat(filled) + "-".repeat(10 - filled) + "]";
    }
}
