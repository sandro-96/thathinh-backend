package vn.thathinh.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.stereotype.Component;
import vn.thathinh.model.User;

/**
 * Đảm bảo các index bắt buộc tồn tại khi khởi động, kể cả khi tắt auto-index-creation
 * (prod). ensureIndex là idempotent nên gọi lại nhiều lần vẫn an toàn.
 *
 * <p>Quan trọng với tính năng "Quanh đây": truy vấn $geoNear cần index 2dsphere trên
 * {@code users.location}, nếu thiếu sẽ lỗi lúc chạy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexInitializer {

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        try {
            mongoTemplate.indexOps(User.class).ensureIndex(
                    new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
            log.info("Đã đảm bảo index 2dsphere cho users.location");
        } catch (Exception e) {
            log.error("Không thể tạo index 2dsphere cho users.location — tính năng 'Quanh đây' có thể lỗi", e);
        }
    }
}
