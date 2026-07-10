package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;
import vn.thathinh.constant.ReportStatus;

import java.time.Instant;

@Data
@Builder
public class AccountStatusResponse {
    private boolean banned;
    private boolean flirtAllowed;
    private String flirtBlockReason;
    private int pendingReportsAgainstMe;
    private ReportSummary latestReportAgainstMe;

    @Data
    @Builder
    public static class ReportSummary {
        private String id;
        private ReportStatus status;
        private String reason;
        private Instant createdAt;
    }
}
