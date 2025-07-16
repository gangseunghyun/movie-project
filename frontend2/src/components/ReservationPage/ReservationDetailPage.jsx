import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import styles from './ReservationPage.module.css';
import { useUser } from '../../contexts/UserContext';

const ReservationDetailPage = () => {
  const { reservationId } = useParams();
  const { user } = useUser();
  const navigate = useNavigate();
  const [reservation, setReservation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);
  const [showPrintModal, setShowPrintModal] = useState(false);
  const [cancelPayment, setCancelPayment] = useState(null);

  useEffect(() => {
    // 페이지 맨 위로 스크롤
    window.scrollTo(0, 0);
    
    if (user?.id && reservationId) {
      fetchReservationDetail();
    }
  }, [user, reservationId]);

  const fetchReservationDetail = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`http://localhost:80/api/users/${user.id}/reservations/${reservationId}`, {
        withCredentials: true
      });
      setReservation(response.data);
    } catch (error) {
      console.error('예매 상세 정보 조회 실패:', error);
      alert('예매 정보를 불러올 수 없습니다.');
      navigate('/reservations');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelPayment = async () => {
    const payment = reservation?.payments?.[0];
    if (!payment) {
      alert('결제 정보가 없습니다.');
      return;
    }

    setCancelPayment(payment);
    setShowCancelConfirm(true);
  };

  const confirmCancelPayment = async () => {
    if (!cancelPayment) return;
    
    const reason = window.prompt('결제 취소 사유를 입력하세요 (선택)');
    
    // 사용자가 취소 버튼을 누르면 null이 반환됨
    if (reason === null) {
      setShowCancelConfirm(false);
      setCancelPayment(null);
      return;
    }
    
    const impUid = cancelPayment.impUid || cancelPayment.imp_uid;
    
    if (!impUid) {
      alert('결제정보가 없습니다.');
      setShowCancelConfirm(false);
      setCancelPayment(null);
      return;
    }

    try {
      const response = await axios.post(
        'http://localhost:80/api/payments/cancel',
        { imp_uid: impUid, reason: reason || '' },
        { withCredentials: true }
      );

      if (response.data.success) {
        alert('결제가 취소되었습니다.');
        navigate('/reservations');
      } else {
        alert('결제취소 실패: ' + response.data.message);
      }
    } catch (error) {
      console.error('결제취소 오류:', error);
      alert('결제취소 중 오류가 발생했습니다.');
    } finally {
      setShowCancelConfirm(false);
      setCancelPayment(null);
    }
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
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.loading}>예매 정보를 불러오는 중...</div>
      </div>
    );
  }

  if (!reservation) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.loading}>예매 정보를 찾을 수 없습니다.</div>
      </div>
    );
  }

  const { screening, cinema, theater, seats, payments, totalAmount, reservedAt } = reservation;
  const payment = payments?.[0];

  return (
    <div className={styles.reservationPage}>
      <div className={styles.container}>
        {/* 헤더 */}
        <div className={styles.header}>
          <button 
            className={styles.backButton}
            onClick={() => navigate('/reservations')}
          >
            ← 예매 내역으로
          </button>
          <h1 className={styles.title}>예매 확인/취소</h1>
        </div>

        {/* 예매 완료 메시지 */}
        <div className={styles.completionMessage}>
          <h2>예매가 완료되었습니다.</h2>
        </div>

        {/* 예매 상세 정보 */}
        <div className={styles.detailCard}>
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
                <span className={styles.infoLabel2}>예매번호</span>
                <span className={styles.infoValue}>{reservationId}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel2}>🏢 영화관</span>
                <span className={styles.infoValue}>{cinema?.name || '영화관'}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel2}>🏟️ 상영관</span>
                <span className={styles.infoValue}>{theater?.name || '상영관'}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel2}>🕒 상영일시</span>
                <span className={styles.infoValue}>
                  {formatTime(screening?.startTime)}
                </span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel2}>💺 좌석</span>
                <span className={styles.infoValue}>
                  {seats?.map(seat => seat.seatNumber).join(', ') || '좌석 정보 없음'}
                </span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel2}>📅 예매일시</span>
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
                  <span className={styles.infoLabel2}>결제금액</span>
                  <span className={styles.infoValue}>
                    <span className={styles.amount}>{totalAmount?.toLocaleString() || 0}원</span>
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel2}>결제수단</span>
                  <span className={styles.infoValue}>{payment.method || 'N/A'}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel2}>결제상태</span>
                  <span className={`${styles.infoValue} ${styles.status}`}>
                    {payment.cancelled ? '취소됨' : payment.status || 'N/A'}
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel2}>결제일시</span>
                  <span className={styles.infoValue}>
                    {formatDate(payment.paidAt)}
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 액션 버튼 */}
        <div className={styles.actionButtons}>
          <button 
            className={styles.printButton}
            onClick={() => setShowPrintModal(true)}
          >
            예매정보 출력
          </button>
          {payment && !payment.cancelled && (payment.status === 'SUCCESS' || payment.status === 'PAID') && (
            <button 
              className={styles.cancelButton}
              onClick={handleCancelPayment}
            >
              예매취소
            </button>
          )}
        </div>

        {/* 예매 유의사항 */}
        <div className={styles.noticeSection}>
          <h4 className={styles.sectionTitle}>예매 유의사항</h4>
          <ul className={styles.noticeList}>
            <li>영화 상영 스케줄은 영화관 사정에 의해 변경될 수 있습니다.</li>
            <li>상영 시작 20분 전까지만 취소가 가능합니다.</li>
            <li>취소 후 동일한 좌석 재예매는 즉시 가능합니다.</li>
            <li>결제 취소 시 환불은 3-5일 내에 처리됩니다.</li>
          </ul>
        </div>
      </div>

      {/* 예매정보 출력 모달 */}
      {showPrintModal && (
        <div className={styles.modalOverlay} onClick={() => setShowPrintModal(false)}>
          <div className={styles.printModal} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3>🎫 예매 정보</h3>
              <button 
                className={styles.closeButton}
                onClick={() => setShowPrintModal(false)}
              >
                ✕
              </button>
            </div>
            
            <div className={styles.printContent}>
              {/* 영화 정보 */}
              <div className={styles.printMovieSection}>
                <img
                  src={screening?.posterUrl || '/placeholder-actor.png'}
                  alt={screening?.movieNm || '영화 포스터'}
                  className={styles.printPoster}
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.src = '/placeholder-actor.png';
                  }}
                />
                <div className={styles.printMovieInfo}>
                  <h4>{screening?.movieNm || '영화 제목'}</h4>
                  <p>{screening?.showTm || 0}분 • {screening?.watchGradeNm || '전체관람가'}</p>
                </div>
              </div>

              {/* 예매 정보 */}
              <div className={styles.printInfoSection}>
                <div className={styles.printInfoRow}>
                  <span className={styles.printLabel}>예매번호</span>
                  <span className={styles.printValue}>{reservationId}</span>
                </div>
                <div className={styles.printInfoRow}>
                  <span className={styles.printLabel}>영화관</span>
                  <span className={styles.printValue}>{cinema?.name || '영화관'}</span>
                </div>
                <div className={styles.printInfoRow}>
                  <span className={styles.printLabel}>상영관</span>
                  <span className={styles.printValue}>{theater?.name || '상영관'}</span>
                </div>
                <div className={styles.printInfoRow}>
                  <span className={styles.printLabel}>상영일시</span>
                  <span className={styles.printValue}>{formatTime(screening?.startTime)}</span>
                </div>
                <div className={styles.printInfoRow}>
                  <span className={styles.printLabel}>좌석</span>
                  <span className={styles.printValue}>
                    {seats?.map(seat => seat.seatNumber).join(', ') || '좌석 정보 없음'}
                  </span>
                </div>
                <div className={styles.printInfoRow}>
                  <span className={styles.printLabel}>예매일시</span>
                  <span className={styles.printValue}>{formatDate(reservedAt)}</span>
                </div>
              </div>

              {/* 결제 정보 */}
              {payment && (
                <div className={styles.printPaymentSection}>
                  <h5>💳 결제 정보</h5>
                  <div className={styles.printInfoRow}>
                    <span className={styles.printLabel}>결제금액</span>
                    <span className={styles.printValue}>{totalAmount?.toLocaleString() || 0}원</span>
                  </div>
                  <div className={styles.printInfoRow}>
                    <span className={styles.printLabel}>결제수단</span>
                    <span className={styles.printValue}>{payment.method || 'N/A'}</span>
                  </div>
                  <div className={styles.printInfoRow}>
                    <span className={styles.printLabel}>결제일시</span>
                    <span className={styles.printValue}>{formatDate(payment.paidAt)}</span>
                  </div>
                </div>
              )}
            </div>

            <div className={styles.modalActions}>
              <button 
                className={styles.printActionButton}
                onClick={() => {
                  window.print();
                  setShowPrintModal(false);
                }}
              >
                🖨️ 인쇄하기
              </button>
              <button 
                className={styles.closeModalButton}
                onClick={() => setShowPrintModal(false)}
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 취소 확인 모달 */}
      {showCancelConfirm && (
        <div className={styles.modalOverlay} onClick={() => setShowCancelConfirm(false)}>
          <div className={styles.confirmModal} onClick={(e) => e.stopPropagation()}>
            <h3>예매 취소 확인</h3>
            <p>정말로 이 예매를 취소하시겠습니까?</p>
            <p>취소 후에는 되돌릴 수 없습니다.</p>
            <div className={styles.modalActions}>
              <button 
                className={styles.closeModalButton}
                onClick={() => setShowCancelConfirm(false)}
              >
                취소
              </button>
              <button 
                className={styles.cancelPaymentButton}
                onClick={confirmCancelPayment}
              >
                예매 취소
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReservationDetailPage; 