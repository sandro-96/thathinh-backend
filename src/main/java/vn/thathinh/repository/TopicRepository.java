package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.Topic;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends MongoRepository<Topic, String> {
    List<Topic> findByActiveTrueAndDeletedFalseOrderByMemberCountDesc();
    List<Topic> findByIdInAndDeletedFalse(List<String> ids);
    Optional<Topic> findBySlugAndDeletedFalse(String slug);
    Optional<Topic> findByIdAndDeletedFalse(String id);
    long countByActiveTrueAndDeletedFalse();
}
