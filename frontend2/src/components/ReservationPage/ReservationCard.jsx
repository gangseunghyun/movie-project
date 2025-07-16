import React from 'react';
import styles from './ReservationPage.module.css';

const ReservationCard = ({ reservation, onClick, onCancelClick }) => {
  const { screening, cinema, theater, seats, payments, totalAmount, reservedAt } = reservation;
  const payment = payments?.[0];

  const getStatusColor = () => {
    if (payment?.cancelled) return styles.cancelled;
    if (payment?.status === 'SUCCESS' || payment?.status === 'PAID') return styles.completed;
    return styles.pending;
  };

  const getStatusText = () => {
    if (payment?.cancelled) return '취소됨';
    if (payment?.status === 'SUCCESS' || payment?.status === 'PAID') return '예매완료';
    return '처리중';
  };

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

  const formatTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className={styles.reservationCard} onClick={onClick}>
      {/* 영화 포스터 */}
      <div className={styles.posterContainer}>
        <img
          src={screening?.posterUrl || '/placeholder-actor.png'}
          alt={screening?.movieNm || '영화 포스터'}
          className={styles.poster}
          onError={(e) => {
            e.target.onerror = null;
            e.target.src = '/placeholder-actor.png';
          }}
        />
        <div className={`${styles.statusBadge} ${getStatusColor()}`}>
          {getStatusText()}
        </div>
      </div>

      {/* 예매 정보 */}
      <div className={styles.cardContent}>
        <h3 className={styles.movieTitle}>{screening?.movieNm || '영화 제목'}</h3>
        
        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>🏢</span>
          <span className={styles.infoValue}>
            {cinema?.name || '영화관'} / {theater?.name || '상영관'}
          </span>
        </div>

        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>🕒</span>
          <span className={styles.infoValue}>
            {formatTime(screening?.startTime)}
          </span>
        </div>

        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>💺</span>
          <span className={styles.infoValue}>
            {seats?.map(seat => seat.seatNumber).join(', ') || '좌석 정보 없음'}
          </span>
        </div>

        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>💰</span>
          <span className={styles.infoValue}>
            {totalAmount?.toLocaleString() || 0}원
          </span>
        </div>

        <div className={styles.infoRow}>
          <span className={styles.infoLabel}>📅</span>
          <span className={styles.infoValue}>
            {formatDate(reservedAt)}
          </span>
        </div>
      </div>

      {/* 액션 버튼 */}
      <div className={styles.cardActions}>
        <button className={styles.detailButton}>
          상세보기
        </button>
        {payment && !payment.cancelled && (payment.status === 'SUCCESS' || payment.status === 'PAID') && (
          <button 
            className={styles.cancelButton}
            onClick={(e) => {
              e.stopPropagation();
              onCancelClick && onCancelClick(reservation);
            }}
          >
            취소하기
          </button>
        )}
      </div>
    </div>
  );
};

export default ReservationCard; 