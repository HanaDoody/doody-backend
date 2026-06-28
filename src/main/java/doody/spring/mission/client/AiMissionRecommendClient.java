package doody.spring.mission.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.mission.dto.TodayMissionResponse;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import doody.spring.mission.dto.TodayMissionResponse.Contact;
import doody.spring.mission.dto.TodayMissionResponse.Mission;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiMissionRecommendClient {

    private final String baseUrl;
    private final RestClient restClient;

    public AiMissionRecommendClient(@Value("${ai.engine.base-url:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = RestClient.builder().build();
    }

    public TodayMissionResponse recommend(AiMissionRecommendRequest request, Mission fallbackMission) {
        if (baseUrl.isBlank()) {
            return fallback(fallbackMission);
        }

        try {
            AiMissionRecommendResponse response = restClient.post()
                .uri(baseUrl + "/mission/recommend")
                .body(request)
                .retrieve()
                .body(AiMissionRecommendResponse.class);

            if (response == null) {
                return fallback(fallbackMission);
            }

            return new TodayMissionResponse(
                toMission(response.mission()),
                toMission(response.fallback()),
                response.missionState(),
                response.restMessage(),
                response.unlockedContacts() == null ? List.of() : response.unlockedContacts(),
                response.vRhythm(),
                response.diagnostics(),
                "AI"
            );
        } catch (Exception exception) {
            return fallback(fallbackMission);
        }
    }

    private TodayMissionResponse fallback(Mission fallbackMission) {
        if (fallbackMission == null) {
            return new TodayMissionResponse(
                null,
                null,
                "gated",
                "Today, rhythm alone is enough.",
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
            null,
            List.of(),
            0.8,
            Map.of("source", "fallback_template"),
            "FALLBACK"
        );
    }

    private Mission toMission(AiMission mission) {
        if (mission == null) {
            return null;
        }

        return new Mission(
            mission.id(),
            mission.missionId(),
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