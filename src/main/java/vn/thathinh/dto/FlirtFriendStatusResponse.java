package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.FriendshipStatus;

@Data
@Builder
public class FlirtFriendStatusResponse {
    private FriendshipStatus status;
    private String friendshipId;
    private String conversationId;
    private String partnerId;
    private boolean requestedByMe;
    private String incomingRequestId;
    private boolean flirtHistoryImported;
}
