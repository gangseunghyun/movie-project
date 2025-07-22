# Flimer - 영화 예매 플랫폼

## 결제 기능 연동 가이드

### 지원하는 결제 수단
- **나이스페이 (N pay)**: 네이버페이 연동
- **카카오페이**: 카카오페이 결제
- **토스페이**: 토스페이먼츠 결제
- **신용카드**: 나이스페이/이니시스 카드 결제

### 환경 설정

1. **환경 변수 설정**
   ```bash
   # .env 파일 생성
   cp env.example .env
   ```

2. **필요한 API 키 설정**
   - 나이스페이: https://www.nicepay.co.kr/
   - 카카오페이: https://developers.kakao.com/docs/latest/ko/kakaopay/
   - 토스페이: https://docs.tosspayments.com/
   - KG이니시스: https://www.inicis.com/

3. **환경 변수 예시**
   ```env
   # API 서버 URL
   REACT_APP_API_URL=http://localhost:8090

   # 나이스페이 설정
   REACT_APP_NICE_CLIENT_ID=your_nice_client_id
   REACT_APP_NICE_CLIENT_SECRET=your_nice_client_secret

   # 카카오페이 설정
   REACT_APP_KAKAO_ADMIN_KEY=your_kakao_admin_key

   # 토스페이 설정
   REACT_APP_TOSS_SECRET_KEY=your_toss_secret_key
   REACT_APP_TOSS_CLIENT_KEY=your_toss_client_key

   # KG이니시스 설정 (신용카드)
   REACT_APP_INI_MERCHANT_ID=your_ini_merchant_id
   ```

### 결제 프로세스

1. **결제 요청**: 사용자가 결제 수단 선택 후 결제 버튼 클릭
2. **PG사 연동**: 선택된 결제 수단에 따라 해당 PG사 API 호출
3. **결제 처리**: PG사에서 결제 승인/거부 처리
4. **결과 처리**: 성공/실패에 따른 페이지 이동

### 파일 구조

```
src/
├── services/
│   ├── paymentConfig.js      # 결제 설정
│   └── PaymentService.js     # 결제 API 서비스
├── components/
│   ├── BookingPage/
│   │   └── PaymentModal.jsx  # 결제 모달
│   └── PaymentResult/
│       ├── PaymentSuccess.jsx # 결제 성공 페이지
│       ├── PaymentFail.jsx    # 결제 실패 페이지
│       └── PaymentResult.module.css
```

### 개발 가이드

1. **테스트 모드**: 각 PG사에서 제공하는 테스트 환경 사용
2. **웹훅 설정**: 결제 완료 후 서버로 알림을 받기 위한 웹훅 URL 설정
3. **에러 처리**: 네트워크 오류, 결제 거부 등 다양한 에러 상황 처리
4. **보안**: API 키는 환경 변수로 관리, 클라이언트에 노출 금지

### 주의사항

- 실제 결제 연동을 위해서는 각 PG사와 계약이 필요합니다
- 테스트 환경에서 충분한 테스트 후 운영 환경으로 전환하세요
- 결제 관련 로그는 반드시 보관하여 문제 발생 시 추적 가능하도록 하세요
- PCI DSS 규정을 준수하여 카드 정보 보안을 유지하세요