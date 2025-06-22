package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // 이 인터페이스가 Repository(저장소) 역할을 한다는 것을 Spring에 알려주는 딱지입니다.
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository를 상속받는 것만으로도 기본적인 CRUD(Create, Read, Update, Delete) 기능이 자동 생성됩니다.
    // 앞으로 이곳에는 우리가 직접 만들어야 하는 특별한 조회 메소드들을 추가하게 됩니다.
    // 예를 들어, '사용자 이름으로 User 찾기' 같은 기능 말이죠!

    // 쿼리 메소드: 메소드 이름을 분석하여 JPA가 자동으로 쿼리를 생성하고 실행합니다.
    // Optional<User>: 반환 타입. 결과가 null일 수도 있는 경우, NPE(NullPointerException)를 방지하기 위해 Optional로 감싸줍니다.

    // 아이디로 User를 찾는 메소드
    Optional<User> findByLoginId(String loginId);

    // 이메일로 User를 찾는 메소드
    Optional<User> findByEmail(String email);
} 