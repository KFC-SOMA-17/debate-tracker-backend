package com.debatetracker.debate.controller.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.debatetracker.exception.DebateTrackerException;
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
                    .isInstanceOf(DebateTrackerException.class)
                    .hasMessage("CORS Origin 은 적어도 한 개 있어야 합니다");
        }

        @Test
        void origin_배열이_비어있으면_예외를_던진다() {
            String[] originUrls = {};

            assertThatThrownBy(() -> new CorsProperties(originUrls))
                    .isInstanceOf(DebateTrackerException.class)
                    .hasMessage("CORS Origin 은 적어도 한 개 있어야 합니다");
        }

        @NullSource
        @ValueSource(strings = {"", " ", "\t"})
        @ParameterizedTest
        void origin_에_null_이거나_공백인_원소가_있으면_예외를_던진다(String blankOrigin) {
            String[] originUrls = {"https://example.com", blankOrigin};

            assertThatThrownBy(() -> new CorsProperties(originUrls))
                    .isInstanceOf(DebateTrackerException.class)
                    .hasMessage("CORS Origin 에 빈 값이 들어올 수 없습니다");
        }
    }
}
