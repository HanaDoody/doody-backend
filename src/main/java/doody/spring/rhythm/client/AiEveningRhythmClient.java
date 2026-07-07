package doody.spring.rhythm.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.rhythm.dto.EveningRhythmResponse.CollectedDudy;
import doody.spring.rhythm.dto.EveningRhythmResponse.Reward;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiEveningRhythmClient {

    private final String baseUrl;
    private final RestClient restClient;

    public AiEveningRhythmClient(
        @Value("${AI_ENGINE_BASE_URL:}") String baseUrl,
        @Qualifier("aiEngineRestClient") RestClient restClient
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = restClient;
    }

    public AiEveningResult leaveNote(String userId, LocalDateTime timestamp, String text) {
        if (baseUrl.isBlank()) {
            return fallback();
        }

        try {
            AiEveningResponse response = restClient.post()
                .uri(baseUrl + "/rhythm/evening")
                .body(new AiEveningRequest(userId, timestamp, text))
                .retrieve()
                .body(AiEveningResponse.class);

            if (response == null) {
                return fallback();
            }

            Integer hanaMoney = response.reward() == null ? 0 : response.reward().hanaMoney();
            String reply = response.reply() == null || response.reply().isBlank()
                ? "오늘도 잘 마무리했어. 이 기록이 내일의 기준이 될 거야."
                : response.reply();
            List<CollectedDudy> collectedDudy = response.collectedDudy() == null
                ? List.of()
                : response.collectedDudy().stream()
                    .map(dudy -> new CollectedDudy(
                        dudy.id(),
                        dudy.tier(),
                        dudy.axis(),
                        dudy.earnedReason()
                    ))
                    .toList();

            return new AiEveningResult(hanaMoney, reply, null, collectedDudy);
        } catch (Exception exception) {
            return fallback();
        }
    }

    private AiEveningResult fallback() {
        return new AiEveningResult(
            30,
            "오늘도 잘 마무리했어. 이 기록이 내일의 기준이 될 거야.",
            null,
            List.of()
        );
    }

    public record AiEveningResult(
        Integer hanaMoney,
        String reply,
        String signals,
        List<CollectedDudy> collectedDudy
    ) {
    }

    private record AiEveningRequest(
        @JsonProperty("user_id")
        String userId,
        LocalDateTime timestamp,
        String text
    ) {
    }

    private record AiEveningResponse(
        Reward reward,
        String reply,
        String signals,
        @JsonProperty("collected_dudy")
        List<AiCollectedDudy> collectedDudy
    ) {
    }

    private record AiCollectedDudy(
        String id,
        String tier,
        String axis,
        @JsonProperty("earned_reason")
        String earnedReason
    ) {
    }
}
