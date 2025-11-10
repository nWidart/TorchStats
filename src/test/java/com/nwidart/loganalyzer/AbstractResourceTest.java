package com.nwidart.loganalyzer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;

abstract public class AbstractResourceTest {

  protected Path resourcePath(String classpathLocation) throws IOException {
    ClassPathResource res = new ClassPathResource(classpathLocation);
    if (!res.exists()) {
      throw new IllegalArgumentException("Test resource not found: " + classpathLocation);
    }
    try {
      URL url = res.getURL();
      return Path.of(url.toURI());
    } catch (URISyntaxException e) {
      throw new IOException("Could not resolve resource URI: " + classpathLocation, e);
    }
  }

  protected long countLines(Path file) throws IOException {
    try (var lines = Files.lines(file)) {
      return lines.count();
    }
  }

  protected void awaitUntil(BooleanSupplier condition, Duration timeout) throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      sleep(25);
    }
    // one last check
    if (!condition.getAsBoolean()) {
      throw new AssertionError("Condition not met within timeout " + timeout);
    }
  }

  @FunctionalInterface
  protected interface BooleanSupplier {
    boolean getAsBoolean();
  }

  protected void sleep(long millis) throws InterruptedException {
    TimeUnit.MILLISECONDS.sleep(millis);
  }
}
