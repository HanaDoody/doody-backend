package doody.spring.mission.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.mission.dto.TodayMissionResponse.AriVector;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiMissionActionClient {

    private final String baseUrl;
    private final RestClient restClient;

    public AiMissionActionClient(@Value("${AI_ENGINE_BASE_URL:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = RestClient.builder().build();
    }

    public MissionCompleteResult complete(MissionCompleteRequest request) {
        if (baseUrl.isBlank()) {
            return fallbackComplete(request);
        }

        try {
            MissionCompleteResult result = restClient.post()
                .uri(baseUrl + "/mission/complete")
                .body(request)
                .retrieve()
                .body(MissionCompleteResult.class);
            return result == null ? fallbackComplete(request) : result;
        } catch (Exception exception) {
            return fallbackComplete(request);
        }
    }

    public MissionRejectResult reject(MissionRejectAiRequest request) {
        if (baseUrl.isBlank()) {
            return fallbackReject(request);
        }

        try {
            MissionRejectResult result = restClient.post()
                .uri(baseUrl + "/mission/reject")
                .body(request)
                .retrieve()
                .body(MissionRejectResult.class);
            return result == null ? fallbackReject(request) : result;
        } catch (Exception exception) {
            return fallbackReject(request);
        }
    }

    private MissionCompleteResult fallbackComplete(MissionCompleteRequest request) {
        AriVector delta = new AriVector(0.0, 0.03, 0.0);
        AriVector updated = new AriVector(
            request.currentAri() == null ? 0.8 : request.currentAri().rhythm(),
            Math.min((request.currentAri() == null ? 0.2 : request.currentAri().autonomy()) + 0.03, 1.0),
            request.currentAri() == null ? 0.1 : request.currentAri().connection()
        );
        return new MissionCompleteResult(updated, delta, 1.0, new Reward(0), true, false, false, false, List.of(), List.of(), "좋아. 작은 행동 하나가 오늘의 자율감을 조금 열었어.");
    }

    private MissionRejectResult fallbackReject(MissionRejectAiRequest request) {
        return new MissionRejectResult(
            "downshift",
            "괜찮아. 오늘은 더 쉬운 버전으로 바꿔볼게.",
            true,
            List.of(),
            new RejectDiagnostics(request.missionId())
        );
    }

    public record MissionCompleteRequest(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("mission_id")
        String missionId,
        @JsonProperty("current_ari")
        AriVector currentAri,
        Short energy
    ) {
    }

    public record MissionCompleteResult(
        @JsonProperty("updated_ari")
        AriVector updatedAri,
        @JsonProperty("applied_delta")
        AriVector appliedDelta,
        Double eta,
        Reward reward,
        Boolean completed,
        @JsonProperty("signature_available")
        Boolean signatureAvailable,
        @JsonProperty("signature_completed")
        Boolean signatureCompleted,
        @JsonProperty("contact_unlocked")
        Boolean contactUnlocked,
        @JsonProperty("collected_dudy")
        List<Dudy> collectedDudy,
        @JsonProperty("unlocked_contacts")
        List<Contact> unlockedContacts,
        String message
    ) {
    }

    public record Reward(
        @JsonProperty("hana_money")
        Integer hanaMoney
    ) {
    }

    public record MissionRejectAiRequest(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("mission_id")
        String missionId,
        @JsonProperty("reason_text")
        String reasonText
    ) {
    }

    public record MissionRejectResult(
        String action,
        String message,
        @JsonProperty("rest_option")
        Boolean restOption,
        List<MissionCandidate> candidates,
        RejectDiagnostics diagnostics
    ) {
    }

    public record MissionCandidate(
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

    public record RejectDiagnostics(
        @JsonProperty("rejected_mission_id")
        String rejectedMissionId
    ) {
    }

    public record Dudy(
        String id,
        String tier,
        String axis,
        @JsonProperty("earned_reason")
        String earnedReason
    ) {
    }

    public record Contact(
        String id,
        String title,
        String axis
    ) {
    }
}
