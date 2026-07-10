package vn.thathinh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.thathinh.constant.*;
import vn.thathinh.dto.ApiResponseDto;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
public class EnumController {

    @GetMapping
    public ApiResponseDto<Map<String, Object>> enums() {
        return ApiResponseDto.success(ApiCode.SUCCESS, Map.of(
                "gender", Arrays.stream(Gender.values()).map(Enum::name).collect(Collectors.toList()),
                "topicType", Arrays.stream(TopicType.values()).map(Enum::name).collect(Collectors.toList()),
                "flirtStatus", Arrays.stream(FlirtStatus.values()).map(Enum::name).collect(Collectors.toList()),
                "reportReasons", ReportReasons.FLIRT_REPORT_REASONS
        ));
    }
}
