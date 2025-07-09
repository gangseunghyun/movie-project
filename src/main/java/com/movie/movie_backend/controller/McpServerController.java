package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.McpToolRequestDto;
import com.movie.movie_backend.dto.McpToolResponseDto;
import com.movie.movie_backend.service.McpChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mcp/tools")
@CrossOrigin(origins = "*")
public class McpServerController {

    @Autowired
    private McpChatbotService mcpChatbotService;

    @PostMapping("/searchMovies")
    public ResponseEntity<McpToolResponseDto> searchMovies(@RequestBody McpToolRequestDto request) {
        try {
            McpToolResponseDto response = mcpChatbotService.handleMcpToolRequest("searchMovies", request.getParameters());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/getMovieInfo")
    public ResponseEntity<McpToolResponseDto> getMovieInfo(@RequestBody McpToolRequestDto request) {
        try {
            McpToolResponseDto response = mcpChatbotService.handleMcpToolRequest("getMovieInfo", request.getParameters());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailableTools() {
        Map<String, Object> tools = Map.of(
            "tools", Map.of(
                "searchMovies", Map.of(
                    "description", "영화 제목으로 영화를 검색합니다",
                    "parameters", Map.of(
                        "query", "검색할 영화 제목이나 키워드"
                    )
                ),
                "getMovieInfo", Map.of(
                    "description", "영화 코드로 상세 정보를 조회합니다",
                    "parameters", Map.of(
                        "movieCd", "영화 코드"
                    )
                )
            )
        );
        return ResponseEntity.ok(tools);
    }
} 