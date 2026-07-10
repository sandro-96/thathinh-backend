package vn.thathinh.validation;

import org.springframework.stereotype.Component;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.exception.BusinessException;

import java.util.Set;

@Component
public class MessageContentValidator {

    private static final int MAX_LENGTH = 2000;

    private static final Set<String> BANNED_SUBSTRINGS = Set.of(
            "dit", "du", "lon", "cac", "buoi", "dmm", "clgt",
            "fuck", "shit", "bitch", "porn", "scam"
    );

    private final NicknameValidator nicknameValidator;

    public MessageContentValidator(NicknameValidator nicknameValidator) {
        this.nicknameValidator = nicknameValidator;
    }

    public String validateAndTrim(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ApiCode.MESSAGE_EMPTY);
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new BusinessException(ApiCode.MESSAGE_TOO_LONG);
        }
        String normalized = nicknameValidator.normalize(trimmed);
        for (String banned : BANNED_SUBSTRINGS) {
            if (normalized.contains(banned)) {
                throw new BusinessException(ApiCode.MESSAGE_BANNED_WORD);
            }
        }
        return trimmed;
    }
}
