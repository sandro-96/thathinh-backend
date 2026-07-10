package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.FriendshipStatus;

import java.time.Instant;

@Data
@Builder
public class FriendRequestResponse {
    private String id;
    private String requesterId;
    private String requesterNickname;
    private String requesterAvatarUrl;
    private FriendshipStatus status;
    private String sourceSessionId;
    private Instant createdAt;
    private String conversationId;
}
