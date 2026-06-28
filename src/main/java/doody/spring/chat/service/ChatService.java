package doody.spring.chat.service;

import doody.spring.chat.client.AiChatClient;
import doody.spring.chat.client.AiChatClient.AiChatResponse;
import doody.spring.chat.dto.ChatMessageRequest;
import doody.spring.chat.dto.ChatMessageResponse;
import doody.spring.domain.entity.ChatLog;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.ChatLogRepository;
import doody.spring.domain.repository.UserRepository;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final ChatLogRepository chatLogRepository;
    private final AiChatClient aiChatClient;

    public ChatService(
        UserRepository userRepository,
        ChatLogRepository chatLogRepository,
        AiChatClient aiChatClient
    ) {
        this.userRepository = userRepository;
        this.chatLogRepository = chatLogRepository;
        this.aiChatClient = aiChatClient;
    }

    @Transactional
    public ChatMessageResponse message(ChatMessageRequest request) {
        validate(request);
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));

        AiChatResponse aiResponse = aiChatClient.message(request);
        ChatLog chatLog = chatLogRepository.save(ChatLog.create(
            user,
            request.message(),
            aiResponse.reply(),
            request.context() == null ? null : request.context().currentMissionId(),
            toJson(aiResponse.signals()),
            aiResponse.suggested_action()
        ));

        return new ChatMessageResponse(
            chatLog.getId(),
            aiResponse.reply(),
            aiResponse.signals(),
            aiResponse.suggested_action(),
            chatLog.getCreatedAt(),
            aiResponse.source() == null ? "AI" : aiResponse.source()
        );
    }

    private void validate(ChatMessageRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id is required.");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required.");
        }
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.entrySet().stream()
            .map(entry -> "\"" + escape(entry.getKey()) + "\":\"" + escape(String.valueOf(entry.getValue())) + "\"")
            .collect(Collectors.joining(",", "{", "}"));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}