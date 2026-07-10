package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import vn.thathinh.model.base.BaseEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_blocks")
@CompoundIndex(name = "block_pair", def = "{'blockerId': 1, 'blockedId': 1}", unique = true)
public class UserBlock extends BaseEntity {

    @Id
    private String id;

    private String blockerId;
    private String blockedId;
}
