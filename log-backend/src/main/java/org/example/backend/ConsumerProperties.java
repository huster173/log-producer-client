package org.example.backend;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "log.consumer")
public class ConsumerProperties {
    private int queueCapacity   = 500_000;
    private int batchSize       = 500;
    private int consumerThreads = 4;
    private int maxRetries      = 3;
    private long initialBackoffMs = 100;

    /** Max entries accepted per POST /api/logs request; larger requests are rejected with 413. */
    private int maxBatchSize = 5_000;
}
