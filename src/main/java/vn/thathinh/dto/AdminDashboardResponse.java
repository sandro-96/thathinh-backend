package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long bannedUsers;
    private long activeTopics;
    private long flirtSessionsToday;
    private long pendingReports;
    private long newUsersToday;
    private long messagesToday;
    private long flirtMatchesToday;
    private double matchRatePercent;
    private long dailyActiveUsers;
}
