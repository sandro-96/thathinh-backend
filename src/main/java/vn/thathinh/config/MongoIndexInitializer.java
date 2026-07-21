package vn.thathinh.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.IndexOperations;
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
            IndexOperations indexOps = mongoTemplate.indexOps(User.class);
            // Nếu đã có index trên trường 'location' (vd. do @GeoSpatialIndexed tạo khi
            // auto-index-creation=true), bỏ qua để tránh IndexOptionsConflict (lỗi 85)
            // do trùng key nhưng khác tên index.
            boolean locationIndexed = indexOps.getIndexInfo().stream()
                    .flatMap(info -> info.getIndexFields().stream())
                    .anyMatch(field -> "location".equals(field.getKey()));
            if (locationIndexed) {
                log.info("Index cho users.location đã tồn tại — bỏ qua tạo mới");
                return;
            }
            indexOps.ensureIndex(new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
            log.info("Đã tạo index 2dsphere cho users.location");
        } catch (Exception e) {
            log.error("Không thể tạo index 2dsphere cho users.location — tính năng 'Quanh đây' có thể lỗi", e);
        }
    }
}
