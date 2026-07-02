package org.example.backend;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Tells the caller how many entries were actually accepted vs. dropped/rejected, so it can back off. */
@Data
@AllArgsConstructor
public class LogIngestResponse {
    private int accepted;
    private int dropped;
}
