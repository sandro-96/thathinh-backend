package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.ReportStatus;

import java.time.Instant;

@Data
@Builder
public class AdminReportResponse {
    private String id;
    private String sessionId;
    private String reporterId;
    private String reporterNickname;
    private String reportedId;
    private String reportedNickname;
    private boolean reportedBanned;
    private String reason;
    private ReportStatus status;
    private String adminNote;
    private Instant createdAt;
}
