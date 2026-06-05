package com.debatetracker.debate.infrastructure.config;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    @PostConstruct
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_ZONE));
    }
}
