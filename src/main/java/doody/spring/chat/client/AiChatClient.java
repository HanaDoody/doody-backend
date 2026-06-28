package doody.spring.chat.client;

import doody.spring.chat.dto.ChatMessageRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiChatClient {

    private final String baseUrl;
    private final RestClient restClient;

    public AiChatClient(@Value("${ai.engine.base-url:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = RestClient.builder().build();
    }

    public AiChatResponse message(ChatMessageRequest request) {
        if (baseUrl.isBlank()) {
            return fallback();
        }

        try {
            AiChatResponse response = restClient.post()
                .uri(baseUrl + "/chat/message")
                .body(request)
                .retrieve()
                .body(AiChatResponse.class);
            return response == null ? fallback() : response;
        } catch (Exception exception) {
            return fallback();
        }
    }

    private AiChatResponse fallback() {
        return new AiChatResponse(
            "It is okay to make this smaller today. One tiny step still counts.",
            null,
            "offer_downshift",
            "FALLBACK"
        );
    }

    public record AiChatResponse(
        String reply,
        Map<String, Object> signals,
        String suggested_action,
        String source
    ) {
    }
}