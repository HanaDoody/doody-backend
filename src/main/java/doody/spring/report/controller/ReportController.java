package doody.spring.report.controller;

import doody.spring.common.dto.ApiResponse;
import doody.spring.report.dto.RecoveryReportResponse;
import doody.spring.report.dto.ReportSummaryResponse;
import doody.spring.report.service.ReportService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/recovery")
    public ApiResponse<RecoveryReportResponse> getRecoveryReport(
        @PathVariable String userId,
        @RequestParam(defaultValue = "month") String period
    ) {
        return ApiResponse.success(HttpStatus.OK, "recovery report lookup success.", reportService.getRecoveryReport(userId, period));
    }
}