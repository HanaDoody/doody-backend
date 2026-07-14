package doody.spring.report.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.report.dto.RecoveryReportResponse.ActivitySummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiReportSummaryClient {

    private static final Logger log = LoggerFactory.getLogger(AiReportSummaryClient.class);

    private final String baseUrl;
    private final RestClient restClient;

    public AiReportSummaryClient(
        @Value("${AI_ENGINE_BASE_URL:}") String baseUrl,
        @Qualifier("aiEngineRestClient") RestClient restClient
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = restClient;
    }

    public ActivitySummary summarize(String userId, String period, ReportSummaryStats stats) {
        if (baseUrl.isBlank()) {
            log.warn("AI report summary fallback: ai.engine.base-url is blank. userId={}, period={}", userId, period);
            return fallback(stats);
        }

        try {
            AiReportSummaryResponse response = restClient.post()
                .uri(baseUrl + "/report/summary")
                .body(new AiReportSummaryRequest(userId, period, stats))
                .retrieve()
                .body(AiReportSummaryResponse.class);

            if (response == null || response.summary() == null || response.summary().isBlank()) {
                log.warn("AI report summary fallback: response summary is blank. userId={}, period={}, url={}",
                    userId,
                    period,
                    baseUrl + "/report/summary"
                );
                return fallback(stats);
            }

            return new ActivitySummary(
                response.summary(),
                response.highlights() == null ? List.of() : response.highlights(),
                parseGeneratedAt(response.generatedAt()),
                "AI"
            );
        } catch (Exception exception) {
            log.warn("AI report summary fallback: request failed. userId={}, period={}, url={}",
                userId,
                period,
                baseUrl + "/report/summary",
                exception
            );
            return fallback(stats);
        }
    }

    private ActivitySummary fallback(ReportSummaryStats stats) {
        String summary = "이번 기간에는 총 " + stats.totalRecords() + "개의 기록이 "
            + stats.activeDays() + "일 동안 쌓였어. "
            + "리듬 " + stats.axisCounts().getOrDefault("rhythm", 0)
            + "개, 자립 " + stats.axisCounts().getOrDefault("autonomy", 0)
            + "개, 연결 " + stats.axisCounts().getOrDefault("connection", 0)
            + "개의 기록이 모였고 작은 회복 흐름을 이어가고 있어.";

        return new ActivitySummary(
            summary,
            List.of(
                "이번 기간에는 총 " + stats.totalRecords() + "개의 기록을 남겼어.",
                "기록이 이어진 날은 모두 " + stats.activeDays() + "일이야.",
                "회복 흐름 속에서 +" + stats.points() + "P를 모았어."
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
        @JsonProperty("total_records")
        Integer totalRecords,
        @JsonProperty("active_days")
        Integer activeDays,
        Integer points,
        @JsonProperty("axis_counts")
        Map<String, Integer> axisCounts,
        Map<String, BigDecimal> goal,
        @JsonProperty("recent_missions")
        List<RecentMission> recentMissions
    ) {
    }

    public record RecentMission(
        @JsonProperty("mission_id")
        String missionId,
        String axis,
        String title,
        @JsonProperty("completed_at")
        LocalDateTime completedAt
    ) {
    }

    private record AiReportSummaryRequest(
        @JsonProperty("user_id")
        String userId,
        String period,
        ReportSummaryStats stats
    ) {
    }

    private record AiReportSummaryResponse(
        String summary,
        List<String> highlights,
        @JsonProperty("generated_at")
        String generatedAt
    ) {
    }
}
