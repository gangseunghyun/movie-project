package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.UserJoinRequestDto;
import com.movie.movie_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController // 이 클래스가 RESTful 웹 서비스의 컨트롤러임을 나타냅니다. (데이터를 반환)
@RequestMapping("/api/users") // 이 컨트롤러의 모든 메소드는 '/api/users' 라는 기본 URL 경로를 갖습니다.
@RequiredArgsConstructor // final이 붙은 필드를 인자로 받는 생성자를 자동으로 만들어줍니다. (의존성 주입)
public class UserController {

    private final UserService userService; // 생성자를 통해 UserService 의존성을 주입받습니다.

    @PostMapping("/join")
    public ResponseEntity<String> join(@Valid @RequestBody UserJoinRequestDto requestDto, BindingResult bindingResult) {
        // 유효성 검사 실패 시, 에러 메시지를 조합하여 반환
        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            bindingResult.getAllErrors().forEach(error -> {
                sb.append(error.getDefaultMessage()).append("\n");
            });
            return ResponseEntity.badRequest().body(sb.toString());
        }

        try {
            userService.join(requestDto);
            // 성공 시, HTTP 200 OK 상태 코드와 함께 성공 메시지를 반환합니다.
            return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            // UserService에서 발생시킨 중복 예외를 잡아서,
            // HTTP 400 Bad Request 상태 코드와 함께 에러 메시지를 반환합니다.
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/check-login-id")
    public ResponseEntity<String> checkLoginId(@RequestParam String loginId) {
        try {
            boolean isAvailable = userService.isLoginIdAvailable(loginId);
            if (isAvailable) {
                return ResponseEntity.ok("사용 가능한 아이디입니다.");
            } else {
                return ResponseEntity.badRequest().body("이미 사용 중인 아이디입니다.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 앞으로 이곳에 회원가입, 로그인, 정보 조회 등
    // 실제 웹 요청을 처리하는 메소드들을 만들어 나갈 겁니다.
    //
    // 예를 들면 이런 모습이 될 거예요.
    //
    // @PostMapping("/join")
    // public ResponseEntity<String> join(@RequestBody UserJoinRequestDto requestDto) {
    //     userService.join(requestDto);
    //     return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    // }

} 