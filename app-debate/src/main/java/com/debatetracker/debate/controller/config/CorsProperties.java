package com.debatetracker.debate.controller.config;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private final String[] originUrls;

    public CorsProperties(String[] originUrls) {
        validate(originUrls);
        this.originUrls = originUrls;
    }

    private void validate(String[] corsOrigin) {
        if (corsOrigin == null || corsOrigin.length == 0) {
            throw new DebateTrackerException(ErrorCode.CORS_ORIGIN_EMPTY);
        }
        for (String origin : corsOrigin) {
            if (origin == null || origin.isBlank()) {
                throw new DebateTrackerException(ErrorCode.CORS_ORIGIN_STRING_BLANK);
            }
        }
    }
}
