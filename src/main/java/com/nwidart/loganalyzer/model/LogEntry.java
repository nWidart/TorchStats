package com.nwidart.loganalyzer.model;

import java.time.Instant;

public record LogEntry(
    String rawLine,
    long lineNumber,
    Instant timestamp
) {
}
