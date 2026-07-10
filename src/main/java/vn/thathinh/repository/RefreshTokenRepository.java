package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    void deleteByUserId(String userId);
}
