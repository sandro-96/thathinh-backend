package vn.thathinh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.thathinh.constant.Gender;
import vn.thathinh.model.DatingPreferences;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Gender gender;
    private LocalDate birthDate;
    private DatingPreferences preferences;
    private boolean profileComplete;
    private boolean verified;
    private Boolean banned;
    private String bio;
    private List<String> interests;
    private List<String> photos;
}
