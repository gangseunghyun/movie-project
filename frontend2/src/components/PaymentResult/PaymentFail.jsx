import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import styles from './PaymentResult.module.css';

const PaymentFail = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [errorInfo, setErrorInfo] = useState(null);

  useEffect(() => {
    // URL 파라미터에서 에러 정보 가져오기
    const errorCode = searchParams.get('errorCode');
    const errorMessage = searchParams.get('errorMessage');
    const orderId = searchParams.get('orderId');

    if (errorCode) {
      setErrorInfo({
        errorCode,
        errorMessage: errorMessage || '알 수 없는 오류가 발생했습니다.',
        orderId
      });
    }
  }, [searchParams]);

  const handleRetry = () => {
    navigate(-1); // 이전 페이지로 돌아가기
  };

  const handleGoHome = () => {
    navigate('/');
  };

  const getErrorMessage = (errorCode) => {
    const errorMessages = {
      'PAYMENT_CANCELED': '결제가 취소되었습니다.',
      'INSUFFICIENT_FUNDS': '잔액이 부족합니다.',
      'CARD_DECLINED': '카드 결제가 거부되었습니다.',
      'NETWORK_ERROR': '네트워크 오류가 발생했습니다.',
      'TIMEOUT': '결제 시간이 초과되었습니다.',
      'INVALID_CARD': '유효하지 않은 카드입니다.',
      'EXPIRED_CARD': '만료된 카드입니다.',
      'DEFAULT': '결제 중 오류가 발생했습니다.'
    };
    
    return errorMessages[errorCode] || errorMessages['DEFAULT'];
  };

  return (
    <div className={styles.container}>
      <div className={styles.resultCard}>
        <div className={styles.failIcon}>✗</div>
        <h1 className={styles.title}>결제에 실패했습니다</h1>
        
        {errorInfo && (
          <div className={styles.errorDetails}>
            <div className={styles.detailRow}>
              <span>주문번호:</span>
              <span>{errorInfo.orderId}</span>
            </div>
            <div className={styles.detailRow}>
              <span>오류코드:</span>
              <span>{errorInfo.errorCode}</span>
            </div>
            <div className={styles.detailRow}>
              <span>오류내용:</span>
              <span>{getErrorMessage(errorInfo.errorCode)}</span>
            </div>
          </div>
        )}

        <div className={styles.notice}>
          <p>• 결제가 완료되지 않았습니다.</p>
          <p>• 선택하신 좌석은 임시로 예약되어 있습니다.</p>
          <p>• 다시 시도하시거나 다른 결제 수단을 이용해주세요.</p>
          <p>• 문제가 지속되면 고객센터로 문의해주세요.</p>
        </div>

        <div className={styles.buttonContainer}>
          <button className={styles.secondaryButton} onClick={handleGoHome}>
            홈으로 가기
          </button>
          <button className={styles.primaryButton} onClick={handleRetry}>
            다시 시도
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentFail; 