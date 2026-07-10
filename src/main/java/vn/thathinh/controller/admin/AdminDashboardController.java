package vn.thathinh.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.ReportStatus;
import vn.thathinh.dto.AdminDashboardResponse;
import vn.thathinh.dto.AdminReportResponse;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.dto.CreateTopicRequest;
import vn.thathinh.dto.UserProfileResponse;
import vn.thathinh.model.Topic;
import vn.thathinh.model.User;
import vn.thathinh.service.AdminService;
import vn.thathinh.service.ReportService;
import vn.thathinh.service.TopicService;
import vn.thathinh.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminService adminService;
    private final TopicService topicService;
    private final UserService userService;
    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ApiResponseDto<AdminDashboardResponse> dashboard() {
        return ApiResponseDto.success(ApiCode.SUCCESS, adminService.getDashboard());
    }

    @GetMapping("/users")
    public ApiResponseDto<List<UserProfileResponse>> users() {
        List<UserProfileResponse> users = adminService.listUsers().stream()
                .map(userService::toAdminResponse)
                .toList();
        return ApiResponseDto.success(ApiCode.SUCCESS, users);
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponseDto<Void> ban(@PathVariable String id, @RequestParam boolean banned) {
        adminService.banUser(id, banned);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @GetMapping("/topics")
    public ApiResponseDto<List<Topic>> topics() {
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.listAllAdmin());
    }

    @PostMapping("/topics")
    public ApiResponseDto<Topic> createTopic(@RequestBody @Valid CreateTopicRequest request) {
        return ApiResponseDto.success(ApiCode.CREATED, topicService.createTopic(request));
    }

    @PutMapping("/topics/{id}")
    public ApiResponseDto<Topic> updateTopic(@PathVariable String id, @RequestBody @Valid CreateTopicRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.updateTopic(id, request));
    }

    @DeleteMapping("/topics/{id}")
    public ApiResponseDto<Void> deleteTopic(@PathVariable String id) {
        topicService.deleteTopic(id);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @GetMapping("/reports")
    public ApiResponseDto<List<AdminReportResponse>> reports(@RequestParam(required = false) ReportStatus status) {
        return ApiResponseDto.success(ApiCode.SUCCESS, reportService.listReportsForAdmin(status));
    }

    @PostMapping("/reports/{id}/review")
    public ApiResponseDto<Void> reviewReport(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        ReportStatus status = ReportStatus.valueOf(body.getOrDefault("status", "REVIEWED"));
        adminService.reviewReport(id, status, body.get("note"));
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
