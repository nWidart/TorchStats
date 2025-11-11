package com.nwidart.loganalyzer.ui;

import com.nwidart.fulltable.FullTableService;
import com.nwidart.loganalyzer.LogService;
import com.nwidart.loganalyzer.StatsService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route("log-file")
@PageTitle("Log File")
public class LogFileView extends Main {

  private static final Logger log = LoggerFactory.getLogger(LogFileView.class);
  private static final int MAX_DISPLAYED_LINES = 1000; // Limit UI lines for performance

  private final LogService logService;
  private final StatsService statsService;
  private final FullTableService fullTableService;
  private final LinkedBlockingDeque<Span> logLines = new LinkedBlockingDeque<>(MAX_DISPLAYED_LINES);

  private final TextField logFileField;
  private final Button startButton;
  private final Button stopButton;
  private final Span statusLabel;

  private Registration listenerRegistration;

  // --- Statistics UI state ---
  private final Span sessionStatus = new Span("-");
  private final Span sessionDuration = new Span("-");
  private final Span revenuePerSession = new Span("-");
  private final Span mapStatus = new Span("-");
  private final Span mapCount = new Span("-");
  private final Span mapDuration = new Span("-");
  private final Span revenuePerMap = new Span("-");
  private final Span avgRevenuePerMap = new Span("-");
  private final Span avgRevenuePerHour = new Span("-");

  private final AtomicLong sessionStartMillis = new AtomicLong(0);
  private final AtomicLong mapStartMillis = new AtomicLong(0);

  public LogFileView(LogService logService, StatsService statsService, FullTableService fullTableService) {
    this.logService = logService;
    this.statsService = statsService;
    this.fullTableService = fullTableService;

    // Header
    var header = new Paragraph("Log File Analyzer - Real-time Tailing");
    header.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

    // File selection
    logFileField = new TextField("Log file path");
    logFileField.setPlaceholder("/Users/nicolaswidart/Sites/explorations/GoTorch/UE_game.log");
    logFileField.setValue("/Users/nicolaswidart/Sites/explorations/GoTorch/UE_game.log");
    logFileField.setWidthFull();

    // Buttons
    startButton = new Button("Start Tailing", event -> startTailing());
    startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    stopButton = new Button("Stop", event -> stopTailing());
    stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    stopButton.setEnabled(false);

    var buttonLayout = new HorizontalLayout(startButton, stopButton);
    buttonLayout.setSpacing(true);

    // Status
    statusLabel = new Span("Ready");
    statusLabel.addClassNames(LumoUtility.FontWeight.BOLD);

    // Statistics card
    var statsCard = createStatisticsCard();

    // Layout
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM
    );

    add(header, logFileField, buttonLayout, statusLabel, statsCard);
  }

  // --- Statistics UI creation ---
  private Div createStatisticsCard() {
    var container = new Div();
    container.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.Background.BASE
    );

    var title = new H3("Statistics");
    title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.LARGE);

    var grid = new FlexLayout(
        statItem("Session status", VaadinIcon.PLAY, sessionStatus),
        statItem("Session duration", VaadinIcon.TIME_BACKWARD, sessionDuration),
        statItem("Revenue per session", VaadinIcon.MONEY, revenuePerSession),
        statItem("Map Status", VaadinIcon.MAP_MARKER, mapStatus),
        statItem("Map Count", VaadinIcon.MAP_MARKER, mapCount),
        statItem("Map Duration", VaadinIcon.TIME_FORWARD, mapDuration),
        statItem("Revenue for current map", VaadinIcon.COIN_PILES, revenuePerMap),
        statItem("Avg revenue per map", VaadinIcon.TRENDING_UP, avgRevenuePerMap),
        statItem("Avg revenue per hour", VaadinIcon.LINE_BAR_CHART, avgRevenuePerHour)
    );

    grid.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    grid.setFlexWrap(FlexWrap.WRAP);
    grid.setAlignItems(FlexComponent.Alignment.STRETCH);
    grid.getStyle().set("gap", "0.75rem");

    container.add(title, grid);
    return container;
  }

  private Div statItem(String label, VaadinIcon icon, Span value) {
    var card = new Div();
    card.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.Background.CONTRAST_5
    );
    card.getStyle().set("min-width", "220px");
    card.getStyle().set("flex", "1 1 220px");

    var header = new HorizontalLayout();
    header.setWidthFull();
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    var ic = new Icon(icon);
    ic.addClassNames(LumoUtility.TextColor.PRIMARY);
    var lbl = new Span(label);
    lbl.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
    header.add(ic, lbl);
    header.setSpacing(true);

    value.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD);

    var content = new VerticalLayout(value);
    content.setPadding(false);
    content.setSpacing(false);

    card.add(header, content);
    return card;
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    if (logService.isRunning()) {
      logService.stopTailing();
    }
  }

  private void startTailing() {
    String filePath = logFileField.getValue();
    if (filePath == null || filePath.isBlank()) {
      statusLabel.setText("Error: Please specify a log file path");
      return;
    }

    try {
      Path path = Paths.get(filePath);
      logService.startTailing(path);

      startButton.setEnabled(false);
      stopButton.setEnabled(true);
      logFileField.setEnabled(false);
      logLines.clear();

      statusLabel.setText("Tailing: " + filePath);
      log.info("Started tailing: {}", filePath);

      // --- Initialize statistics for a new session ---
      sessionStartMillis.set(System.currentTimeMillis());
      mapStartMillis.set(System.currentTimeMillis());
      sessionStatus.setText("Running");
      mapStatus.setText("Active");
      // Placeholder initial values
      sessionDuration.setText("0m 00s");
      mapDuration.setText("0m 00s");
      revenuePerSession.setText("0");
      revenuePerMap.setText("0");
      avgRevenuePerMap.setText("0");
      avgRevenuePerHour.setText("0");
      mapCount.setText("0");

      // Start periodic UI updates (example polling using UI access)
      getUI().ifPresent(ui -> {
        ui.setPollInterval(1000);
        ui.addPollListener(e -> updateStats());
      });
    } catch (Exception e) {
      statusLabel.setText("Error: " + e.getMessage());
      log.error("Failed to start tailing", e);
    }
  }

  private void stopTailing() {
    logService.stopTailing();

    startButton.setEnabled(true);
    stopButton.setEnabled(false);
    logFileField.setEnabled(true);

    statusLabel.setText("Stopped");
    log.info("Stopped tailing");

    // --- Update statistics status ---
    sessionStatus.setText("Stopped");
    mapStatus.setText("Paused");
    getUI().ifPresent(ui -> ui.setPollInterval(-1)); // disable polling
  }

  // --- Example periodic stats update hook ---
  private void updateStats() {
    // Replace these placeholders with your real data source and calculations.
    long now = System.currentTimeMillis();

    if (sessionStartMillis.get() > 0) {
      sessionDuration.setText(formatDuration(Duration.ofMillis(now - sessionStartMillis.get())));
    }

    mapDuration.setText(formatDuration(statsService.timeInActiveMap()));

    Float sessionRevenue = statsService.getSessionRevenue();
    Float currentMapRevenue = statsService.currentMapRevenue();
    Float mapsCompleted = statsService.mapsCompleted();
    double avgPerMap = sessionRevenue / mapsCompleted;
    double hours = Math.max(0.01, (now - sessionStartMillis.get()) / 3_600_000.0);
    double avgPerHour = sessionRevenue / hours;

    revenuePerSession.setText(formatMoney(sessionRevenue));
    revenuePerMap.setText(formatMoney(currentMapRevenue));
    avgRevenuePerMap.setText(formatMoney(avgPerMap));
    avgRevenuePerHour.setText(formatMoney(avgPerHour));
    mapCount.setText(formatMoney(mapsCompleted));
  }

  private String formatDuration(Duration d) {
    long h = d.toHours();
    long m = d.toMinutesPart();
    long s = d.toSecondsPart();
    if (h > 0) {
      return String.format("%dh %02dm %02ds", h, m, s);
    }
    return String.format("%dm %02ds", m, s);
  }

  private String formatMoney(double value) {
    // Adjust formatting/currency as needed
    return String.format("%.0f", value);
  }

  private double fakeValue(long now, double base) {
    // Simple oscillation to visualize updates; replace with real data
    return base + 50.0 * Math.sin(now / 5000.0);
  }
}
