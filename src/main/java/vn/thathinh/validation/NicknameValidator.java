package vn.thathinh.validation;

import org.springframework.stereotype.Component;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.exception.BusinessException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class NicknameValidator {

    private static final Pattern ALLOWED = Pattern.compile("^[a-zA-Z0-9_\\u00C0-\\u1EF9]{3,20}$");
    private static final Pattern ALL_DIGITS = Pattern.compile("^\\d+$");

    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "mod", "moderator", "system", "support",
            "thathinh", "root", "null", "undefined", "helpdesk", "official"
    );

    private static final Set<String> BANNED_SUBSTRINGS = Set.of(
            "admin", "dit", "du", "lon", "cac", "buoi", "dmm", "clgt", "vl",
            "fuck", "shit", "bitch", "sex", "porn", "nude", "scam", "lua dao"
    );

    public void validate(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ApiCode.NICKNAME_INVALID);
        }
        String trimmed = nickname.trim();
        if (!ALLOWED.matcher(trimmed).matches()) {
            throw new BusinessException(ApiCode.NICKNAME_INVALID);
        }
        if (ALL_DIGITS.matcher(trimmed).matches()) {
            throw new BusinessException(ApiCode.NICKNAME_INVALID);
        }
        String normalized = normalize(trimmed);
        if (RESERVED.contains(normalized)) {
            throw new BusinessException(ApiCode.NICKNAME_BANNED_WORD);
        }
        for (String banned : BANNED_SUBSTRINGS) {
            if (normalized.contains(banned)) {
                throw new BusinessException(ApiCode.NICKNAME_BANNED_WORD);
            }
        }
    }

    public String normalize(String input) {
        if (input == null) return "";
        String n = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return n.replaceAll("[^a-z0-9]", "");
    }
}
