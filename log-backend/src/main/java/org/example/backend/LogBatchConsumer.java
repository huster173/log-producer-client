package org.example.backend;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Drains LogQueue in batches and inserts into DB with exponential backoff retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogBatchConsumer {

    private final ConsumerProperties config;
    private final LogQueue           logQueue;
    private final BackendStats       stats;
    private final JdbcTemplate       jdbc;

    private final AtomicInteger threadId = new AtomicInteger();

    @PostConstruct
    void start() {
        ExecutorService pool = Executors.newFixedThreadPool(
                config.getConsumerThreads(),
                r -> {
                    Thread t = new Thread(r, "log-consumer-" + threadId.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
        );

        for (int i = 0; i < config.getConsumerThreads(); i++) {
            pool.submit(this::consumeLoop);
        }

        log.info("Started {} consumer threads, batch size = {}",
                config.getConsumerThreads(), config.getBatchSize());
    }

    private void consumeLoop() {
        List<LogRequest> buffer = new ArrayList<>(config.getBatchSize());
        while (!Thread.currentThread().isInterrupted()) {
            try {
                buffer.clear();
                int drained = logQueue.drainTo(buffer, config.getBatchSize());

                if (drained > 0) {
                    insertWithRetry(buffer);
                } else {
                    Thread.sleep(5);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error in consumer loop", e);
            }
        }
    }

    private void insertWithRetry(List<LogRequest> batch) {
        int attempt = 0;
        while (true) {
            try {
                batchInsert(batch);
                stats.addInserted(batch.size());
                return;
            } catch (Exception e) {
                attempt++;
                stats.incrementRetries();

                if (attempt > config.getMaxRetries()) {
                    stats.addFailed(batch.size());
                    log.error("Batch of {} logs dropped after {} retries: {}",
                            batch.size(), config.getMaxRetries(), e.getMessage());
                    return;
                }

                long backoff = config.getInitialBackoffMs() * (1L << (attempt - 1));
                log.warn("Insert attempt {} failed ({}), retrying in {}ms",
                        attempt, e.getMessage(), backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void batchInsert(List<LogRequest> batch) {
        final long receivedAt = System.currentTimeMillis();
        jdbc.batchUpdate(
                "INSERT INTO log_entries (timestamp, ip, method, path, status, received_at) VALUES (?,?,?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        LogRequest r = batch.get(i);
                        ps.setLong(1, r.getTimestamp());
                        ps.setString(2, r.getIp());
                        ps.setString(3, r.getMethod());
                        ps.setString(4, r.getPath());
                        ps.setInt(5, r.getStatus());
                        ps.setLong(6, receivedAt);
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                }
        );
    }
}
