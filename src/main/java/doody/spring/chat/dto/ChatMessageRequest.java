package doody.spring.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ChatMessageRequest(
    @JsonProperty("user_id")
    String userId,
    String message,
    ChatContext context
) {

    public record ChatContext(
        @JsonProperty("current_ari")
        Map<String, Object> currentAri,
        @JsonProperty("current_mission_id")
        String currentMissionId,
        Integer energy
    ) {
    }
}