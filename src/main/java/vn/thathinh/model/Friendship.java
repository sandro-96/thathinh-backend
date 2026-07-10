package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import vn.thathinh.constant.FriendshipStatus;
import vn.thathinh.model.base.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "friendships")
@CompoundIndex(name = "user_pair", def = "{'userLowId': 1, 'userHighId': 1}", unique = true)
public class Friendship extends BaseEntity {

    @Id
    private String id;

    private String userLowId;
    private String userHighId;

    @Builder.Default
    private FriendshipStatus status = FriendshipStatus.PENDING;

    private String requestedBy;
    private String sourceSessionId;
    private Instant acceptedAt;
}
