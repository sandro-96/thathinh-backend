package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.FlirtStatus;

import java.time.Instant;

@Data
@Builder
public class FlirtSessionHistoryResponse {
    private String sessionId;
    private String partnerId;
    private String partnerNickname;
    private String partnerAvatarUrl;
    private FlirtStatus status;
    private Instant matchedAt;
    private Instant endedAt;
}
