package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "private_messages")
@CompoundIndex(name = "conv_sent", def = "{'conversationId': 1, 'sentAt': -1}")
public class PrivateMessage {

    @Id
    private String id;

    private String conversationId;
    private String senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private String content;
    private String imageUrl;
    private Instant sentAt;

    private String replyToId;
    private String replyToSenderNickname;
    private String replyToPreview;
    private String replyToImageUrl;

    /** emoji -> tập userId đã thả cảm xúc đó. */
    @Builder.Default
    private Map<String, Set<String>> reactions = new HashMap<>();

    @Builder.Default
    private boolean deleted = false;
    private Instant deletedAt;
}
