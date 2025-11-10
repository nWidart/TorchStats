package com.nwidart.fulltable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FullTableItem(
    @Nullable @JsonProperty("from") String from,
    @Nullable @JsonProperty("last_time") Long lastTime,
    @JsonProperty("last_update") long lastUpdate,
    @JsonProperty("name") String name,
    @JsonProperty("price") double price,
    @JsonProperty("type") String type
) {

}
