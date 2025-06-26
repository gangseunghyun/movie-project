package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.USRUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthApiController {
    private final USRUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 중복된 로그인 엔드포인트 제거 - UserController의 /api/user-login 사용
    // @PostMapping("/login") 제거됨
} 
