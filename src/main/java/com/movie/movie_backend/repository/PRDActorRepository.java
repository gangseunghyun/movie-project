package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PRDActorRepository extends JpaRepository<Actor, Long> {
    Optional<Actor> findByName(String name);
    List<Actor> findByNameContainingIgnoreCase(String name);
} 
