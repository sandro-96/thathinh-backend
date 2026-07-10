package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.ReportStatus;
import vn.thathinh.dto.AccountStatusResponse;
import vn.thathinh.dto.AdminReportResponse;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.model.User;
import vn.thathinh.model.UserReport;
import vn.thathinh.repository.UserReportRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserReportRepository reportRepository;
    private final UserService userService;

    /** Báo cáo một người dùng ngoài phiên thả thính (vd. trong chat riêng). */
    public void reportUser(String reporterId, String reportedId, String reason) {
        if (reporterId.equals(reportedId)) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        userService.findUser(reportedId);
        reportRepository.save(UserReport.builder()
                .reporterId(reporterId)
                .reportedId(reportedId)
                .reason(reason)
                .build());
    }

    public AccountStatusResponse getAccountStatus(String userId) {
        User user = userService.findUser(userId);
        List<UserReport> pending = reportRepository.findByReportedIdAndStatusOrderByCreatedAtDesc(
                userId, ReportStatus.PENDING);

        boolean flirtAllowed = user.isActive() && !user.isBanned();
        String flirtBlockReason = null;
        if (!user.isActive()) {
            flirtBlockReason = "Tài khoản không còn hoạt động";
        } else if (user.isBanned()) {
            flirtBlockReason = "Tài khoản đã bị khoá do vi phạm. Liên hệ admin nếu bạn cho rằng đây là nhầm lẫn.";
        }

        AccountStatusResponse.ReportSummary latest = null;
        if (!pending.isEmpty()) {
            UserReport report = pending.get(0);
            latest = AccountStatusResponse.ReportSummary.builder()
                    .id(report.getId())
                    .status(report.getStatus())
                    .reason(report.getReason())
                    .createdAt(report.getCreatedAt())
                    .build();
        }

        return AccountStatusResponse.builder()
                .banned(user.isBanned())
                .flirtAllowed(flirtAllowed)
                .flirtBlockReason(flirtBlockReason)
                .pendingReportsAgainstMe(pending.size())
                .latestReportAgainstMe(latest)
                .build();
    }

    public List<AdminReportResponse> listReportsForAdmin(ReportStatus status) {
        List<UserReport> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(this::toAdminResponse).toList();
    }

    public AdminReportResponse toAdminResponse(UserReport report) {
        User reporter = userService.findUser(report.getReporterId());
        User reported = userService.findUser(report.getReportedId());
        return AdminReportResponse.builder()
                .id(report.getId())
                .sessionId(report.getSessionId())
                .reporterId(report.getReporterId())
                .reporterNickname(reporter.getNickname())
                .reportedId(report.getReportedId())
                .reportedNickname(reported.getNickname())
                .reportedBanned(reported.isBanned())
                .reason(report.getReason())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
