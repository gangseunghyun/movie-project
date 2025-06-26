import React from 'react';

function SocialLoginPage() {
  return (
    <div>
      <h2>소셜 로그인 테스트</h2>
      <a href="http://localhost:8080/oauth2/authorization/google">
        <button>구글 로그인</button>
      </a>
      <a href="http://localhost:8080/oauth2/authorization/naver">
        <button>네이버 로그인</button>
      </a>
      <a href="http://localhost:8080/oauth2/authorization/kakao">
        <button>카카오 로그인</button>
      </a>
    </div>
  );
}

export default SocialLoginPage; 