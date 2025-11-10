package com.nwidart.fulltable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FullTableServiceTest {

    @Autowired
    private FullTableService service;

    @Test
    void loads_full_table_on_startup() {
        assertThat(service.size()).isGreaterThan(0);
        assertThat(service.findById("10001")).isPresent();
        assertThat(service.findById("10001").get().name()).isEqualTo("Sin's Plundering Compass");
    }
}
