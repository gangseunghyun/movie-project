package com.movie.movie_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.movie_backend.dto.ChatbotResponseDto;
import com.movie.movie_backend.dto.McpToolRequestDto;
import com.movie.movie_backend.dto.McpToolResponseDto;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.dto.OllamaRequestDto;
import com.movie.movie_backend.dto.OllamaResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class McpChatbotService {
    
    @Value("${ollama.model:llama2}")
    private String ollamaModel;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public McpChatbotService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }
    
    public ChatbotResponseDto processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return createGreetingResponse();
        }
        
        try {
            // 1단계: MCP 도구 호출 결정
            List<McpToolRequestDto.ToolCall> toolCalls = determineToolCalls(message);
            
            // 2단계: 도구 실행 및 결과 수집
            List<McpToolResponseDto> toolResults = executeToolCalls(toolCalls);
            
            // 3단계: Ollama로 최종 응답 생성
            String aiResponse = generateResponseWithTools(message, toolResults);
            
            // 4단계: 응답 구성
            List<MovieDetailDto> movies = extractMoviesFromResults(toolResults);
            
            return ChatbotResponseDto.builder()
                    .message(aiResponse)
                    .type("MCP_TOOL_RESPONSE")
                    .movies(movies)
                    .build();
                    
        } catch (Exception e) {
            System.err.println("MCP 처리 중 오류: " + e.getMessage());
            return createFallbackResponse(message);
        }
    }
    
    private List<McpToolRequestDto.ToolCall> determineToolCalls(String message) {
        List<McpToolRequestDto.ToolCall> toolCalls = new ArrayList<>();
        String lowerMessage = message.toLowerCase();
        
        // 영화 정보 검색
        if (lowerMessage.contains("정보") || lowerMessage.contains("알려줘") || 
            lowerMessage.contains("뭐야") || lowerMessage.contains("어떤 영화")) {
            
            String title = extractMovieTitle(message);
            if (title != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("title", title);
                
                toolCalls.add(McpToolRequestDto.ToolCall.builder()
                        .tool("getMovieInfo")
                        .parameters(params)
                        .build());
            }
        }
        
        // 영화 검색
        if (lowerMessage.contains("추천") || lowerMessage.contains("찾아줘") || 
            lowerMessage.contains("보여줘") || lowerMessage.contains("알려줘")) {
            
            Map<String, Object> params = new HashMap<>();
            
            // 장르별 검색
            String[] genres = {"로맨스", "액션", "코미디", "스릴러", "드라마", "sf", "판타지", "호러", "애니메이션", "다큐멘터리"};
            for (String genre : genres) {
                if (lowerMessage.contains(genre)) {
                    params.put("genre", genre);
                    break;
                }
            }
            
            // 상황별 검색
            if (lowerMessage.contains("우울") || lowerMessage.contains("기분")) {
                params.put("situation", "우울할 때");
            } else if (lowerMessage.contains("친구")) {
                params.put("situation", "친구와 함께");
            } else if (lowerMessage.contains("연인")) {
                params.put("situation", "연인과 함께");
            } else if (lowerMessage.contains("가족")) {
                params.put("situation", "가족과 함께");
            } else if (lowerMessage.contains("힐링")) {
                params.put("situation", "힐링");
            }
            
            // 타입별 검색
            if (lowerMessage.contains("인기") || lowerMessage.contains("인기있는")) {
                params.put("type", "인기");
            } else if (lowerMessage.contains("개봉예정") || lowerMessage.contains("곧")) {
                params.put("type", "개봉예정");
            }
            
            if (!params.isEmpty()) {
                toolCalls.add(McpToolRequestDto.ToolCall.builder()
                        .tool("searchMovies")
                        .parameters(params)
                        .build());
            }
        }
        
        return toolCalls;
    }
    
    private String extractMovieTitle(String message) {
        // 따옴표로 감싸진 영화 제목 찾기
        Pattern pattern = Pattern.compile("[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 일반적인 영화 제목 패턴 찾기
        String[] words = message.split(" ");
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].length() > 1 && !isStopWord(words[i])) {
                return words[i];
            }
        }
        
        return null;
    }
    
    private boolean isStopWord(String word) {
        String[] stopWords = {"영화", "추천", "알려줘", "정보", "뭐야", "이", "그", "저", "좋은", "재밌는"};
        for (String stopWord : stopWords) {
            if (word.equals(stopWord)) {
                return true;
            }
        }
        return false;
    }
    
    private List<McpToolResponseDto> executeToolCalls(List<McpToolRequestDto.ToolCall> toolCalls) {
        List<McpToolResponseDto> results = new ArrayList<>();
        
        for (McpToolRequestDto.ToolCall toolCall : toolCalls) {
            try {
                McpToolRequestDto request = McpToolRequestDto.builder()
                        .tool(toolCall.getTool())
                        .parameters(toolCall.getParameters())
                        .build();
                
                McpToolResponseDto response = webClient.post()
                        .uri("/api/mcp/tools/" + toolCall.getTool())
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(McpToolResponseDto.class)
                        .block();
                
                if (response != null) {
                    results.add(response);
                    toolCall.setResult(objectMapper.writeValueAsString(response.getResult()));
                }
                
            } catch (Exception e) {
                System.err.println("도구 실행 실패: " + e.getMessage());
            }
        }
        
        return results;
    }
    
    private String generateResponseWithTools(String userMessage, List<McpToolResponseDto> toolResults) {
        try {
            // 도구 결과를 포함한 프롬프트 생성
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(createMcpSystemPrompt());
            promptBuilder.append("\n\n사용자: ").append(userMessage);
            
            if (!toolResults.isEmpty()) {
                promptBuilder.append("\n\n도구 실행 결과:");
                for (McpToolResponseDto result : toolResults) {
                    if (result.isSuccess()) {
                        promptBuilder.append("\n- ").append(result.getTool()).append(": 성공");
                        if (result.getResult() instanceof McpToolResponseDto.MovieSearchResult) {
                            McpToolResponseDto.MovieSearchResult searchResult = (McpToolResponseDto.MovieSearchResult) result.getResult();
                            promptBuilder.append(" (").append(searchResult.getCount()).append("개 영화 발견)");
                        }
                    } else {
                        promptBuilder.append("\n- ").append(result.getTool()).append(": 실패 - ").append(result.getError());
                    }
                }
            }
            
            promptBuilder.append("\n\n챗봇:");
            
            // Ollama API 호출
            OllamaRequestDto request = OllamaRequestDto.builder()
                    .model(ollamaModel)
                    .prompt(promptBuilder.toString())
                    .stream(false)
                    .options(OllamaRequestDto.OllamaOptions.builder()
                            .temperature(0.7)
                            .numPredict(400)
                            .build())
                    .build();
            
            OllamaResponseDto response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponseDto.class)
                    .block();
            
            if (response != null && response.getResponse() != null) {
                return response.getResponse().trim();
            } else {
                throw new RuntimeException("Ollama 응답이 비어있습니다.");
            }
            
        } catch (Exception e) {
            System.err.println("Ollama 응답 생성 실패: " + e.getMessage());
            return createFallbackResponse(userMessage).getMessage();
        }
    }
    
    private String createMcpSystemPrompt() {
        return """
            당신은 MCP(Model Context Protocol) 기반 영화 추천 전문가입니다.
            도구 실행 결과를 바탕으로 정확하고 유용한 영화 추천을 제공하세요.
            
            응답 스타일:
            - 친근하고 자연스러운 톤으로 대화하세요
            - 이모지를 적절히 사용하세요 (🎬, ⭐, 💡, 🎭 등)
            - 도구 실행 결과를 바탕으로 구체적인 추천을 해주세요
            - 영화에 대한 간단한 설명이나 추천 이유를 포함하세요
            
            도구 사용 가이드:
            - searchMovies: 장르, 상황, 타입별 영화 검색
            - getMovieInfo: 영화 상세 정보 조회
            
            응답 예시:
            - 도구 성공 시: "로맨스 영화를 찾아드렸어요! 💕 총 5개의 따뜻한 로맨스 영화를 추천해드릴게요..."
            - 도구 실패 시: "죄송해요. 해당 영화를 찾을 수 없어요. 다른 영화를 추천해드릴까요?"
            """;
    }
    
    private List<MovieDetailDto> extractMoviesFromResults(List<McpToolResponseDto> toolResults) {
        List<MovieDetailDto> movies = new ArrayList<>();
        
        for (McpToolResponseDto result : toolResults) {
            if (result.isSuccess() && result.getResult() instanceof McpToolResponseDto.MovieSearchResult) {
                McpToolResponseDto.MovieSearchResult searchResult = (McpToolResponseDto.MovieSearchResult) result.getResult();
                movies.addAll(searchResult.getMovies());
            } else if (result.isSuccess() && result.getResult() instanceof McpToolResponseDto.MovieInfoResult) {
                McpToolResponseDto.MovieInfoResult infoResult = (McpToolResponseDto.MovieInfoResult) result.getResult();
                movies.add(infoResult.getMovie());
            }
        }
        
        return movies;
    }
    
    private ChatbotResponseDto createGreetingResponse() {
        return ChatbotResponseDto.builder()
                .message("안녕하세요! MCP 기반 AI 영화 챗봇입니다! 🤖\n\n자연어로 영화에 대해 무엇이든 물어보세요!\n\n예시:\n• \"오늘 기분이 우울한데 영화 추천해줘\"\n• \"로맨스 영화 중에서 가장 재밌는 거 뭐야?\"\n• \"인터스텔라 영화에 대해 자세히 알려줘\"\n\n💡 MCP 도구를 사용하여 정확한 영화 정보를 제공합니다!")
                .type("GREETING")
                .build();
    }
    
    private ChatbotResponseDto createFallbackResponse(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("로맨스")) {
            return ChatbotResponseDto.builder()
                    .message("로맨스 영화를 찾고 계시는군요! 💕 따뜻하고 설레는 로맨스 영화들을 추천해드릴게요!")
                    .type("RECOMMENDATION")
                    .build();
        } else if (lowerMessage.contains("액션")) {
            return ChatbotResponseDto.builder()
                    .message("액션 영화를 원하시는군요! 🔥 스릴 넘치는 액션 영화들을 추천해드릴게요!")
                    .type("RECOMMENDATION")
                    .build();
        }
        
        return ChatbotResponseDto.builder()
                .message("죄송합니다. 현재 MCP 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요. 😅")
                .type("ERROR")
                .build();
    }
} 