package vn.thathinh.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.thathinh.constant.Gender;
import vn.thathinh.model.DatingPreferences;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private Gender gender;
    private LocalDate birthDate;
    private DatingPreferences preferences;

    @Size(max = 300)
    private String bio;

    private List<@Size(max = 30) String> interests;
}
