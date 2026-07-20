package vn.thathinh.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.Gender;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sinh avatar mặc định theo giới tính. Dùng DiceBear (đáng tin cậy, không cần lưu ảnh):
 * mỗi giới có màu nền riêng (nam xanh / nữ hồng / khác tím) và một danh sách seed cố định
 * để người dùng chọn lại. URL sinh từ seed nên FE và BE luôn khớp nhau.
 */
@Service
public class AvatarService {

    private static final String STYLE = "avataaars";
    private static final String BASE = "https://api.dicebear.com/7.x/" + STYLE + "/svg";

    /** Danh sách seed (mỗi seed cho ra một khuôn mặt khác nhau). */
    private static final List<String> SEEDS = List.of(
            "Aiden", "Bella", "Cami", "Dieu", "Emmy", "Finn",
            "Gia", "Hana", "Ivy", "Jax", "Kai", "Lily",
            "Minh", "Nova", "Oscar", "Pixie", "Quin", "Rosa",
            "Suri", "Theo", "Uma", "Vinh", "Wren", "Yuki");

    private String backgroundColor(Gender gender) {
        if (gender == Gender.MALE) return "b6e3f4,a0e7e5";
        if (gender == Gender.FEMALE) return "ffd5dc,ffb3c6";
        return "d1d4f9,c0aede";
    }

    public String buildUrl(Gender gender, String seed) {
        String safeSeed = SEEDS.contains(seed) ? seed : SEEDS.get(0);
        return BASE + "?seed=" + safeSeed + "&backgroundColor=" + backgroundColor(gender)
                + "&radius=50";
    }

    /** URL avatar mặc định ngẫu nhiên cho giới tính khi tạo tài khoản. */
    public String randomDefault(Gender gender) {
        String seed = SEEDS.get(ThreadLocalRandom.current().nextInt(SEEDS.size()));
        return buildUrl(gender, seed);
    }

    public boolean isValidSeed(String seed) {
        return SEEDS.contains(seed);
    }

    /** Danh sách preset để người dùng chọn lại trong màn Hồ sơ. */
    public List<AvatarPreset> presets(Gender gender) {
        return SEEDS.stream()
                .map(seed -> AvatarPreset.builder().seed(seed).url(buildUrl(gender, seed)).build())
                .toList();
    }

    @Getter
    @Builder
    public static class AvatarPreset {
        private final String seed;
        private final String url;
    }
}
