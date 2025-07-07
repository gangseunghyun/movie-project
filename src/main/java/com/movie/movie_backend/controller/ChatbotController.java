package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.ChatbotRequestDto;
import com.movie.movie_backend.dto.ChatbotResponseDto;
import com.movie.movie_backend.service.McpChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Autowired
    private McpChatbotService mcpChatbotService;

    @PostMapping("/chat")
    public ResponseEntity<ChatbotResponseDto> chat(@RequestBody ChatbotRequestDto request) {
        ChatbotResponseDto response = mcpChatbotService.processMessage(request.getMessage());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions() {
        List<String> suggestions = List.of(
            "로맨스 영화 추천해줘",
            "액션 영화 추천해줘",
            "코미디 영화 추천해줘",
            "최신 영화 추천해줘",
            "평점 높은 영화 추천해줘",
            "인기 영화 추천해줘"
        );
        return ResponseEntity.ok(suggestions);
    }
} 