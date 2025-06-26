package com.movie.repository;

import com.movie.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PRDActorRepository extends JpaRepository<Actor, Long> {
    Optional<Actor> findByName(String name);
} 