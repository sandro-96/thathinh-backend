package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.ReportStatus;
import vn.thathinh.dto.AdminDashboardResponse;
import vn.thathinh.exception.ResourceNotFoundException;
import vn.thathinh.model.User;
import vn.thathinh.model.UserReport;
import vn.thathinh.repository.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final FlirtSessionRepository flirtSessionRepository;
    private final UserReportRepository reportRepository;
    private final TopicMessageRepository topicMessageRepository;
    private final PrivateMessageRepository privateMessageRepository;

    public AdminDashboardResponse getDashboard() {
        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long sessionsToday = flirtSessionRepository.countByCreatedAtAfter(today);
        long matchesToday = flirtSessionRepository.countByMatchedAtAfter(today);
        double matchRate = sessionsToday > 0 ? (matchesToday * 100.0 / sessionsToday) : 0.0;
        long messagesToday = topicMessageRepository.countBySentAtAfter(today)
                + privateMessageRepository.countBySentAtAfter(today);

        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.countByDeletedFalse())
                .bannedUsers(userRepository.countByBannedTrueAndDeletedFalse())
                .activeTopics(topicRepository.countByActiveTrueAndDeletedFalse())
                .flirtSessionsToday(sessionsToday)
                .flirtMatchesToday(matchesToday)
                .matchRatePercent(Math.round(matchRate * 10) / 10.0)
                .pendingReports(reportRepository.countByStatus(ReportStatus.PENDING))
                .newUsersToday(userRepository.countByCreatedAtAfterAndDeletedFalse(today))
                .messagesToday(messagesToday)
                .dailyActiveUsers(userRepository.countByUpdatedAtAfterAndDeletedFalse(today))
                .build();
    }

    public List<User> listUsers() {
        return userRepository.findAll().stream().filter(u -> !u.isDeleted()).toList();
    }

    public void banUser(String userId, boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));
        user.setBanned(banned);
        userRepository.save(user);
    }

    public void reviewReport(String reportId, ReportStatus status, String note) {
        UserReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        report.setStatus(status);
        report.setAdminNote(note);
        reportRepository.save(report);
        if (status == ReportStatus.ACTIONED) {
            banUser(report.getReportedId(), true);
        }
    }
}
