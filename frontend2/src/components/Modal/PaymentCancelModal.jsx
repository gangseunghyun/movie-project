import React, { useState } from 'react';
import styles from './Modal.module.css';
import PaymentService from '../../services/PaymentService';

const PaymentCancelModal = ({ isOpen, onClose, paymentInfo, onCancelSuccess }) => {
  const [reason, setReason] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [selectedReason, setSelectedReason] = useState('');

  const cancelReasons = [
    { value: 'schedule_change', label: '일정 변경' },
    { value: 'duplicate_payment', label: '중복 결제' },
    { value: 'service_unsatisfactory', label: '서비스 불만족' },
    { value: 'technical_issue', label: '기술적 문제' },
    { value: 'other', label: '기타' }
  ];

  if (!isOpen) return null;

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const handleCancel = async () => {
    if (!selectedReason) {
      alert('취소 사유를 선택해주세요.');
      return;
    }

    setIsProcessing(true);

    try {
      // 결제 취소 처리
      const cancelResult = await PaymentService.cancelPaymentByMethod(
        paymentInfo.paymentMethod,
        paymentInfo.paymentId,
        selectedReason
      );

      if (cancelResult.success) {
        alert('결제가 취소되었습니다.');
        onCancelSuccess(cancelResult);
        onClose();
      } else {
        alert('결제 취소에 실패했습니다: ' + cancelResult.message);
      }
    } catch (error) {
      console.error('결제 취소 오류:', error);
      alert('결제 취소 중 오류가 발생했습니다. 다시 시도해주세요.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReasonChange = (value) => {
    setSelectedReason(value);
    if (value === 'other') {
      setReason('');
    } else {
      setReason(cancelReasons.find(r => r.value === value)?.label || '');
    }
  };

  return (
    <div className={styles.modalOverlay} onClick={handleBackdropClick}>
      <div className={styles.modalContent}>
        <div className={styles.modalHeader}>
          <h3>결제 취소</h3>
          <button className={styles.closeButton} onClick={onClose}>×</button>
        </div>

        <div className={styles.modalBody}>
          {paymentInfo && (
            <div className={styles.paymentInfo}>
              <div className={styles.infoRow}>
                <span>주문번호:</span>
                <span>{paymentInfo.orderId}</span>
              </div>
              <div className={styles.infoRow}>
                <span>결제금액:</span>
                <span>{paymentInfo.amount?.toLocaleString()}원</span>
              </div>
              <div className={styles.infoRow}>
                <span>결제수단:</span>
                <span>{paymentInfo.paymentMethodName}</span>
              </div>
              <div className={styles.infoRow}>
                <span>결제일시:</span>
                <span>{new Date(paymentInfo.paymentDate).toLocaleString()}</span>
              </div>
            </div>
          )}

          <div className={styles.cancelSection}>
            <h4>취소 사유 선택</h4>
            <div className={styles.reasonList}>
              {cancelReasons.map((reasonOption) => (
                <label key={reasonOption.value} className={styles.reasonOption}>
                  <input
                    type="radio"
                    name="cancelReason"
                    value={reasonOption.value}
                    checked={selectedReason === reasonOption.value}
                    onChange={() => handleReasonChange(reasonOption.value)}
                  />
                  <span>{reasonOption.label}</span>
                </label>
              ))}
            </div>

            {selectedReason === 'other' && (
              <div className={styles.customReason}>
                <label>기타 사유:</label>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="취소 사유를 입력해주세요"
                  rows="3"
                />
              </div>
            )}
          </div>

          <div className={styles.notice}>
            <p>• 결제 취소 시 환불은 3-5일 내에 처리됩니다.</p>
            <p>• 영화 시작 20분 전까지만 취소가 가능합니다.</p>
            <p>• 취소 후 동일한 좌석 재예매는 즉시 가능합니다.</p>
          </div>
        </div>

        <div className={styles.modalFooter}>
          <button 
            className={styles.cancelButton} 
            onClick={onClose}
            disabled={isProcessing}
          >
            취소
          </button>
          <button
            className={styles.confirmButton}
            onClick={handleCancel}
            disabled={!selectedReason || isProcessing}
          >
            {isProcessing ? '처리 중...' : '결제 취소'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentCancelModal; 