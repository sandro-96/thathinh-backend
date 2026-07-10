package vn.thathinh.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.FlirtMessage;

import java.time.Instant;
import java.util.List;

public interface FlirtMessageRepository extends MongoRepository<FlirtMessage, String> {
    List<FlirtMessage> findBySessionIdOrderBySentAtDesc(String sessionId, Pageable pageable);
    List<FlirtMessage> findBySessionIdOrderBySentAtAsc(String sessionId);
    List<FlirtMessage> findBySessionIdAndSentAtBeforeOrderBySentAtDesc(String sessionId, Instant before, Pageable pageable);
}
