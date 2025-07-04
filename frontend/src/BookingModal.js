import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './BookingModal.css';
import { useNavigate } from 'react-router-dom';

const BookingModal = ({ movie, onClose, onBookingComplete, goToMyReservations }) => {
  const [cinemas, setCinemas] = useState([]);
  const [selectedCinema, setSelectedCinema] = useState(null);
  const [theaters, setTheaters] = useState([]);
  const [selectedTheater, setSelectedTheater] = useState(null);
  const [screenings, setScreenings] = useState([]);
  const [selectedScreening, setSelectedScreening] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [isLocked, setIsLocked] = useState(false);
  const [paymentReady, setPaymentReady] = useState(false);
  const [showCompleteModal, setShowCompleteModal] = useState(false);

  const MAX_SELECT = 2;

  // 영화관 데이터 로드
  useEffect(() => {
    fetchCinemas();
  }, []);

  // 영화관 목록 가져오기
  const fetchCinemas = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axios.get('http://localhost:80/api/cinemas', {
        withCredentials: true
      });
      console.log('영화관 데이터:', response.data);
      
      if (Array.isArray(response.data) && response.data.length > 0) {
        setCinemas(response.data);
        const firstCinema = response.data[0];
        if (firstCinema && firstCinema.id) {
          setSelectedCinema(firstCinema);
          fetchTheaters(firstCinema.id);
        }
      } else {
        setCinemas([]);
        setError('영화관 정보가 없습니다.');
      }
    } catch (error) {
      console.error('영화관 목록 로드 실패:', error);
      setError('영화관 정보를 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.');
      setCinemas([]);
    } finally {
      setLoading(false);
      setInitialLoading(false);
    }
  };

  // 상영관 목록 가져오기
  const fetchTheaters = async (cinemaId) => {
    try {
      setError(null);
      const response = await axios.get(`http://localhost:80/api/cinemas/${cinemaId}/theaters`, {
        withCredentials: true
      });
      console.log('상영관 데이터:', response.data);
      setTheaters(response.data);
      if (response.data.length > 0) {
        setSelectedTheater(response.data[0]);
        fetchScreenings(response.data[0].id);
      }
    } catch (error) {
      console.error('상영관 목록 로드 실패:', error);
      setError('상영관 정보를 불러오는데 실패했습니다.');
    }
  };

  // 상영 스케줄 가져오기
  const fetchScreenings = async (theaterId) => {
    try {
      setError(null);
      const response = await axios.get(`http://localhost:80/api/theaters/${theaterId}/screenings?movieId=${movie.movieCd}`);
      console.log('상영 스케줄 데이터:', response.data);
      setScreenings(response.data);
    } catch (error) {
      console.error('상영 스케줄 로드 실패:', error);
      setError('상영 스케줄을 불러오는데 실패했습니다.');
    }
  };

  // 좌석 정보 가져오기
  const fetchSeats = async (screeningId) => {
    try {
      setError(null);
      const response = await axios.get(`http://localhost:80/api/screenings/${screeningId}/seats`);
      console.log('좌석 데이터:', response.data);
      setSeats(response.data);
    } catch (error) {
      console.error('좌석 정보 로드 실패:', error);
      setError('좌석 정보를 불러오는데 실패했습니다.');
    }
  };

  // 좌석 데이터를 행별로 그룹화 (예: A1~A5, B1~B5 ...)
  const groupSeatsByRow = (seats) => {
    const rows = {};
    seats.forEach(seat => {
      const row = seat.seatNumber[0];
      if (!rows[row]) rows[row] = [];
      rows[row].push(seat);
    });
    return Object.keys(rows)
      .sort()
      .map(rowKey => rows[rowKey].sort((a, b) => {
        return parseInt(a.seatNumber.slice(1)) - parseInt(b.seatNumber.slice(1));
      }));
  };

  // 영화관 선택 핸들러
  const handleCinemaChange = (cinema) => {
    if (!cinema || !cinema.id) {
      console.error('유효하지 않은 영화관 정보:', cinema);
      return;
    }
    setSelectedCinema(cinema);
    setSelectedTheater(null);
    setSelectedScreening(null);
    setSelectedSeats([]);
    setScreenings([]);
    setSeats([]);
    fetchTheaters(cinema.id);
  };

  // 상영관 선택 핸들러
  const handleTheaterChange = (theater) => {
    if (!theater || !theater.id) {
      console.error('유효하지 않은 상영관 정보:', theater);
      return;
    }
    setSelectedTheater(theater);
    setSelectedScreening(null);
    setSelectedSeats([]);
    setScreenings([]);
    setSeats([]);
    fetchScreenings(theater.id);
  };

  // 상영시간 선택 핸들러
  const handleScreeningChange = (screening) => {
    if (!screening || !screening.id) {
      console.error('유효하지 않은 상영 정보:', screening);
      return;
    }
    setSelectedScreening(screening);
    setSelectedSeats([]);
    setSeats([]);
    fetchSeats(screening.id);
  };

  // 좌석 선택 핸들러
  const handleSeatClick = (seat) => {
    if (seat.status !== 'AVAILABLE') return;
    const isSelected = selectedSeats.find(s => s.seatId === seat.seatId);
    if (!isSelected && selectedSeats.length >= MAX_SELECT) {
      alert('최대 2좌석까지만 선택할 수 있습니다.');
      return;
    }
    setSelectedSeats(prev => {
      if (isSelected) {
        return prev.filter(s => s.seatId !== seat.seatId);
      } else {
        return [...prev, seat];
      }
    });
  };

  // 예매하기 핸들러 (이제는 좌석 LOCK만 담당)
  const handleBooking = async () => {
    if (!selectedSeats.length) {
      alert('좌석을 선택해주세요.');
      return;
    }
    try {
      setLoading(true);
      // 1. LOCKED API 먼저 호출
      const lockRes = await axios.post('http://localhost:80/api/bookings/lock-seats', {
        screeningId: selectedScreening.id,
        seatIds: selectedSeats.map(seat => seat.seatId)
      });
      if (!lockRes.data.success) {
        alert('좌석 임시 홀드에 실패했습니다. 이미 예매 중이거나 사용할 수 없는 좌석이 있습니다.');
        setLoading(false);
        return;
      }
      setIsLocked(true); // LOCKED 성공
      setPaymentReady(true); // 결제 버튼 노출
    } catch (error) {
      alert('좌석 홀드 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 결제(예약 확정) 핸들러
  const handlePayment = async () => {
    try {
      setLoading(true);
      const bookingData = {
        movieId: movie.movieCd,
        screeningId: selectedScreening.id,
        seatIds: selectedSeats.map(seat => seat.seatId),
        totalPrice: selectedSeats.length * 100
      };
      const response = await axios.post('http://localhost:80/api/bookings', bookingData);
      if (response.data.success) {
        setShowCompleteModal(true);
        setIsLocked(false);
        setPaymentReady(false);
        setSelectedSeats([]);
        if (onBookingComplete) onBookingComplete();
      } else {
        alert('예매에 실패했습니다.');
      }
    } catch (error) {
      alert('예매 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 좌석 상태에 따른 클래스명
  const getSeatClassName = (seat) => {
    let className = 'seat';
    if (seat.status === 'RESERVED' || seat.status === 'BOOKED' || seat.status === 'LOCKED' || seat.status === 'CLOSED' || seat.status === 'UNAVAILABLE' || seat.status === 'COMPLETED') {
      className += ' booked';
    } else if (selectedSeats.find(s => s.seatId === seat.seatId)) {
      className += ' selected';
    } else if (seat.status === 'AVAILABLE') {
      className += ' available';
    } else {
      className += ' unavailable';
    }
    return className;
  };

  // 카카오페이 결제창 호출 함수
  const onClickKakaoPay = async () => {
    const { IMP } = window;
    IMP.init('imp45866522'); // 아임포트 가맹점 식별코드
    IMP.request_pay({
      pg: 'kakaopay', // 카카오페이 테스트 채널
      pay_method: 'card',
      merchant_uid: 'order_' + new Date().getTime(),
      name: '영화 예매',
      amount: selectedSeats.length * 100 || 100, // 최소 1좌석 금액
      buyer_email: 'test@example.com',
      buyer_name: '홍길동',
      buyer_tel: '010-1234-5678',
      buyer_addr: '서울특별시',
      buyer_postcode: '123-456'
    }, async function (rsp) {
      if (rsp.success) {
        // 1. 예매 확정(좌석 예약) API 호출
        const bookingRes = await axios.post('http://localhost:80/api/bookings', {
          movieId: movie.movieCd,
          screeningId: selectedScreening.id,
          seatIds: selectedSeats.map(seat => seat.seatId),
          totalPrice: selectedSeats.length * 100
        });
        if (bookingRes.data.success) {
          // 2. 결제정보 저장 API 호출
          await axios.post('http://localhost:80/api/payments/complete', {
            imp_uid: rsp.imp_uid,
            merchant_uid: rsp.merchant_uid,
            userId: 1, // TODO: 실제 로그인 정보로 대체
            reservationId: bookingRes.data.reservationId
          });
          setShowCompleteModal(true);
          setIsLocked(false);
          setPaymentReady(false);
          setSelectedSeats([]);
          if (onBookingComplete) onBookingComplete();
        } else {
          alert('예매에 실패했습니다.');
        }
      } else {
        alert('결제 실패: ' + rsp.error_msg);
      }
    });
  };

  // 토스페이 결제창 호출 함수
  const onClickTossPay = async () => {
    const { IMP } = window;
    IMP.init('imp45866522'); // 아임포트 가맹점 식별코드
    IMP.request_pay({
      pg: 'uplus', // 구모듈(V1) 토스페이먼츠는 'uplus'로 설정
      pay_method: 'card',
      merchant_uid: 'order_' + new Date().getTime(),
      name: '영화 예매',
      amount: selectedSeats.length * 100 || 100, // 최소 1좌석 금액
      buyer_email: 'test@example.com',
      buyer_name: '홍길동',
      buyer_tel: '010-1234-5678',
      buyer_addr: '서울특별시',
      buyer_postcode: '123-456'
    }, async function (rsp) {
      if (rsp.success) {
        // 1. 예매 확정(좌석 예약) API 호출
        const bookingRes = await axios.post('http://localhost:80/api/bookings', {
          movieId: movie.movieCd,
          screeningId: selectedScreening.id,
          seatIds: selectedSeats.map(seat => seat.seatId),
          totalPrice: selectedSeats.length * 100
        });
        if (bookingRes.data.success) {
          // 2. 결제정보 저장 API 호출
          await axios.post('http://localhost:80/api/payments/complete', {
            imp_uid: rsp.imp_uid,
            merchant_uid: rsp.merchant_uid,
            userId: 1, // TODO: 실제 로그인 정보로 대체
            reservationId: bookingRes.data.reservationId
          });
          setShowCompleteModal(true);
          setIsLocked(false);
          setPaymentReady(false);
          setSelectedSeats([]);
          if (onBookingComplete) onBookingComplete();
        } else {
          alert('예매에 실패했습니다.');
        }
      } else {
        alert('결제 실패: ' + rsp.error_msg);
      }
    });
  };

  // 나이스페이 결제창 호출 함수
  const onClickNicePay = async () => {
    const { IMP } = window;
    IMP.init('imp45866522'); // 아임포트 가맹점 식별코드
    IMP.request_pay({
      pg: 'nice',
      pay_method: 'card',
      merchant_uid: 'order_' + new Date().getTime(),
      name: '영화 예매',
      amount: selectedSeats.length * 100 || 100,
      buyer_email: 'test@example.com',
      buyer_name: '홍길동',
      buyer_tel: '010-1234-5678',
      buyer_addr: '서울특별시',
      buyer_postcode: '123-456'
    }, async function (rsp) {
      if (rsp.success) {
        // 1. 예매 확정(좌석 예약) API 호출
        const bookingRes = await axios.post('http://localhost:80/api/bookings', {
          movieId: movie.movieCd,
          screeningId: selectedScreening.id,
          seatIds: selectedSeats.map(seat => seat.seatId),
          totalPrice: selectedSeats.length * 100
        });
        if (bookingRes.data.success) {
          // 2. 결제정보 저장 API 호출
          await axios.post('http://localhost:80/api/payments/complete', {
            imp_uid: rsp.imp_uid,
            merchant_uid: rsp.merchant_uid,
            userId: 1, // TODO: 실제 로그인 정보로 대체
            reservationId: bookingRes.data.reservationId
          });
          setShowCompleteModal(true);
          setIsLocked(false);
          setPaymentReady(false);
          setSelectedSeats([]);
          if (onBookingComplete) onBookingComplete();
        } else {
          alert('예매에 실패했습니다.');
        }
      } else {
        alert('결제 실패: ' + rsp.error_msg);
      }
    });
  };

  // 좌석 홀드 취소 핸들러
  const handleUnlockSeats = async () => {
    if (!selectedScreening || !selectedSeats.length) return;
    try {
      setLoading(true);
      const res = await axios.post('http://localhost:80/api/bookings/unlock-seats', {
        screeningId: selectedScreening.id,
        seatIds: selectedSeats.map(seat => seat.seatId)
      });
      if (res.data.success) {
        alert('좌석 홀드가 취소되었습니다.');
        setIsLocked(false);
        setPaymentReady(false);
        setSelectedSeats([]);
        fetchSeats(selectedScreening.id); // 좌석 목록 새로고침
      } else {
        alert('좌석 홀드 취소에 실패했습니다.');
      }
    } catch (error) {
      alert('좌석 홀드 취소 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 초기 로딩 중일 때
  if (initialLoading) {
    return (
      <div className="booking-modal-overlay">
        <div className="booking-modal-content">
          <div className="loading">영화관 정보를 불러오는 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="booking-modal-overlay" onClick={onClose}>
      <div className="booking-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="booking-modal-header">
          <h2>{movie.movieNm} 예매</h2>
          <button className="booking-modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="booking-modal-body">
          {error && (
            <div className="error-message">
              {error}
              <button 
                onClick={fetchCinemas}
                style={{
                  marginLeft: '10px',
                  padding: '5px 10px',
                  background: '#667eea',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                다시 시도
              </button>
            </div>
          )}

          {/* 영화 정보 */}
          <div className="movie-info">
            <img src={movie.posterUrl} alt={movie.movieNm} />
            <div>
              <h3>{movie.movieNm}</h3>
              <p>{movie.showTm}분 | {movie.watchGradeNm}</p>
            </div>
          </div>

          {/* 영화관 선택 */}
          <div className="selection-section">
            <h4>영화관 선택</h4>
            <div className="cinema-list">
              {Array.isArray(cinemas) && cinemas.length > 0 ? (
                cinemas.map(cinema => (
                  <button
                    key={cinema.id}
                    className={`cinema-btn ${selectedCinema?.id === cinema.id ? 'selected' : ''}`}
                    onClick={() => handleCinemaChange(cinema)}
                  >
                    {cinema.name}
                  </button>
                ))
              ) : (
                <p>영화관 정보를 불러올 수 없습니다.</p>
              )}
            </div>
          </div>

          {/* 상영관 선택 */}
          {selectedCinema && (
            <div className="selection-section">
              <h4>상영관 선택</h4>
              <div className="theater-list">
                {Array.isArray(theaters) && theaters.length > 0 ? (
                  theaters.map(theater => (
                    <button
                      key={theater.id}
                      className={`theater-btn ${selectedTheater?.id === theater.id ? 'selected' : ''}`}
                      onClick={() => handleTheaterChange(theater)}
                    >
                      {theater.name}
                    </button>
                  ))
                ) : (
                  <p>상영관 정보를 불러올 수 없습니다.</p>
                )}
              </div>
            </div>
          )}

          {/* 상영시간 선택 */}
          {selectedTheater && (
            <div className="selection-section">
              <h4>상영시간 선택</h4>
              <div className="screening-list">
                {Array.isArray(screenings) && screenings.length > 0 ? (
                  screenings.map(screening => (
                    <button
                      key={screening.id}
                      className={`screening-btn ${selectedScreening?.id === screening.id ? 'selected' : ''}`}
                      onClick={() => handleScreeningChange(screening)}
                    >
                      {new Date(screening.startTime).toLocaleString()}
                    </button>
                  ))
                ) : (
                  <p>해당 영화의 상영 스케줄이 없습니다.</p>
                )}
              </div>
            </div>
          )}

          {/* 좌석 선택 */}
          {selectedScreening && (
            <div className="selection-section">
              <h4>좌석 선택</h4>
              <div className="seat-map">
                {Array.isArray(seats) && seats.length > 0 ? (
                  groupSeatsByRow(seats).map((rowSeats, rowIdx) => (
                    <div className="seat-row" key={rowIdx}>
                      {rowSeats.map(seat => (
                        <button
                          key={seat.seatId}
                          className={getSeatClassName(seat)}
                          onClick={() => handleSeatClick(seat)}
                          disabled={seat.status !== 'AVAILABLE'}
                        >
                          {seat.seatNumber}
                        </button>
                      ))}
                    </div>
                  ))
                ) : (
                  <p>좌석 정보를 불러올 수 없습니다.</p>
                )}
              </div>
              <div className="seat-legend">
                <span className="legend-item">
                  <span className="legend-seat available"></span>
                  예매 가능
                </span>
                <span className="legend-item">
                  <span className="legend-seat selected"></span>
                  선택됨
                </span>
                <span className="legend-item">
                  <span className="legend-seat booked"></span>
                  예매됨
                </span>
              </div>
            </div>
          )}

          {/* 예매 버튼 */}
          {Array.isArray(selectedSeats) && selectedSeats.length > 0 && (
            <div className="booking-summary">
              <p>선택된 좌석: {selectedSeats.map(seat => seat.seatNumber).join(', ')}</p>
              <p>총 금액: {selectedSeats.length * 100}원</p>
              <button
                className="booking-btn"
                onClick={handleBooking}
                disabled={loading || !selectedSeats.length || isLocked}
              >
                좌석 홀드하기
              </button>
              {paymentReady && (
                <>
                  <button
                    className="payment-btn"
                    onClick={handlePayment}
                    disabled={loading}
                  >
                    결제하기 (시뮬레이션)
                  </button>
                  <button
                    className="kakao-pay-btn"
                    onClick={onClickKakaoPay}
                    disabled={loading}
                  >
                    카카오페이로 결제하기
                  </button>
                  <button
                    className="toss-pay-btn"
                    onClick={onClickTossPay}
                    disabled={loading}
                  >
                    토스페이로 결제하기
                  </button>
                  <button
                    className="nice-pay-btn"
                    onClick={onClickNicePay}
                    disabled={loading}
                  >
                    나이스페이로 결제하기
                  </button>
                </>
              )}
              {isLocked && paymentReady && (
                <button
                  className="unlock-btn"
                  onClick={handleUnlockSeats}
                  disabled={loading}
                >
                  좌석 홀드 취소
                </button>
              )}
            </div>
          )}

          {showCompleteModal && (
            <div style={{
              position: 'fixed',
              top: 0,
              left: 0,
              width: '100vw',
              height: '100vh',
              background: 'rgba(0,0,0,0.3)',
              zIndex: 3000,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <div style={{ background: '#fff', padding: 32, borderRadius: 12, boxShadow: '0 2px 12px rgba(0,0,0,0.18)', minWidth: 320, textAlign: 'center' }}>
                <div style={{ fontSize: 20, fontWeight: 600, marginBottom: 16 }}>결제 및 예매가 완료되었습니다!</div>
                <button onClick={() => {
                  setShowCompleteModal(false);
                  onClose();
                }} style={{ padding: '8px 24px', borderRadius: 8, background: '#5c6bc0', color: '#fff', border: 'none', fontWeight: 600, fontSize: 16, marginRight: 12 }}>닫기</button>
                <button onClick={() => {
                  setShowCompleteModal(false);
                  onClose();
                  if (goToMyReservations) goToMyReservations();
                }} style={{ padding: '8px 24px', borderRadius: 8, background: '#1976d2', color: '#fff', border: 'none', fontWeight: 600, fontSize: 16 }}>내 예매목록으로 가기</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default BookingModal; 