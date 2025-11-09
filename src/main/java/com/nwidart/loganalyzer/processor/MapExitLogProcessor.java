package com.nwidart.loganalyzer.processor;

import com.nwidart.loganalyzer.model.LogEntry;
import com.nwidart.loganalyzer.model.Map;
import com.nwidart.loganalyzer.model.MapRepository;
import java.util.regex.Matcher;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MapExitLogProcessor implements LogProcessor {

  private static final String PATTERN = "NextSceneName = World'/Game/Art/Maps/01SD/XZ_YuJinZhiXiBiNanSuo200/XZ_YuJinZhiXiBiNanSuo200.XZ_YuJinZhiXiBiNanSuo200'";

  private final MapRepository mapRepository;

  public MapExitLogProcessor(MapRepository mapRepository) {
    this.mapRepository = mapRepository;
  }

  @Override
  public void process(LogEntry logEntry, @Nullable Matcher matcher) {
    Map activeMap = this.mapRepository.findActiveMap();
    if (activeMap != null) {
      activeMap.endMap();
      this.mapRepository.save(activeMap);
    }
  }

  @Override
  public String getPattern() {
    return PATTERN;
  }

  @Override
  public int getOrder() {
    return 15;
  }
}
