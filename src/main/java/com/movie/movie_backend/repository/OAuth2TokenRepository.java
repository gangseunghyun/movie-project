package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.OAuth2Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuth2TokenRepository extends JpaRepository<OAuth2Token, Long> {
    Optional<OAuth2Token> findByEmailAndProvider(String email, String provider);
    void deleteByEmailAndProvider(String email, String provider);
} 