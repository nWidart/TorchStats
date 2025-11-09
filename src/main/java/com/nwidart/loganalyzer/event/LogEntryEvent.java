package com.nwidart.loganalyzer.event;

import com.nwidart.loganalyzer.model.LogEntry;
import org.springframework.context.ApplicationEvent;

public class LogEntryEvent extends ApplicationEvent {

    private final LogEntry logEntry;

    public LogEntryEvent(Object source, LogEntry logEntry) {
        super(source);
        this.logEntry = logEntry;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }
}
