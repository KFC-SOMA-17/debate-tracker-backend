package com.debatetracker.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    NOT_FOUND_DEBATE_ID(400, "토론 아이디가 존재하지 않습니다"),

    FIELD_ERROR(400, "요청 값이 올바르지 않습니다."),
    URL_PARAMETER_ERROR(400, "요청 파라미터가 올바르지 않습니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(400, "요청 값의 타입이 올바르지 않습니다."),
    NO_COOKIE_FOUND(400, "필수 쿠키가 존재하지 않습니다."),
    FILE_UPLOAD_ERROR(400, "파일 업로드에 실패했습니다."),
    NO_RESOURCE_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_SUPPORTED(405, "지원하지 않는 HTTP 메서드입니다."),
    MEDIA_TYPE_NOT_SUPPORTED(415, "지원하지 않는 미디어 타입입니다."),
    ALREADY_DISCONNECTED(499, "이미 연결이 종료되었습니다."),

    CORS_ORIGIN_EMPTY(500, "CORS Origin 은 적어도 한 개 있어야 합니다"),
    CORS_ORIGIN_STRING_BLANK(500, "CORS Origin 에 빈 값이 들어올 수 없습니다"),
    SERIALIZATION_ERROR(500, "직렬화에 실패했습니다."),
    DESERIALIZATION_ERROR(500, "역직렬화에 실패했습니다."),
    REFINE_RESULT_MISMATCH(500, "보정 결과가 요청과 일치하지 않습니다."),

    // infra-llm
    LLM_REQUEST_SERIALIZATION_FAILED(500, "LLM 요청 직렬화에 실패했습니다."),
    LLM_RESPONSE_PARSING_FAILED(500, "LLM 응답 파싱에 실패했습니다."),
    REFINE_RESPONSE_SEGMENT_SIZE_MISMATCH(500, "정제 응답의 세그먼트 개수가 요청과 일치하지 않습니다."),
    REFINE_RESPONSE_SEGMENT_ID_MISMATCH(500, "정제 응답의 세그먼트 ID 가 요청과 일치하지 않습니다."),
    EXTRACT_RESPONSE_INVALID_FORMAT(500, "쟁점 추출 응답 형식이 올바르지 않습니다."),

    // infra-stt
    STT_CONNECTION_FAILED(500, "STT 연결에 실패했습니다."),
    STT_SESSION_START_INTERRUPTED(500, "STT 세션 시작이 중단되었습니다."),

    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다. 관리자에게 문의하세요."),
    ;

    private final int statusCode;
    private final String message;

    public boolean is5XxError() {
        return statusCode >= 500 && statusCode <= 599;
    }
}
