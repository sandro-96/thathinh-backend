package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.Gender;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ConversationResponse {
    private String id;
    private String partnerId;
    private String partnerNickname;
    private String partnerAvatarUrl;
    private Integer partnerAge;
    private Gender partnerGender;
    private String partnerBio;
    private List<String> partnerInterests;
    private List<String> partnerPhotos;
    private String lastMessagePreview;
    private Instant lastMessageAt;
    private String friendshipStatus;
    private boolean flirtHistoryImported;
    private boolean blockedByMe;
    private boolean blockedByPartner;
    private boolean unread;
    private int unreadCount;
    private boolean partnerOnline;
    private Instant partnerLastSeenAt;
    private Instant partnerLastReadAt;
    private Instant myLastReadAt;
    private boolean muted;
}
