// 결제 서비스 설정
export const PAYMENT_CONFIG = {
  // 아임포트 설정 (통합 결제)
  IAMPORT: {
    IMP_CODE: process.env.REACT_APP_IAMPORT_CODE || 'imp00000000',
    MERCHANT_UID: 'merchant_' + new Date().getTime(),
  },
  
  // 나이스페이 설정
  NICE_PAY: {
    CLIENT_ID: process.env.REACT_APP_NICE_CLIENT_ID || '',
    CLIENT_SECRET: process.env.REACT_APP_NICE_CLIENT_SECRET || '',
    API_URL: 'https://api.nicepay.co.kr',
  },
  
  // 카카오페이 설정
  KAKAO_PAY: {
    ADMIN_KEY: process.env.REACT_APP_KAKAO_ADMIN_KEY || '',
    API_URL: 'https://kapi.kakao.com',
  },
  
  // 토스페이 설정
  TOSS_PAY: {
    SECRET_KEY: process.env.REACT_APP_TOSS_SECRET_KEY || '',
    CLIENT_KEY: process.env.REACT_APP_TOSS_CLIENT_KEY || '',
    API_URL: 'https://api.tosspayments.com',
  },
  
  // 신용카드 설정 (PG사별)
  CREDIT_CARD: {
    // KG이니시스
    INI_PAY: {
      MERCHANT_ID: process.env.REACT_APP_INI_MERCHANT_ID || '',
      API_URL: 'https://iniapi.inicis.com',
    },
    // 나이스페이
    NICE_PAY_CARD: {
      CLIENT_ID: process.env.REACT_APP_NICE_CLIENT_ID || '',
      CLIENT_SECRET: process.env.REACT_APP_NICE_CLIENT_SECRET || '',
      API_URL: 'https://api.nicepay.co.kr',
    }
  }
};

// 결제 수단별 설정
export const PAYMENT_METHODS = {
  naverpay: {
    name: 'N pay',
    pg: 'naverpay',
    method: 'naverpay'
  },
  kakaopay: {
    name: '카카오페이',
    pg: 'kakaopay',
    method: 'kakaopay'
  },
  tosspay: {
    name: '토스페이',
    pg: 'tosspay',
    method: 'tosspay'
  },
  card: {
    name: '신용카드',
    pg: 'nicepay', // 또는 'inicis'
    method: 'card'
  }
}; 