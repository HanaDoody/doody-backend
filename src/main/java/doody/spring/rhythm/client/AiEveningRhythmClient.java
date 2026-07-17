package doody.spring.rhythm.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.rhythm.dto.EveningRhythmResponse.CollectedDudy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiEveningRhythmClient {

    private static final Logger log = LoggerFactory.getLogger(AiEveningRhythmClient.class);

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
                .body(new AiEveningRequest(userId, text))
                .retrieve()
                .body(AiEveningResponse.class);

            if (response == null) {
                return fallback();
            }

            Integer hanaMoney = response.reward() == null ? 0 : response.reward().hanaMoney();
            String reply = response.reply() == null || response.reply().isBlank()
                ? "오늘 하루도 수고 많았어. 이야기해줘서 고마워."
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

            String signals = response.signals() == null ? null : response.signals().toString();
            return new AiEveningResult(hanaMoney, reply, signals, collectedDudy);
        } catch (Exception exception) {
            log.warn("AI evening rhythm request failed; using fallback reply: {}", exception.getMessage());
            return fallback();
        }
    }

    private AiEveningResult fallback() {
        return new AiEveningResult(
            20,
            "오늘 하루도 수고 많았어. 이야기해줘서 고마워.",
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
        String text
    ) {
    }

    private record AiEveningResponse(
        AiReward reward,
        String reply,
        Map<String, Double> signals,
        @JsonProperty("collected_dudy")
        List<AiCollectedDudy> collectedDudy
    ) {
    }

    private record AiReward(
        @JsonProperty("hana_money")
        Integer hanaMoney
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
