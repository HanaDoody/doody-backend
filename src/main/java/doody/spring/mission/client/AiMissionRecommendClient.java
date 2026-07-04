package doody.spring.mission.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.mission.dto.TodayMissionResponse;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import doody.spring.mission.dto.TodayMissionResponse.Contact;
import doody.spring.mission.dto.TodayMissionResponse.Mission;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiMissionRecommendClient {

    private static final Logger log = LoggerFactory.getLogger(AiMissionRecommendClient.class);

    private final String baseUrl;
    private final RestClient restClient;

    public AiMissionRecommendClient(@Value("${AI_ENGINE_BASE_URL:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = RestClient.builder().build();
    }

    public TodayMissionResponse recommend(AiMissionRecommendRequest request, Mission fallbackMission) {
        if (baseUrl.isBlank()) {
            log.warn("AI mission recommend fallback: AI_ENGINE_BASE_URL is blank. userId={}, hasFallbackMission={}",
                request == null ? null : request.userId(),
                fallbackMission != null);
            return fallback(fallbackMission);
        }

        try {
            log.info("AI mission recommend request: url={}, userId={}, energy={}, historyCount={}",
                baseUrl + "/mission/recommend",
                request == null ? null : request.userId(),
                request == null ? null : request.energy(),
                request == null || request.history() == null ? 0 : request.history().size());

            AiMissionRecommendResponse response = restClient.post()
                .uri(baseUrl + "/mission/recommend")
                .body(request)
                .retrieve()
                .body(AiMissionRecommendResponse.class);

            if (response == null) {
                log.warn("AI mission recommend fallback: response body is null. userId={}, hasFallbackMission={}",
                    request == null ? null : request.userId(),
                    fallbackMission != null);
                return fallback(fallbackMission);
            }

            Mission mission = toMission(response.mission());
            Mission fallback = toMission(response.fallback());
            log.info("AI mission recommend response: userId={}, missionState={}, missionId={}, restMessagePresent={}, fallbackMissionId={}",
                request == null ? null : request.userId(),
                response.missionState(),
                mission == null ? null : mission.missionId(),
                response.restMessage() != null && !response.restMessage().isBlank(),
                fallback == null ? null : fallback.missionId());
            return new TodayMissionResponse(
                mission,
                fallback,
                response.missionState(),
                recommendedAxis(mission),
                nextPath(mission),
                response.restMessage(),
                response.unlockedContacts() == null ? List.of() : response.unlockedContacts(),
                response.vRhythm(),
                response.diagnostics(),
                "AI"
            );
        } catch (Exception exception) {
            log.warn("AI mission recommend fallback: request failed. userId={}, url={}, hasFallbackMission={}",
                request == null ? null : request.userId(),
                baseUrl + "/mission/recommend",
                fallbackMission != null,
                exception);
            return fallback(fallbackMission);
        }
    }

    private TodayMissionResponse fallback(Mission fallbackMission) {
        if (fallbackMission == null) {
            return new TodayMissionResponse(
                null,
                null,
                "gated",
                null,
                null,
                "오늘은 리듬만 붙잡아도 충분해.",
                List.of(),
                0.0,
                Map.of("source", "fallback_no_template"),
                "FALLBACK"
            );
        }

        return new TodayMissionResponse(
            fallbackMission,
            null,
            "active",
            recommendedAxis(fallbackMission),
            nextPath(fallbackMission),
            null,
            List.of(),
            0.8,
            Map.of("source", "fallback_template"),
            "FALLBACK"
        );
    }

    private String recommendedAxis(Mission mission) {
        return mission == null ? null : normalizeAxis(mission.axis());
    }

    private String nextPath(Mission mission) {
        String axis = recommendedAxis(mission);
        if ("AUTONOMY".equals(axis)) {
            return "/autonomy";
        }
        if ("CONNECTION".equals(axis)) {
            return "/connection";
        }
        if ("RHYTHM".equals(axis)) {
            return "/rhythm";
        }
        return null;
    }

    private String normalizeAxis(String axis) {
        return axis == null ? null : axis.strip().toUpperCase();
    }

    private Mission toMission(AiMission mission) {
        if (mission == null) {
            return null;
        }
        String missionId = mission.missionId() == null || mission.missionId().isBlank()
            ? mission.id()
            : mission.missionId();

        return new Mission(
            mission.id(),
            missionId,
            mission.axis(),
            mission.stage(),
            mission.waypoint(),
            mission.difficulty(),
            mission.delta(),
            mission.title(),
            mission.description(),
            mission.missionType(),
            mission.requiredCount(),
            mission.signature(),
            mission.fallback(),
            mission.fallbackMissionId(),
            mission.goalTags() == null ? List.of() : mission.goalTags(),
            mission.howTo() == null ? List.of() : mission.howTo(),
            mission.reason()
        );
    }

    public record AiMissionRecommendRequest(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("current_ari")
        AriVector currentAri,
        AriVector goal,
        @JsonProperty("recent_trajectory")
        List<Object> recentTrajectory,
        Short energy,
        @JsonProperty("rhythm_history")
        RhythmHistory rhythmHistory,
        List<MissionHistory> history
    ) {
    }

    public record RhythmHistory(
        List<MorningHistory> morning,
        List<EveningHistory> evening
    ) {
    }

    public record MorningHistory(
        LocalDateTime timestamp,
        @JsonProperty("did_check_in")
        Boolean didCheckIn
    ) {
    }

    public record EveningHistory(
        LocalDateTime timestamp,
        String text
    ) {
    }

    public record MissionHistory(
        @JsonProperty("mission_id")
        String missionId,
        String axis,
        String title,
        @JsonProperty("completed_at")
        LocalDateTime completedAt
    ) {
    }

    private record AiMissionRecommendResponse(
        AiMission mission,
        AiMission fallback,
        @JsonProperty("mission_state")
        String missionState,
        @JsonProperty("rest_message")
        String restMessage,
        @JsonProperty("unlocked_contacts")
        List<Contact> unlockedContacts,
        @JsonProperty("v_rhythm")
        Double vRhythm,
        Map<String, Object> diagnostics
    ) {
    }

    private record AiMission(
        String id,
        @JsonProperty("mission_id")
        String missionId,
        String axis,
        Integer stage,
        String waypoint,
        Integer difficulty,
        AriVector delta,
        String title,
        String description,
        @JsonProperty("mission_type")
        String missionType,
        @JsonProperty("required_count")
        Integer requiredCount,
        @JsonProperty("is_signature")
        Boolean signature,
        @JsonProperty("is_fallback")
        Boolean fallback,
        @JsonProperty("fallback_mission_id")
        String fallbackMissionId,
        @JsonProperty("goal_tags")
        List<String> goalTags,
        @JsonProperty("how_to")
        List<String> howTo,
        String reason
    ) {
    }
}
