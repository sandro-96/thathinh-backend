package vn.thathinh.model;

import lombok.*;
import vn.thathinh.constant.Gender;

import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingPreferences {

    @Builder.Default
    private Set<Gender> lookingFor = EnumSet.noneOf(Gender.class);

    @Builder.Default
    private int minAge = 18;

    @Builder.Default
    private int maxAge = 60;

    public boolean wantsGender(Gender gender) {
        if (gender == null) return true;
        if (lookingFor == null || lookingFor.isEmpty()) return true;
        return lookingFor.contains(gender);
    }
}
