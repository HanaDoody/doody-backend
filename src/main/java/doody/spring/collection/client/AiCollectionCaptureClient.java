package doody.spring.collection.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import doody.spring.collection.dto.CollectionCaptureResponse;
import doody.spring.collection.dto.CollectionCaptureResponse.AriVector;
import doody.spring.collection.dto.CollectionCaptureResponse.Contact;
import doody.spring.collection.dto.CollectionCaptureResponse.Dudy;
import doody.spring.collection.dto.CollectionCaptureResponse.Reward;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiCollectionCaptureClient {

    private final String baseUrl;
    private final RestClient restClient;

    public AiCollectionCaptureClient(@Value("${AI_ENGINE_BASE_URL:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = RestClient.builder().build();
    }

    public AiCaptureResult capture(AiCaptureRequest request, Dudy fallbackDudy) {
        if (baseUrl.isBlank()) {
            return fallback(fallbackDudy);
        }

        try {
            AiCaptureResponse response = restClient.post()
                .uri(baseUrl + "/collection/capture")
                .body(request)
                .retrieve()
                .body(AiCaptureResponse.class);

            if (response == null) {
                return fallback(fallbackDudy);
            }

            return new AiCaptureResult(
                response.dudy() == null ? fallbackDudy : response.dudy(),
                response.reward() == null ? new Reward(0) : response.reward(),
                response.updatedAri(),
                response.appliedDelta(),
                response.collectedDudy() == null ? List.of() : response.collectedDudy(),
                response.unlockedContacts() == null ? List.of() : response.unlockedContacts()
            );
        } catch (Exception exception) {
            return fallback(fallbackDudy);
        }
    }

    private AiCaptureResult fallback(Dudy fallbackDudy) {
        AriVector updatedAri = new AriVector(0.8, 0.3, 0.4);
        AriVector appliedDelta = new AriVector(0.0, 0.0, 0.05);
        Dudy dudy = fallbackDudy == null ? null : fallbackDudy;
        return new AiCaptureResult(
            dudy,
            new Reward(30),
            updatedAri,
            appliedDelta,
            dudy == null ? List.of() : List.of(dudy),
            List.of()
        );
    }

    public record AiCaptureResult(
        Dudy dudy,
        Reward reward,
        AriVector updatedAri,
        AriVector appliedDelta,
        List<Dudy> collectedDudy,
        List<Contact> unlockedContacts
    ) {
    }

    public record AiCaptureRequest(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("pin_id")
        String pinId,
        Location location,
        @JsonProperty("current_ari")
        AriVector currentAri,
        AriVector goal,
        Short energy
    ) {
    }

    public record Location(
        BigDecimal lat,
        BigDecimal lng
    ) {
    }

    private record AiCaptureResponse(
        Dudy dudy,
        Reward reward,
        @JsonProperty("updated_ari")
        AriVector updatedAri,
        @JsonProperty("applied_delta")
        AriVector appliedDelta,
        @JsonProperty("collected_dudy")
        List<Dudy> collectedDudy,
        @JsonProperty("unlocked_contacts")
        List<Contact> unlockedContacts
    ) {
    }
}