package vn.thathinh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.thathinh.constant.TopicType;

@Data
public class CreateTopicRequest {
    @NotBlank @Size(max = 100)
    private String name;
    @Size(max = 500)
    private String description;
    private TopicType type;
    private String coverImageUrl;
}
