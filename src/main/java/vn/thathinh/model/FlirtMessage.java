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
@Document(collection = "flirt_messages")
@CompoundIndex(name = "session_sent", def = "{'sessionId': 1, 'sentAt': -1}")
public class FlirtMessage {

    @Id
    private String id;

    private String sessionId;
    private String senderId;
    private String content;
    private String imageUrl;
    private Instant sentAt;

    private String replyToId;
    private String replyToSenderId;
    private String replyToPreview;
    private String replyToImageUrl;

    @Builder.Default
    private Map<String, Set<String>> reactions = new HashMap<>();

    @Builder.Default
    private boolean deleted = false;
    private Instant deletedAt;
}
