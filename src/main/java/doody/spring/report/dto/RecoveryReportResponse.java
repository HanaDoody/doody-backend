package doody.spring.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RecoveryReportResponse(
    String period,
    LocalDate periodStart,
    LocalDate periodEnd,
    ActivitySummary activitySummary,
    Summary summary,
    List<AxisSummary> axisSummaries,
    List<AxisRecordGroup> recordGroups
) {

    public record ActivitySummary(
        String summary,
        List<String> highlights,
        LocalDateTime generatedAt,
        String source
    ) {
    }

    public record Summary(
        Integer totalRecordCount,
        Integer recordedDayCount,
        Integer totalPoint
    ) {
    }

    public record AxisSummary(
        String axis,
        Integer recordCount
    ) {
    }

    public record AxisRecordGroup(
        String axis,
        Integer recordCount,
        List<RecordItem> records
    ) {
    }

    public record RecordItem(
        String recordType,
        Long recordId,
        String title,
        Integer reward,
        LocalDate recordDate,
        LocalDateTime recordedAt
    ) {
    }
}