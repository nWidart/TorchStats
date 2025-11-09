package com.nwidart.loganalyzer.processor;

import com.nwidart.loganalyzer.model.LogEntry;
import java.util.regex.Matcher;
import org.jspecify.annotations.Nullable;

/**
 * Interface for processing log entries with regex pattern matching.
 * Implementations define business logic for handling matched log lines.
 */
public interface LogProcessor {

    /**
     * Process a single log entry.
     *
     * @param logEntry the log entry to process
     * @param matcher
     */
    void process(LogEntry logEntry, @Nullable Matcher matcher);

    /**
     * Get the regex pattern this processor handles.
     * Pattern will be compiled and cached for performance.
     *
     * @return the regex pattern string, or null to process all lines
     */
    String getPattern();

    /**
     * Get the priority order for this processor (lower = higher priority).
     * Used when multiple processors match the same line.
     *
     * @return the priority order (default 100)
     */
    default int getOrder() {
        return 100;
    }
}
