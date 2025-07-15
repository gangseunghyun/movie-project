import React, { useState } from 'react';
import styles from './ReservationPage.module.css';

const ReservationDetailModal = ({ reservation, onClose, onCancelPayment }) => {
  const { screening, cinema, theater, seats, payments, totalAmount, reservedAt } = reservation;
  const payment = payments?.[0];
  const [showFullReceipt, setShowFullReceipt] = useState(false);

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const shortId = (id) => {
    if (!id) return '';
    if (id.length <= 16) return id;
    return id.slice(0, 8) + '...' + id.slice(-8);
  };

  const handleCancelClick = () => {
    if (payment && !payment.cancelled) {
      onCancelPayment(payment, reservation.reservationId);
    }
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className={styles.modalOverlay} onClick={handleBackdropClick}>
      <div className={styles.detailModal}>
        {/* 헤더 */}
        <div className={styles.modalHeader}>
          <h2 className={styles.modalTitle}>🎫 예매 상세 정보</h2>
          <button className={styles.closeButton} onClick={onClose}>
            ✕
          </button>
        </div>

        <div className={styles.modalContent}>
          {/* 영화 정보 */}
          <div className={styles.movieSection}>
            <div className={styles.movieHeader}>
              <img
                src={screening?.posterUrl || '/placeholder-actor.png'}
                alt={screening?.movieNm || '영화 포스터'}
                className={styles.moviePoster}
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = '/placeholder-actor.png';
                }}
              />
              <div className={styles.movieInfo}>
                <h3 className={styles.movieTitle}>{screening?.movieNm || '영화 제목'}</h3>
                <div className={styles.movieMeta}>
                  <span>{screening?.showTm || 0}분</span>
                  {screening?.watchGradeNm && <span> • {screening.watchGradeNm}</span>}
                </div>
              </div>
            </div>
          </div>

          {/* 예매 정보 */}
          <div className={styles.infoSection}>
            <h4 className={styles.sectionTitle}>📋 예매 정보</h4>
            <div className={styles.infoGrid}>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>🏢 영화관</span>
                <span className={styles.infoValue}>{cinema?.name || '영화관'}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>🏟️ 상영관</span>
                <span className={styles.infoValue}>{theater?.name || '상영관'}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>🕒 상영일시</span>
                <span className={styles.infoValue}>
                  {formatDate(screening?.startTime)}
                </span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>💺 좌석</span>
                <span className={styles.infoValue}>
                  {seats?.map(seat => seat.seatNumber).join(', ') || '좌석 정보 없음'}
                </span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>📅 예매일시</span>
                <span className={styles.infoValue}>
                  {formatDate(reservedAt)}
                </span>
              </div>
            </div>
          </div>

          {/* 결제 정보 */}
          {payment && (
            <div className={styles.paymentSection}>
              <h4 className={styles.sectionTitle}>💳 결제 정보</h4>
              <div className={styles.infoGrid}>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>결제금액</span>
                  <span className={styles.infoValue}>
                    <span className={styles.amount}>{totalAmount?.toLocaleString() || 0}원</span>
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>결제수단</span>
                  <span className={styles.infoValue}>{payment.method || 'N/A'}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>결제상태</span>
                  <span className={`${styles.infoValue} ${styles.status}`}>
                    {payment.cancelled ? '취소됨' : payment.status || 'N/A'}
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>결제일시</span>
                  <span className={styles.infoValue}>
                    {formatDate(payment.paidAt)}
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>결제번호</span>
                  <span className={styles.infoValue}>
                    <span className={styles.code}>{payment.impUid || 'N/A'}</span>
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>주문번호</span>
                  <span className={styles.infoValue}>
                    <span className={styles.code}>{payment.merchantUid || 'N/A'}</span>
                  </span>
                </div>
                {payment.receiptNumber && (
                  <div className={styles.infoItem}>
                    <span className={styles.infoLabel}>영수증번호</span>
                    <span className={styles.infoValue}>
                      <span 
                        className={styles.code}
                        onClick={() => setShowFullReceipt(!showFullReceipt)}
                        style={{ cursor: 'pointer' }}
                      >
                        {showFullReceipt ? payment.receiptNumber : shortId(payment.receiptNumber)}
                      </span>
                    </span>
                  </div>
                )}
                {payment.cardName && (
                  <div className={styles.infoItem}>
                    <span className={styles.infoLabel}>카드사명</span>
                    <span className={styles.infoValue}>{payment.cardName}</span>
                  </div>
                )}
                {payment.cardNumberSuffix && (
                  <div className={styles.infoItem}>
                    <span className={styles.infoLabel}>카드번호(끝4자리)</span>
                    <span className={styles.infoValue}>{payment.cardNumberSuffix}</span>
                  </div>
                )}
                {payment.approvalNumber && (
                  <div className={styles.infoItem}>
                    <span className={styles.infoLabel}>승인번호</span>
                    <span className={styles.infoValue}>{payment.approvalNumber}</span>
                  </div>
                )}
                {payment.cancelled && (
                  <>
                    <div className={styles.infoItem}>
                      <span className={styles.infoLabel}>취소사유</span>
                      <span className={styles.infoValue}>{payment.cancelReason || 'N/A'}</span>
                    </div>
                    <div className={styles.infoItem}>
                      <span className={styles.infoLabel}>취소일시</span>
                      <span className={styles.infoValue}>
                        {formatDate(payment.cancelledAt)}
                      </span>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

          {/* 영수증 링크 */}
          {payment?.receiptUrl && (
            <div className={styles.receiptSection}>
              <a 
                href={payment.receiptUrl} 
                target="_blank" 
                rel="noopener noreferrer"
                className={styles.receiptLink}
              >
                📄 영수증 바로가기
              </a>
            </div>
          )}
        </div>

        {/* 액션 버튼 */}
        <div className={styles.modalActions}>
          <button className={styles.closeModalButton} onClick={onClose}>
            닫기
          </button>
          {payment && !payment.cancelled && (payment.status === 'SUCCESS' || payment.status === 'PAID') && (
            <button className={styles.cancelPaymentButton} onClick={handleCancelClick}>
              결제취소
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default ReservationDetailModal; 