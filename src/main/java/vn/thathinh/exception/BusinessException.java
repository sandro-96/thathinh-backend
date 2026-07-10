package vn.thathinh.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import vn.thathinh.constant.ApiCode;

@Getter
public class BusinessException extends RuntimeException {
    private final ApiCode apiCode;

    public BusinessException(ApiCode apiCode) {
        super(apiCode.getMessage());
        this.apiCode = apiCode;
    }

    public BusinessException(ApiCode apiCode, String message) {
        super(message);
        this.apiCode = apiCode;
    }

    public HttpStatus getHttpStatus() {
        return switch (apiCode) {
            case UNAUTHORIZED, INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED, USER_BANNED, USER_BLOCKED -> HttpStatus.FORBIDDEN;
            case RATE_LIMIT_MESSAGE, RATE_LIMIT_FLIRT -> HttpStatus.TOO_MANY_REQUESTS;
            case NOT_FOUND, USER_NOT_FOUND, TOPIC_NOT_FOUND, FLIRT_SESSION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_ERROR, BAD_REQUEST, EMAIL_ALREADY_EXISTS, NICKNAME_ALREADY_EXISTS,
                 NICKNAME_INVALID, NICKNAME_BANNED_WORD, MESSAGE_EMPTY, MESSAGE_TOO_LONG, MESSAGE_BANNED_WORD,
                 TOPIC_ALREADY_JOINED, TOPIC_NOT_MEMBER, FLIRT_ALREADY_WAITING, FLIRT_SESSION_ENDED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
