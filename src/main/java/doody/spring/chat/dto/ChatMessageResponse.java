package doody.spring.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

public record ChatMessageResponse(
    Long chatLogId,
    String reply,
    Map<String, Object> signals,
    @JsonProperty("suggested_action")
    String suggestedAction,
    LocalDateTime createdAt,
    String source
) {
}