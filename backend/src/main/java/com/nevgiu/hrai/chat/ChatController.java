package com.nevgiu.hrai.chat;

import com.nevgiu.hrai.chat.dto.ChatRequestDto;
import com.nevgiu.hrai.chat.dto.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
        return chatService.handleWebMessage(request);
    }
}
