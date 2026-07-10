package vn.thathinh.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import vn.thathinh.constant.FlirtStatus;
import vn.thathinh.model.FlirtSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FlirtSessionRepository extends MongoRepository<FlirtSession, String> {
    Optional<FlirtSession> findByIdAndDeletedFalse(String id);
    List<FlirtSession> findByUserAIdOrUserBIdAndStatusIn(String userAId, String userBId, List<FlirtStatus> statuses);
    List<FlirtSession> findByUserAIdAndStatus(String userAId, FlirtStatus status);
    List<FlirtSession> findByUserBIdAndStatus(String userBId, FlirtStatus status);
    long countByCreatedAtAfter(Instant after);
    long countByMatchedAtAfter(Instant after);
    List<FlirtSession> findByUserAIdOrUserBIdAndCreatedAtAfter(String userAId, String userBId, Instant after);

    @Query(value = "{ 'deleted': false, '$or': [ { 'userAId': ?0 }, { 'userBId': ?0 } ], 'matchedAt': { '$ne': null } }",
            sort = "{ 'matchedAt': -1 }")
    List<FlirtSession> findMatchedHistoryForUser(String userId, Pageable pageable);
}
