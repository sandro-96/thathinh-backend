package vn.thathinh.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.thathinh.constant.TopicType;
import vn.thathinh.model.Topic;
import vn.thathinh.repository.TopicRepository;
import vn.thathinh.util.SlugUtils;

@Configuration
public class TopicSeedConfig {

    @Bean
    ApplicationRunner seedTopics(TopicRepository topicRepository) {
        return args -> {
            // Tỉnh thành
            seedIfMissing(topicRepository, "Hà Nội", "Trò chuyện với người ở Hà Nội", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "TP. Hồ Chí Minh", "Trò chuyện với người ở Sài Gòn", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Đà Nẵng", "Trò chuyện với người ở Đà Nẵng", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Hải Phòng", "Trò chuyện với người ở Hải Phòng", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Cần Thơ", "Trò chuyện với người ở Cần Thơ", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Huế", "Trò chuyện với người ở Huế", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Nha Trang", "Trò chuyện với người ở Nha Trang", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Đà Lạt", "Trò chuyện với người ở Đà Lạt", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Bình Dương", "Trò chuyện với người ở Bình Dương", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Đồng Nai", "Trò chuyện với người ở Đồng Nai", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Quảng Ninh", "Trò chuyện với người ở Quảng Ninh", TopicType.PROVINCE);
            seedIfMissing(topicRepository, "Vũng Tàu", "Trò chuyện với người ở Vũng Tàu", TopicType.PROVINCE);

            // CLB / sở thích
            seedIfMissing(topicRepository, "CLB Đọc sách", "Gặp gỡ những người yêu sách", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Cà phê", "Thích cà phê và trò chuyện", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Du lịch", "Chia sẻ chuyến đi và điểm đến yêu thích", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Gym & Fitness", "Tập luyện và sống khoẻ", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Anime", "Otaku gặp gỡ, chia sẻ anime/manga", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Nấu ăn", "Món ngon và bí quyết bếp núc", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Chạy bộ", "Runner và những buổi sáng năng động", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Âm nhạc", "Nghe nhạc, đi show, chơi nhạc cụ", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Board game", "Cờ, bài và game tương tác", TopicType.CLUB);
            seedIfMissing(topicRepository, "CLB Phim ảnh", "Review phim và rủ xem phim", TopicType.CLUB);
        };
    }

    private void seedIfMissing(TopicRepository repo, String name, String desc, TopicType type) {
        String slug = SlugUtils.toSlug(name);
        if (repo.findBySlugAndDeletedFalse(slug).isEmpty()) {
            repo.save(Topic.builder()
                    .name(name)
                    .description(desc)
                    .type(type)
                    .slug(slug)
                    .active(true)
                    .memberCount(0)
                    .build());
        }
    }
}
