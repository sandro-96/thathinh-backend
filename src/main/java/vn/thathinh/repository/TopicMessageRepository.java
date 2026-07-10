package vn.thathinh.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.TopicMessage;

import java.time.Instant;
import java.util.List;

public interface TopicMessageRepository extends MongoRepository<TopicMessage, String> {
    List<TopicMessage> findByTopicIdAndSentAtBeforeOrderBySentAtDesc(String topicId, Instant before, Pageable pageable);
    List<TopicMessage> findByTopicIdOrderBySentAtDesc(String topicId, Pageable pageable);
    long countBySentAtAfter(Instant after);
}
