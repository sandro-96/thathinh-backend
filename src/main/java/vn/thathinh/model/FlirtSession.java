package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import vn.thathinh.constant.FlirtStatus;
import vn.thathinh.model.base.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "flirt_sessions")
@CompoundIndexes({
        @CompoundIndex(name = "userA_status", def = "{'userAId': 1, 'status': 1}"),
        @CompoundIndex(name = "userB_status", def = "{'userBId': 1, 'status': 1}"),
        @CompoundIndex(name = "userA_matchedAt", def = "{'userAId': 1, 'matchedAt': -1}"),
        @CompoundIndex(name = "userB_matchedAt", def = "{'userBId': 1, 'matchedAt': -1}")
})
public class FlirtSession extends BaseEntity {

    @Id
    private String id;

    private String userAId;
    private String userBId;

    @Builder.Default
    private FlirtStatus status = FlirtStatus.WAITING;

    private DatingPreferences userAPrefs;
    private DatingPreferences userBPrefs;

    private Instant matchedAt;
    private Instant endedAt;
    private String endedBy;
    private String reportReason;
}
