package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BlockedUserResponse {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private Instant blockedAt;
}
