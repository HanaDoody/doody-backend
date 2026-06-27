package doody.spring.report.client;

import doody.spring.report.dto.RecoveryReportResponse.ActivitySummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiReportSummaryClient {

    private final String baseUrl;
    private final RestClient restClient;

    public AiReportSummaryClient(@Value("${ai.engine.base-url:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = RestClient.builder().build();
    }

    public ActivitySummary summarize(String userId, String period, ReportSummaryStats stats) {
        if (baseUrl.isBlank()) {
            return fallback(stats);
        }

        try {
            AiReportSummaryResponse response = restClient.post()
                .uri(baseUrl + "/report/summary")
                .body(new AiReportSummaryRequest(userId, period, stats))
                .retrieve()
                .body(AiReportSummaryResponse.class);

            if (response == null || response.summary() == null || response.summary().isBlank()) {
                return fallback(stats);
            }

            return new ActivitySummary(
                response.summary(),
                response.highlights() == null ? List.of() : response.highlights(),
                parseGeneratedAt(response.generatedAt()),
                "AI"
            );
        } catch (Exception exception) {
            return fallback(stats);
        }
    }

    private ActivitySummary fallback(ReportSummaryStats stats) {
        String summary = "This period has " + stats.totalRecords() + " records across "
            + stats.activeDays() + " active days. "
            + "Rhythm " + stats.axisCounts().getOrDefault("rhythm", 0)
            + ", autonomy " + stats.axisCounts().getOrDefault("autonomy", 0)
            + ", and connection " + stats.axisCounts().getOrDefault("connection", 0)
            + " records were collected.";

        return new ActivitySummary(
            summary,
            List.of(
                "Total records: " + stats.totalRecords(),
                "Active days: " + stats.activeDays(),
                "Points: +" + stats.points() + "P"
            ),
            LocalDateTime.now(),
            "FALLBACK"
        );
    }

    private LocalDateTime parseGeneratedAt(String generatedAt) {
        if (generatedAt == null || generatedAt.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(generatedAt.replace("+09:00", ""));
        } catch (Exception exception) {
            return LocalDateTime.now();
        }
    }

    public record ReportSummaryStats(
        Integer totalRecords,
        Integer activeDays,
        Integer points,
        Map<String, Integer> axisCounts,
        List<RecentMission> recentMissions
    ) {
    }

    public record RecentMission(
        String missionId,
        String axis,
        String title,
        LocalDateTime completedAt
    ) {
    }

    private record AiReportSummaryRequest(
        String userId,
        String period,
        ReportSummaryStats stats
    ) {
    }

    private record AiReportSummaryResponse(
        String summary,
        List<String> highlights,
        String generatedAt
    ) {
    }
}