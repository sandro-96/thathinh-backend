package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import vn.thathinh.model.UserBlock;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends MongoRepository<UserBlock, String> {
    Optional<UserBlock> findByBlockerIdAndBlockedIdAndDeletedFalse(String blockerId, String blockedId);
    boolean existsByBlockerIdAndBlockedIdAndDeletedFalse(String blockerId, String blockedId);

    @Query(value = "{ 'deleted': false, '$or': [ { 'blockerId': ?0, 'blockedId': ?1 }, { 'blockerId': ?1, 'blockedId': ?0 } ] }", exists = true)
    boolean existsActiveBlockBetween(String userA, String userB);

    List<UserBlock> findByBlockerIdAndDeletedFalse(String blockerId);

    List<UserBlock> findByBlockedIdAndDeletedFalse(String blockedId);
}
