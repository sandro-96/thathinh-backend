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
@Document(collection = "topic_memberships")
@CompoundIndex(name = "topic_user_unique", def = "{'topicId': 1, 'userId': 1}", unique = true)
public class TopicMembership {

    @Id
    private String id;

    private String topicId;
    private String userId;
    private Instant joinedAt;
}
