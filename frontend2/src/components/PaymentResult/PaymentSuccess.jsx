import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import styles from './PaymentResult.module.css';

const PaymentSuccess = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [paymentInfo, setPaymentInfo] = useState(null);

  useEffect(() => {
    // URL 파라미터에서 결제 정보 가져오기
    const paymentId = searchParams.get('paymentId');
    const amount = searchParams.get('amount');
    const orderId = searchParams.get('orderId');

    if (paymentId) {
      setPaymentInfo({
        paymentId,
        amount: amount ? parseInt(amount) : 0,
        orderId
      });
    }
  }, [searchParams]);

  const handleGoHome = () => {
    navigate('/');
  };

  const handleGoMyPage = () => {
    navigate('/mypage');
  };

  return (
    <div className={styles.container}>
      <div className={styles.resultCard}>
        <div className={styles.successIcon}>✓</div>
        <h1 className={styles.title}>결제가 완료되었습니다!</h1>
        
        {paymentInfo && (
          <div className={styles.paymentDetails}>
            <div className={styles.detailRow}>
              <span>주문번호:</span>
              <span>{paymentInfo.orderId}</span>
            </div>
            <div className={styles.detailRow}>
              <span>결제금액:</span>
              <span>{paymentInfo.amount.toLocaleString()}원</span>
            </div>
            <div className={styles.detailRow}>
              <span>결제일시:</span>
              <span>{new Date().toLocaleString()}</span>
            </div>
          </div>
        )}

        <div className={styles.notice}>
          <p>• 예매 확인은 마이페이지에서 확인하실 수 있습니다.</p>
          <p>• 영화 시작 20분 전까지 예매 취소가 가능합니다.</p>
          <p>• 모바일 티켓은 문자 또는 앱을 통해 발급됩니다.</p>
        </div>

        <div className={styles.buttonContainer}>
          <button className={styles.secondaryButton} onClick={handleGoHome}>
            홈으로 가기
          </button>
          <button className={styles.primaryButton} onClick={handleGoMyPage}>
            마이페이지
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentSuccess; 