package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthApiController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
        Map<String, Object> res = new HashMap<>();
        User user = userRepository.findByLoginId(username).orElse(null);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            res.put("success", true);
            res.put("message", "로그인 성공");
            // 실제 서비스에서는 JWT 발급 등 추가
        } else {
            res.put("success", false);
            res.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
        }
        return res;
    }
} 