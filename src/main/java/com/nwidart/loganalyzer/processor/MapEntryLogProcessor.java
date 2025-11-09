package com.nwidart.loganalyzer.processor;

import com.nwidart.loganalyzer.model.LogEntry;
import com.nwidart.loganalyzer.model.Map;
import com.nwidart.loganalyzer.model.MapRepository;
import java.util.regex.Matcher;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MapEntryLogProcessor implements LogProcessor {

  private static final String PATTERN = "PageApplyBase@ _UpdateGameEnd: LastSceneName = World'/Game/Art/Maps/01SD/XZ_YuJinZhiXiBiNanSuo200/XZ_YuJinZhiXiBiNanSuo200.XZ_YuJinZhiXiBiNanSuo200' NextSceneName = World'/Game/Art/Maps";

  private final MapRepository mapRepository;

  public MapEntryLogProcessor(MapRepository mapRepository) {
    this.mapRepository = mapRepository;
  }

  @Override
  public void process(LogEntry logEntry, @Nullable Matcher matcher) {
    var map = Map.newMap();
    this.mapRepository.save(map);
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
