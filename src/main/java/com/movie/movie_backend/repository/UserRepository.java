package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByLoginId(String loginId);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByLoginId(String loginId);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
} 