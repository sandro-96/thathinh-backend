package vn.thathinh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReactionRequest {
    @NotBlank
    @Size(max = 16)
    private String emoji;
}
