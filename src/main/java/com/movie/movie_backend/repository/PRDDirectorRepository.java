package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Director;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PRDDirectorRepository extends JpaRepository<Director, Long> {
    Optional<Director> findByName(String name);
} 
