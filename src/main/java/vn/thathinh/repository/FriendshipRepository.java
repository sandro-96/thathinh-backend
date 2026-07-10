package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import vn.thathinh.constant.FriendshipStatus;
import vn.thathinh.model.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    Optional<Friendship> findByUserLowIdAndUserHighIdAndDeletedFalse(String userLowId, String userHighId);

    Optional<Friendship> findByUserLowIdAndUserHighId(String userLowId, String userHighId);

    @Query("{ 'deleted': false, '$or': [ { 'userLowId': ?0 }, { 'userHighId': ?0 } ] }")
    List<Friendship> findAllActiveForUser(String userId);

    List<Friendship> findByDeletedFalseAndStatusAndUserLowId(FriendshipStatus status, String userId);

    List<Friendship> findByDeletedFalseAndStatusAndUserHighId(FriendshipStatus status, String userId);

    List<Friendship> findByDeletedFalseAndStatusAndUserLowIdAndRequestedByNot(
            FriendshipStatus status, String userId, String requestedBy);

    List<Friendship> findByDeletedFalseAndStatusAndUserHighIdAndRequestedByNot(
            FriendshipStatus status, String userId, String requestedBy);
}
