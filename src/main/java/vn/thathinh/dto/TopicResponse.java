package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.TopicType;

@Data
@Builder
public class TopicResponse {
    private String id;
    private String name;
    private String description;
    private TopicType type;
    private String slug;
    private String coverImageUrl;
    private int memberCount;
    private boolean joined;
    private int onlineCount;
}
