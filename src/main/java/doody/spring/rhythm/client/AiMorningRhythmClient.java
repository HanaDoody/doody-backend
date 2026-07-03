package doody.spring.rhythm.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.rhythm.dto.MorningRhythmResponse.CollectedDudy;
import doody.spring.rhythm.dto.MorningRhythmResponse.Reward;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiMorningRhythmClient {

    private final String baseUrl;
    private final RestClient restClient;

    public AiMorningRhythmClient(@Value("${AI_ENGINE_BASE_URL:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = RestClient.builder().build();
    }

    public AiMorningResult checkIn(String userId, LocalDateTime timestamp, Short energy) {
        if (baseUrl.isBlank()) {
            return fallback();
        }

        try {
            AiMorningResponse response = restClient.post()
                .uri(baseUrl + "/rhythm/morning")
                .body(new AiMorningRequest(userId, timestamp, energy))
                .retrieve()
                .body(AiMorningResponse.class);

            if (response == null) {
                return fallback();
            }

            Integer hanaMoney = response.reward() == null ? 0 : response.reward().hanaMoney();
            String greeting = response.greeting() == null || response.greeting().isBlank()
                ? "오늘의 아침 리듬이 기록됐어요."
                : response.greeting();
            List<CollectedDudy> collectedDudy = response.collectedDudy() == null
                ? List.of()
                : response.collectedDudy();

            return new AiMorningResult(hanaMoney, greeting, collectedDudy);
        } catch (Exception exception) {
            return fallback();
        }
    }

    private AiMorningResult fallback() {
        return new AiMorningResult(
            20,
            "오늘의 아침 리듬이 기록됐어요.",
            List.of()
        );
    }

    public record AiMorningResult(
        Integer hanaMoney,
        String greeting,
        List<CollectedDudy> collectedDudy
    ) {
    }

    private record AiMorningRequest(
        @JsonProperty("user_id")
        String userId,
        LocalDateTime timestamp,
        Short energy
    ) {
    }

    private record AiMorningResponse(
        Reward reward,
        String greeting,
        @JsonProperty("collected_dudy")
        List<CollectedDudy> collectedDudy
    ) {
    }
}
