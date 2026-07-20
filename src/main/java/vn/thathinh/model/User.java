package vn.thathinh.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import vn.thathinh.constant.AdminPermission;
import vn.thathinh.constant.Gender;
import vn.thathinh.constant.UserRole;
import vn.thathinh.model.base.BaseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    @Indexed(unique = true)
    private String nickname;

    private String avatarUrl;
    private Gender gender;
    private LocalDate birthDate;

    private String bio;

    @Builder.Default
    private java.util.List<String> interests = new java.util.ArrayList<>();

    @Builder.Default
    private java.util.List<String> photos = new java.util.ArrayList<>();

    @Builder.Default
    private DatingPreferences preferences = DatingPreferences.builder().build();

    @Builder.Default
    private boolean verified = false;

    @JsonIgnore
    private String verificationToken;
    private Instant verificationExpiry;

    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @Builder.Default
    private Set<AdminPermission> adminPermissions = EnumSet.noneOf(AdminPermission.class);

    @JsonIgnore
    private String resetToken;
    private Instant resetTokenExpiry;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean banned = false;

    private Instant lastLoginAt;

    @Indexed(unique = true, sparse = true)
    private String googleId;

    /** Vị trí GeoJSON [lng, lat] để tìm người quanh đây (2dsphere). Null nếu chưa bật. */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    /** Người dùng có bật chia sẻ vị trí (opt-in) hay không. */
    @Builder.Default
    private boolean locationEnabled = false;

    private Instant locationUpdatedAt;

    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
