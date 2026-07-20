package vn.thathinh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.thathinh.constant.Gender;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyUserResponse {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private Gender gender;
    private int age;
    private String bio;
    /** Khoảng cách tới người dùng hiện tại, làm tròn (km). */
    private double distanceKm;
    /** Trạng thái kết bạn với người này: PENDING/ACCEPTED/... hoặc null nếu chưa có. */
    private String friendshipStatus;
}
