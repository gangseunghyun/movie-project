package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.USRUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class USRUserDetailServiceImpl implements UserDetailsService {

    private final USRUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. 아이디: " + loginId));
        
        // User 엔티티가 UserDetails를 구현하므로 직접 반환
        return user;
    }
} 
