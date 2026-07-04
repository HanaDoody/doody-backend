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

    public AiChatClient(@Value("${AI_ENGINE_BASE_URL:}") String baseUrl) {
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
            "오늘은 더 작게 해도 괜찮아. 아주 작은 한 걸음도 충분히 의미 있어.",
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
