package doody.spring.report.dto;

import doody.spring.domain.entity.ReportSummary;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportSummaryResponse(
    Long reportSummaryId,
    String period,
    LocalDate periodStart,
    LocalDate periodEnd,
    String stats,
    String summary,
    String highlights,
    LocalDateTime generatedAt
) {

    public static ReportSummaryResponse from(ReportSummary reportSummary) {
        return new ReportSummaryResponse(
            reportSummary.getId(),
            reportSummary.getPeriod(),
            reportSummary.getPeriodStart(),
            reportSummary.getPeriodEnd(),
            reportSummary.getStats(),
            reportSummary.getSummary(),
            reportSummary.getHighlights(),
            reportSummary.getGeneratedAt()
        );
    }
}