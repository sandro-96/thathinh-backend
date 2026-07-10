package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.TopicMembership;

import java.util.List;
import java.util.Optional;

public interface TopicMembershipRepository extends MongoRepository<TopicMembership, String> {
    Optional<TopicMembership> findByTopicIdAndUserId(String topicId, String userId);
    boolean existsByTopicIdAndUserId(String topicId, String userId);
    long countByTopicId(String topicId);
    List<TopicMembership> findByUserId(String userId);
    void deleteByTopicIdAndUserId(String topicId, String userId);
}
