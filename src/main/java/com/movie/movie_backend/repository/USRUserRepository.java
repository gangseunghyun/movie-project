package com.movie.repository;

import com.movie.entity.User;
import com.movie.constant.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface USRUserRepository extends JpaRepository<User, Long> {
    // 기본 조회 메서드들
    Optional<User> findByEmail(String email);
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByNickname(String nickname);
    
    // 존재 여부 확인
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);
    boolean existsByNickname(String nickname);
    
    // 소셜 로그인 관련
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
    Optional<User> findByEmailAndProvider(String email, Provider provider);
    
    // String 타입 provider 지원 (기존 코드 호환성)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // 이메일 인증 관련
    List<User> findByEmailVerified(boolean emailVerified);
    
    // 권한별 조회
    List<User> findByRole(com.movie.constant.UserRole role);
    
    // 소셜 회원가입 완료 여부
    List<User> findBySocialJoinCompleted(boolean socialJoinCompleted);
    
    // 복합 검색
    @Query("SELECT u FROM User u WHERE u.email LIKE %:keyword% OR u.nickname LIKE %:keyword% OR u.loginId LIKE %:keyword%")
    List<User> findByKeyword(@Param("keyword") String keyword);
    
    // 최근 가입자 조회
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecentUsers();
    
    // 기존 메서드 호환성을 위한 별칭 (deprecated)
    @Deprecated
    default Optional<User> findByUsername(String username) {
        return findByLoginId(username);
    }
    
    @Deprecated
    default boolean existsByUsername(String username) {
        return existsByLoginId(username);
    }
}