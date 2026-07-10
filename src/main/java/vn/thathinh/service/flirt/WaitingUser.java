package vn.thathinh.service.flirt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import vn.thathinh.constant.Gender;
import vn.thathinh.model.DatingPreferences;

import java.time.Instant;

/**
 * Một user đang chờ ghép đôi. Serialize được sang JSON để lưu trong Redis.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WaitingUser(
        @JsonProperty("userId") String userId,
        @JsonProperty("gender") Gender gender,
        @JsonProperty("age") int age,
        @JsonProperty("prefs") DatingPreferences prefs,
        @JsonProperty("since") Instant since) {

    @JsonCreator
    public WaitingUser {
    }

    public boolean wantsGenderOf(WaitingUser other) {
        return prefs.wantsGender(other.gender());
    }
}
