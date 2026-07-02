package org.example.backend;

import lombok.RequiredArgsConstructor;
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

    private final LogQueue     logQueue;
    private final BackendStats stats;

    @PostMapping
    public ResponseEntity<Void> receiveLogs(@RequestBody List<LogRequest> batch) {
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

        return ResponseEntity.accepted().build();
    }
}
