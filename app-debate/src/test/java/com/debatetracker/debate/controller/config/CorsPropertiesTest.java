package com.debatetracker.debate.controller.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CorsPropertiesTest {

    @Nested
    class Create {

        @Test
        void origin_배열이_null_이면_예외를_던진다() {
            assertThatThrownBy(() -> new CorsProperties(null))
                    .asInstanceOf(type(DebateTrackerException.class))
                    .extracting(DebateTrackerException::getErrorCode)
                    .isEqualTo(ErrorCode.CORS_ORIGIN_EMPTY);
        }

        @Test
        void origin_배열이_비어있으면_예외를_던진다() {
            String[] originUrls = {};

            assertThatThrownBy(() -> new CorsProperties(originUrls))
                    .asInstanceOf(type(DebateTrackerException.class))
                    .extracting(DebateTrackerException::getErrorCode)
                    .isEqualTo(ErrorCode.CORS_ORIGIN_EMPTY);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", " ", "\t"})
        void origin_에_null_이거나_공백인_원소가_있으면_예외를_던진다(String blankOrigin) {
            String[] originUrls = {"https://example.com", blankOrigin};

            assertThatThrownBy(() -> new CorsProperties(originUrls))
                    .asInstanceOf(type(DebateTrackerException.class))
                    .extracting(DebateTrackerException::getErrorCode)
                    .isEqualTo(ErrorCode.CORS_ORIGIN_STRING_BLANK);
        }
    }
}
