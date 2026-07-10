package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import vn.thathinh.model.base.BaseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
@CompoundIndex(name = "user_pair", def = "{'userLowId': 1, 'userHighId': 1}", unique = true)
public class Conversation extends BaseEntity {

    @Id
    private String id;

    private String userLowId;
    private String userHighId;
    private String sourceSessionId;

    private String lastMessagePreview;
    private Instant lastMessageAt;
    private String lastSenderId;

    @Builder.Default
    private Map<String, Instant> lastReadAtByUser = new HashMap<>();

    /** Tập userId đã tắt thông báo cho hội thoại này. */
    @Builder.Default
    private Set<String> mutedUserIds = new HashSet<>();

    @Builder.Default
    private boolean flirtHistoryImported = false;
}
