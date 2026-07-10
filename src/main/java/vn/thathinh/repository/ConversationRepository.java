package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByUserLowIdAndUserHighIdAndDeletedFalse(String userLowId, String userHighId);
    Optional<Conversation> findByUserLowIdAndUserHighId(String userLowId, String userHighId);
    Optional<Conversation> findByIdAndDeletedFalse(String id);
    List<Conversation> findByDeletedFalseAndUserLowIdOrDeletedFalseAndUserHighIdOrderByLastMessageAtDesc(
            String userLowId, String userHighId);
}
