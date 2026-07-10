package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.FlirtStatus;
import vn.thathinh.constant.Gender;
import vn.thathinh.model.DatingPreferences;

import java.util.List;

@Data
@Builder
public class FlirtStatusResponse {
    private FlirtStatus status;
    private String sessionId;
    private String partnerNickname;
    private String partnerAvatarUrl;
    private Integer partnerAge;
    private Gender partnerGender;
    private String partnerBio;
    private List<String> partnerInterests;
    private List<String> partnerPhotos;
    private DatingPreferences preferences;
    /** Seconds left in the match queue; set when status is WAITING. */
    private Integer waitingSecondsRemaining;
}
