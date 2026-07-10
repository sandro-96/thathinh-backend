package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByNicknameAndDeletedFalse(String nickname);
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByResetToken(String token);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmailAndDeletedFalse(String email);
    boolean existsByNicknameAndDeletedFalse(String nickname);
    long countByDeletedFalse();
    long countByBannedTrueAndDeletedFalse();
    long countByCreatedAtAfterAndDeletedFalse(java.time.Instant after);
    long countByUpdatedAtAfterAndDeletedFalse(java.time.Instant after);
}
