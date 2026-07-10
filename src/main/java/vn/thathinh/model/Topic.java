package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import vn.thathinh.constant.TopicType;
import vn.thathinh.model.base.BaseEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "topics")
public class Topic extends BaseEntity {

    @Id
    private String id;

    private String name;
    private String description;

    @Builder.Default
    private TopicType type = TopicType.CUSTOM;

    @Indexed(unique = true)
    private String slug;

    private String coverImageUrl;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int memberCount = 0;
}
