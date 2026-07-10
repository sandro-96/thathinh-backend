package vn.thathinh.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.thathinh.constant.Gender;
import vn.thathinh.model.DatingPreferences;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileRequest {
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_\\u00C0-\\u1EF9]+$")
    private String nickname;
    private Gender gender;
    private LocalDate birthDate;
    private DatingPreferences preferences;

    @Size(max = 300)
    private String bio;

    private List<@Size(max = 30) String> interests;
}
