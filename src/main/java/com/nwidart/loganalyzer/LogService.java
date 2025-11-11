package com.nwidart.loganalyzer;

import com.nwidart.loganalyzer.event.LogEntryEvent;
import com.nwidart.loganalyzer.model.LogEntry;
import com.nwidart.loganalyzer.processor.LogProcessor;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class LogService {

  private static final Logger log = LoggerFactory.getLogger(LogService.class);
  private static final long POLL_INTERVAL_MS = 100; // Poll every 100ms
  private static final int BUFFER_SIZE = 8192; // 8KB buffer

  private final ApplicationEventPublisher eventPublisher;
  private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();
  private final List<LogProcessor> processors;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final AtomicLong currentLineNumber = new AtomicLong(0);

  private volatile Tailer tailer;
  @Value("${tlitracker.tail-from-end:true}")
  private boolean tailFromEnd;

  public LogService(ApplicationEventPublisher eventPublisher, List<LogProcessor> processors) {
    this.eventPublisher = eventPublisher;
    this.processors = processors.stream()
        .sorted(Comparator.comparingInt(LogProcessor::getOrder))
        .toList();

    // Pre-compile all patterns
    for (LogProcessor processor : processors) {
      if (processor.getPattern() != null) {
        compiledPatterns.put(
            processor.getPattern(),
            Pattern.compile(processor.getPattern(), Pattern.DOTALL)
        );
      }
    }
  }

  /**
   * Start tailing the specified log file.
   *
   * @param logFilePath path to the log file to tail
   */
  public void startTailing(Path logFilePath) {
    if (tailer != null) {
      log.warn("Already tailing a file. Stop current tailing first.");
      return;
    }

    this.currentLineNumber.set(0);

    TailerListener listener = new TailerListener() {
      @Override
      public void init(Tailer tailer) {
        log.info("Tailer initialized for file: {}", logFilePath);
      }

      @Override
      public void fileNotFound() {
        log.error("Log file not found: {}", logFilePath);
      }

      @Override
      public void fileRotated() {
        log.info("Log file rotated: {}", logFilePath);
        currentLineNumber.set(0);
      }

      @Override
      public void handle(String line) {
        processLine(line);
      }

      @Override
      public void handle(Exception ex) {
        log.error("Error while tailing file: {}", logFilePath, ex);
      }
    };

    // Create and start tailer
    // Parameters: file, listener, delay (ms), fromEnd (start at end), reOpen (handle rotation), bufferSize
    tailer = Tailer.builder()
        .setFile(logFilePath.toFile())
        .setTailerListener(listener)
        .setDelayDuration(java.time.Duration.ofMillis(POLL_INTERVAL_MS))
        .setStartThread(false)
        .setTailFromEnd(this.tailFromEnd)
        .setReOpen(true) // Handle log rotation
        .get();

    executorService.submit(tailer);
    log.info("Started tailing log file: {}", logFilePath);
  }

  /**
   * Stop tailing the current log file.
   */
  public void stopTailing() {
    if (tailer != null) {
      tailer.close();
      tailer = null;
      log.info("Stopped tailing log file");
    }
  }

  /**
   * Process a single log line.
   */
  private void processLine(String line) {
    long lineNum = currentLineNumber.incrementAndGet();
    LogEntry logEntry = new LogEntry(line, lineNum, Instant.now());

    //log.info("Processing log line: {}", line);

    // Publish event for UI update
    eventPublisher.publishEvent(new LogEntryEvent(this, logEntry));

    // Process with registered processors
    for (LogProcessor processor : processors) {
      String pattern = processor.getPattern();

      // Processor with null pattern processes all lines
      if (pattern == null) {
        processor.process(logEntry, null);
        continue;
      }

      // Check if pattern matches
      Pattern compiled = compiledPatterns.get(pattern);
      if (compiled != null) {
        Matcher matcher = compiled.matcher(line);
        if (matcher.find()) {
          processor.process(logEntry, matcher);
        }
      }
    }
  }

  public boolean isRunning() {
    return tailer != null;
  }

  public long getCurrentLineNumber() {
    return currentLineNumber.get();
  }

  @PreDestroy
  public void shutdown() {
    stopTailing();
    executorService.shutdown();
  }
}
