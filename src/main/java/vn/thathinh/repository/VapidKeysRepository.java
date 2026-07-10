package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.model.VapidKeys;

public interface VapidKeysRepository extends MongoRepository<VapidKeys, String> {
}
