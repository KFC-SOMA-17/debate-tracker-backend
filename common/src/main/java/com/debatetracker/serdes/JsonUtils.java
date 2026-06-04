package com.debatetracker.serdes;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String serialize(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: type={}", value == null ? null : value.getClass().getName(), e);
            throw new DebateTrackerException(ErrorCode.SERIALIZATION_ERROR, e);
        }
    }

    public static <T> T deserialize(String value, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(value, type);
        } catch (JsonProcessingException e) {
            log.error("JSON 역직렬화 실패: type={}, value={}", type.getName(), value, e);
            throw new DebateTrackerException(ErrorCode.DESERIALIZATION_ERROR, e);
        }
    }
}
