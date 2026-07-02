package org.example.backend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {
    private long timestamp;
    private String ip;
    private String method;
    private String path;
    private int status;
}
