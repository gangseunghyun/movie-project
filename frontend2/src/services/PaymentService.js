import axios from 'axios';
import { PAYMENT_CONFIG, PAYMENT_METHODS } from './paymentConfig';

class PaymentService {
  constructor() {
    this.baseURL = process.env.REACT_APP_API_URL || '';
  }

  // 결제 요청 생성
  async createPayment(paymentData) {
    try {
      const response = await axios.post(`${this.baseURL}/api/payments/create`, paymentData);
      return response.data;
    } catch (error) {
      console.error('결제 생성 오류:', error);
      throw error;
    }
  }

  // 결제 상태 확인
  async checkPaymentStatus(paymentId) {
    try {
      const response = await axios.get(`${this.baseURL}/api/payments/${paymentId}/status`);
      return response.data;
    } catch (error) {
      console.error('결제 상태 확인 오류:', error);
      throw error;
    }
  }

  // 결제 취소
  async cancelPayment(paymentId, reason) {
    try {
      const response = await axios.post(`${this.baseURL}/api/payments/${paymentId}/cancel`, {
        reason: reason
      });
      return response.data;
    } catch (error) {
      console.error('결제 취소 오류:', error);
      throw error;
    }
  }

  // 나이스페이 결제
  async nicePayPayment(paymentData) {
    try {
      const response = await axios.post(`${PAYMENT_CONFIG.NICE_PAY.API_URL}/v1/payments`, {
        clientId: PAYMENT_CONFIG.NICE_PAY.CLIENT_ID,
        clientSecret: PAYMENT_CONFIG.NICE_PAY.CLIENT_SECRET,
        ...paymentData
      });
      return response.data;
    } catch (error) {
      console.error('나이스페이 결제 오류:', error);
      throw error;
    }
  }

  // 나이스페이 결제 취소
  async nicePayCancel(paymentId, reason) {
    try {
      const response = await axios.post(`${PAYMENT_CONFIG.NICE_PAY.API_URL}/v1/payments/${paymentId}/cancel`, {
        clientId: PAYMENT_CONFIG.NICE_PAY.CLIENT_ID,
        clientSecret: PAYMENT_CONFIG.NICE_PAY.CLIENT_SECRET,
        reason: reason
      });
      return response.data;
    } catch (error) {
      console.error('나이스페이 결제 취소 오류:', error);
      throw error;
    }
  }

  // 카카오페이 결제
  async kakaoPayPayment(paymentData) {
    try {
      const response = await axios.post(`${PAYMENT_CONFIG.KAKAO_PAY.API_URL}/v1/payments`, {
        cid: 'TC0ONETIME', // 단건결제
        partner_order_id: paymentData.merchantUid,
        partner_user_id: paymentData.userId,
        item_name: paymentData.itemName,
        quantity: paymentData.quantity,
        total_amount: paymentData.amount,
        tax_free_amount: 0,
        approval_url: `${window.location.origin}/payment/success`,
        cancel_url: `${window.location.origin}/payment/cancel`,
        fail_url: `${window.location.origin}/payment/fail`
      }, {
        headers: {
          'Authorization': `KakaoAK ${PAYMENT_CONFIG.KAKAO_PAY.ADMIN_KEY}`,
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      });
      return response.data;
    } catch (error) {
      console.error('카카오페이 결제 오류:', error);
      throw error;
    }
  }

  // 카카오페이 결제 취소
  async kakaoPayCancel(paymentId, reason) {
    try {
      const response = await axios.post(`${PAYMENT_CONFIG.KAKAO_PAY.API_URL}/v1/payments/${paymentId}/cancel`, {
        cancel_amount: 0, // 전체 취소
        cancel_tax_free_amount: 0,
        cancel_reason: reason
      }, {
        headers: {
          'Authorization': `KakaoAK ${PAYMENT_CONFIG.KAKAO_PAY.ADMIN_KEY}`,
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      });
      return response.data;
    } catch (error) {
      console.error('카카오페이 결제 취소 오류:', error);
      throw error;
    }
  }

  // 토스페이 결제
  async tossPayPayment(paymentData) {
    try {
      const response = await axios.post(`${PAYMENT_CONFIG.TOSS_PAY.API_URL}/v1/payments`, {
        amount: paymentData.amount,
        orderId: paymentData.merchantUid,
        orderName: paymentData.itemName,
        successUrl: `${window.location.origin}/payment/success`,
        failUrl: `${window.location.origin}/payment/fail`,
        customerName: paymentData.customerName,
        customerEmail: paymentData.customerEmail
      }, {
        headers: {
          'Authorization': `Basic ${btoa(PAYMENT_CONFIG.TOSS_PAY.SECRET_KEY + ':')}`,
          'Content-Type': 'application/json'
        }
      });
      return response.data;
    } catch (error) {
      console.error('토스페이 결제 오류:', error);
      throw error;
    }
  }

  // 토스페이 결제 취소
  async tossPayCancel(paymentId, reason) {
    try {
      const response = await axios.post(`${PAYMENT_CONFIG.TOSS_PAY.API_URL}/v1/payments/${paymentId}/cancel`, {
        cancelReason: reason
      }, {
        headers: {
          'Authorization': `Basic ${btoa(PAYMENT_CONFIG.TOSS_PAY.SECRET_KEY + ':')}`,
          'Content-Type': 'application/json'
        }
      });
      return response.data;
    } catch (error) {
      console.error('토스페이 결제 취소 오류:', error);
      throw error;
    }
  }

  // 신용카드 결제 (나이스페이)
  async creditCardPayment(paymentData) {
    try {
      const response = await axios.post(`${PAYMENT_CONFIG.CREDIT_CARD.NICE_PAY_CARD.API_URL}/v1/payments`, {
        clientId: PAYMENT_CONFIG.CREDIT_CARD.NICE_PAY_CARD.CLIENT_ID,
        clientSecret: PAYMENT_CONFIG.CREDIT_CARD.NICE_PAY_CARD.CLIENT_SECRET,
        method: 'card',
        ...paymentData
      });
      return response.data;
    } catch (error) {
      console.error('신용카드 결제 오류:', error);
      throw error;
    }
  }

  // 결제 수단별 결제 처리
  async processPayment(paymentMethod, paymentData) {
    switch (paymentMethod) {
      case 'naverpay':
        return await this.nicePayPayment(paymentData);
      case 'kakaopay':
        return await this.kakaoPayPayment(paymentData);
      case 'tosspay':
        return await this.tossPayPayment(paymentData);
      case 'card':
        return await this.creditCardPayment(paymentData);
      default:
        throw new Error('지원하지 않는 결제 수단입니다.');
    }
  }

  // 결제 수단별 결제 취소 처리
  async cancelPaymentByMethod(paymentMethod, paymentId, reason) {
    switch (paymentMethod) {
      case 'naverpay':
        return await this.nicePayCancel(paymentId, reason);
      case 'kakaopay':
        return await this.kakaoPayCancel(paymentId, reason);
      case 'tosspay':
        return await this.tossPayCancel(paymentId, reason);
      case 'card':
        return await this.nicePayCancel(paymentId, reason);
      default:
        throw new Error('지원하지 않는 결제 수단입니다.');
    }
  }

  // 결제 내역 조회
  async getPaymentHistory(userId, page = 1, limit = 10) {
    try {
      const response = await axios.get(`${this.baseURL}/api/payments/history`, {
        params: {
          userId,
          page,
          limit
        }
      });
      return response.data;
    } catch (error) {
      console.error('결제 내역 조회 오류:', error);
      throw error;
    }
  }

  // 결제 상세 정보 조회
  async getPaymentDetail(paymentId) {
    try {
      const response = await axios.get(`${this.baseURL}/api/payments/${paymentId}`);
      return response.data;
    } catch (error) {
      console.error('결제 상세 정보 조회 오류:', error);
      throw error;
    }
  }
}

export default new PaymentService(); 