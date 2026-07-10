package vn.thathinh.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.thathinh.constant.ReportStatus;
import vn.thathinh.model.UserReport;

import java.util.List;
import java.util.Optional;

public interface UserReportRepository extends MongoRepository<UserReport, String> {
    List<UserReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<UserReport> findAllByOrderByCreatedAtDesc();
    List<UserReport> findByReportedIdAndStatusOrderByCreatedAtDesc(String reportedId, ReportStatus status);
    Optional<UserReport> findFirstBySessionId(String sessionId);
    long countByStatus(ReportStatus status);
}
