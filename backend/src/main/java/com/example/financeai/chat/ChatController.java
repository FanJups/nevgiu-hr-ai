package com.example.financeai.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ChatController {

    private final ChatService chatService;
    private final DocumentService documentService;

    public ChatController(ChatService chatService,
                          DocumentService documentService) {
        this.chatService = chatService;
        this.documentService = documentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String answer = chatService.ask(request.getMessage());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @PostMapping(value = "/docs", consumes = "text/plain")
    public ResponseEntity<String> uploadDocument(@RequestBody String text) {
        documentService.storeText(text);
        return ResponseEntity.ok("Document stored");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

}
