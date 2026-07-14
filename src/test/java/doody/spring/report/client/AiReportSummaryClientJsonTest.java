package doody.spring.report.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import doody.spring.report.client.AiReportSummaryClient.RecentMission;
import doody.spring.report.client.AiReportSummaryClient.ReportSummaryStats;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiReportSummaryClientJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializesReportStatsUsingAiEngineFieldNames() throws Exception {
        ReportSummaryStats stats = new ReportSummaryStats(
            12,
            8,
            160,
            Map.of("rhythm", 6, "autonomy", 4, "connection", 2),
            Map.of("rhythm", new BigDecimal("0.5")),
            List.of(new RecentMission(
                "mission-1",
                "AUTONOMY",
                "예시 미션",
                LocalDateTime.of(2026, 6, 10, 12, 30)
            ))
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(stats));

        assertThat(json.has("total_records")).isTrue();
        assertThat(json.has("active_days")).isTrue();
        assertThat(json.has("axis_counts")).isTrue();
        assertThat(json.has("recent_missions")).isTrue();
        assertThat(json.has("totalRecords")).isFalse();
        assertThat(json.at("/recent_missions/0/mission_id").asText()).isEqualTo("mission-1");
        assertThat(json.at("/recent_missions/0/completed_at").asText()).isEqualTo("2026-06-10T12:30:00");
    }
}
