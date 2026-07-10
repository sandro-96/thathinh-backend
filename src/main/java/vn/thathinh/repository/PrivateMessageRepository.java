package vn.thathinh.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.PrivateMessage;

import java.time.Instant;
import java.util.List;

public interface PrivateMessageRepository extends MongoRepository<PrivateMessage, String> {
    List<PrivateMessage> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);
    List<PrivateMessage> findByConversationIdAndSentAtBeforeOrderBySentAtDesc(
            String conversationId, Instant before, Pageable pageable);
    long countBySentAtAfter(Instant after);
    long countByConversationIdAndSenderIdNot(String conversationId, String senderId);
    long countByConversationIdAndSenderIdNotAndSentAtAfter(
            String conversationId, String senderId, Instant after);
    void deleteByConversationId(String conversationId);
}
