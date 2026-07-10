package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "topic_messages")
@CompoundIndex(name = "topic_sent", def = "{'topicId': 1, 'sentAt': -1}")
public class TopicMessage {

    @Id
    private String id;

    private String topicId;
    private String senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private String content;
    private Instant sentAt;
}
