package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class MessageResponse {
    private String id;
    private String senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private String content;
    private String imageUrl;
    private Instant sentAt;
    private boolean mine;

    private String replyToId;
    private String replyToSenderId;
    private String replyToSenderNickname;
    private String replyToPreview;
    private String replyToImageUrl;

    /** emoji -> danh sách userId đã thả cảm xúc. */
    private Map<String, List<String>> reactions;

    private boolean deleted;
}
