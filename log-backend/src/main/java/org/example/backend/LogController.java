package org.example.backend;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoint that accepts batches of log entries from the producer client.
 * Enqueues each log into LogQueue for async batch-insert processing.
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogQueue logQueue;
    private final BackendStats stats;
    private final ConsumerProperties config;

    @PostMapping
    public ResponseEntity<LogIngestResponse> receiveLogs(@RequestBody List<LogRequest> batch) {
        // Reject oversized requests outright instead of burning CPU enqueuing them
        // one-by-one only to have most of them dropped by the queue anyway.
        if (batch.size() > config.getMaxBatchSize()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new LogIngestResponse(0, batch.size()));
        }

        int dropped = 0;
        for (LogRequest log : batch) {
            if (!logQueue.offer(log)) {
                dropped++;
            }
        }

        stats.addReceived(batch.size());
        if (dropped > 0) {
            stats.addDropped(dropped);
        }

        LogIngestResponse body = new LogIngestResponse(batch.size() - dropped, dropped);

        // Signal backpressure instead of always claiming success — callers should
        // treat a non-2xx here as "slow down / retry later", not a fire-and-forget ack.
        if (dropped > 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        return ResponseEntity.accepted().body(body);
    }
}
