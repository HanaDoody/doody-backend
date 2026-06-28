package doody.spring.chat.controller;

import doody.spring.chat.dto.ChatMessageRequest;
import doody.spring.chat.dto.ChatMessageResponse;
import doody.spring.chat.service.ChatService;
import doody.spring.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ApiResponse<ChatMessageResponse> message(@RequestBody ChatMessageRequest request) {
        return ApiResponse.success(HttpStatus.OK, "chat message success.", chatService.message(request));
    }
}