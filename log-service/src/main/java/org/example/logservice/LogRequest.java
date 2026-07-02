package org.example.logservice;

import jdk.jfr.Timestamp;

public class Event {
    private Timestamp timestamp;
    private String ip;
    private String method;
    private String path;
    private int status;
}
