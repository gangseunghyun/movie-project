import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import styles from './ReservationPage.module.css';
import ReservationCard from './ReservationCard';
import ReservationDetailModal from './ReservationDetailModal';
import ReservationFilter from './ReservationFilter';
import { useUser } from '../../contexts/UserContext';
import previousIcon from '../../assets/previous_icon.png';

const ReservationPage = () => {
  const { user } = useUser();
  const navigate = useNavigate();
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedReservation, setSelectedReservation] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [filters, setFilters] = useState({
    search: '',
    period: 'all',
    status: 'all',
    sortBy: 'latest'
  });

  useEffect(() => {
    if (user?.id) {
      fetchReservations();
    }
  }, [user]);

  const fetchReservations = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`http://localhost:80/api/users/${user.id}/reservations`, {
        withCredentials: true
      });
      setReservations(response.data);
      console.log(response.data);
    } catch (error) {
      console.error('예매 내역 조회 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCardClick = (reservation) => {
    setSelectedReservation(reservation);
    setShowDetailModal(true);
  };

  const handleCancelPayment = async (payment, reservationId) => {
    const reason = window.prompt('결제 취소 사유를 입력하세요 (선택)') || '';
    const impUid = payment.impUid || payment.imp_uid;
    
    if (!impUid) {
      alert('결제정보가 없습니다.');
      return;
    }

    try {
      const response = await axios.post(
        'http://localhost:80/api/payments/cancel',
        { imp_uid: impUid, reason },
        { withCredentials: true }
      );

      if (response.data.success) {
        alert('결제가 취소되었습니다.');
        fetchReservations(); // 목록 새로고침
        setShowDetailModal(false);
      } else {
        alert('결제취소 실패: ' + response.data.message);
      }
    } catch (error) {
      console.error('결제취소 오류:', error);
      alert('결제취소 중 오류가 발생했습니다.');
    }
  };

  const filteredReservations = reservations.filter(reservation => {
    const { search, period, status } = filters;
    const movieName = reservation.screening?.movieNm || '';
    const cinemaName = reservation.cinema?.name || '';
    const payment = reservation.payments?.[0];
    
    // 검색 필터
    if (search && !movieName.toLowerCase().includes(search.toLowerCase()) && 
        !cinemaName.toLowerCase().includes(search.toLowerCase())) {
      return false;
    }

    // 기간 필터
    if (period !== 'all') {
      const reservationDate = new Date(reservation.reservedAt);
      const now = new Date();
      const diffTime = now - reservationDate;
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      
      if (period === 'thisMonth' && diffDays > 30) return false;
      if (period === 'lastMonth' && (diffDays <= 30 || diffDays > 60)) return false;
    }

    // 상태 필터
    if (status !== 'all') {
      if (status === 'completed' && (!payment || payment.status !== 'SUCCESS')) return false;
      if (status === 'cancelled' && (!payment || !payment.cancelled)) return false;
    }

    return true;
  });

  // 정렬
  const sortedReservations = [...filteredReservations].sort((a, b) => {
    switch (filters.sortBy) {
      case 'latest':
        return new Date(b.reservedAt) - new Date(a.reservedAt);
      case 'oldest':
        return new Date(a.reservedAt) - new Date(b.reservedAt);
      case 'amount':
        return (b.totalAmount || 0) - (a.totalAmount || 0);
      default:
        return 0;
    }
  });

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.loading}>예매 내역을 불러오는 중...</div>
      </div>
    );
  }

  return (
    <div className={styles.reservationPage}>
      <div className={styles.container}>
        {/* 헤더 */}
        <div className={styles.header}>
          <button 
            className={styles.backButton}
            onClick={() => navigate(-1)}
          >
            <img src={previousIcon} alt="뒤로가기" className={styles.backIcon} />
          </button>
          <h1 className={styles.title}>예매 내역</h1>
          <div className={styles.reservationCount}>
            총 {reservations.length}건
          </div>
        </div>

        {/* 필터 */}
        <ReservationFilter 
          filters={filters}
          onFilterChange={setFilters}
        />

        {/* 예매 목록 */}
        {sortedReservations.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>🎬</div>
            <h3>예매 내역이 없습니다</h3>
            <p>영화를 예매해보세요!</p>
            <button 
              className={styles.browseButton}
              onClick={() => navigate('/')}
            >
              영화 둘러보기
            </button>
          </div>
        ) : (
          <div className={styles.reservationGrid}>
            {sortedReservations.map((reservation) => (
              <ReservationCard
                key={reservation.reservationId}
                reservation={reservation}
                onClick={() => handleCardClick(reservation)}
              />
            ))}
          </div>
        )}
      </div>

      {/* 상세 정보 모달 */}
      {showDetailModal && selectedReservation && (
        <ReservationDetailModal
          reservation={selectedReservation}
          onClose={() => setShowDetailModal(false)}
          onCancelPayment={handleCancelPayment}
        />
      )}
    </div>
  );
};

export default ReservationPage; 