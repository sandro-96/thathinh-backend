package vn.thathinh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AvatarPresetRequest {
    @NotBlank
    private String seed;
}
