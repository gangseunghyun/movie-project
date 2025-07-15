import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';
import { useLocation } from 'react-router-dom';
import styles from './BookingPage.module.css';
import BookingSummary from "./BookingSummary";
import PaymentModal from "./PaymentModal";

const BookingPage = () => {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [movieInfo, setMovieInfo] = useState(location.state?.movieDetail || null);
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedTime, setSelectedTime] = useState('');
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [selectedTheater, setSelectedTheater] = useState(''); // 상영관 id
  const [loading, setLoading] = useState(true);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [cinemas, setCinemas] = useState([]); // 영화관 목록
  const [theaterList, setTheaterList] = useState([]); // 상영관 목록
  const [screenings, setScreenings] = useState([]); // 상영 스케줄 목록
  const [selectedScreeningId, setSelectedScreeningId] = useState('');
  const [seatInfo, setSeatInfo] = useState([]); // 실제 좌석 정보

  // 영화 정보 가져오기 (실제 API 호출로 대체 필요)
  useEffect(() => {
    if (movieInfo) {
      setLoading(false);
      return;
    }
    // 임시 영화 데이터 (state로 안 넘어온 경우만)
    const mockMovieData = {
      id: movieId,
      title: '영화 제목',
      poster: '/path/to/poster.jpg',
      duration: '120분',
      rating: '12세 이상',
      genre: '액션/드라마'
    };
    setMovieInfo(mockMovieData);
    setLoading(false);
  }, [movieId, movieInfo]);

  // 영화관 목록 받아오기
  useEffect(() => {
    fetch('http://localhost:80/api/cinemas', {
      credentials: 'include',
    })
      .then((res) => {
        if (!res.ok) throw new Error('영화관 목록을 불러오지 못했습니다.');
        return res.json();
      })
      .then((data) => {
        setCinemas(data);
      })
      .catch((err) => {
        console.error('영화관 목록 불러오기 실패:', err);
        setCinemas([]);
      });
  }, []);

  // 영화관 선택 시 상영관 목록 받아오기
  useEffect(() => {
    if (!selectedCinemaId) {
      setTheaterList([]);
      setSelectedTheater('');
      return;
    }
    fetch(`http://localhost:80/api/cinemas/${selectedCinemaId}/theaters`, {
      credentials: 'include',
    })
      .then((res) => {
        if (!res.ok) throw new Error('상영관 목록을 불러오지 못했습니다.');
        return res.json();
      })
      .then((data) => {
        setTheaterList(data);
        setSelectedTheater('');
      })
      .catch((err) => {
        console.error('상영관 목록 불러오기 실패:', err);
        setTheaterList([]);
        setSelectedTheater('');
      });
  }, [selectedCinemaId]);

  // 상영관 선택 시 상영 스케줄 받아오기
  useEffect(() => {
    if (!selectedTheater) {
      setScreenings([]);
      setSelectedDate('');
      setSelectedTime('');
      return;
    }
    //console.log('상영 스케줄 API 호출:', { selectedTheater, movieId });
    const url = `http://localhost:80/api/theaters/${selectedTheater}/screenings?movieId=${movieId}`;
    //console.log('요청 URL:', url);
    fetch(url, {
      credentials: 'include',
    })
      .then((res) => {
        //console.log('상영 스케줄 응답 상태:', res.status, res.statusText);
        if (!res.ok) throw new Error('상영 스케줄을 불러오지 못했습니다.');
        return res.json();
      })
      .then((data) => {
        console.log('받아온 상영 스케줄 데이터:', data);
        setScreenings(data);
        setSelectedDate('');
        setSelectedTime('');
      })
      .catch((err) => {
        console.error('상영 스케줄 불러오기 실패:', err);
        setScreenings([]);
        setSelectedDate('');
        setSelectedTime('');
      });
  }, [selectedTheater, movieId]);

  // 스케줄에서 날짜/시간 옵션 추출
  // 모든 상영 스케줄의 날짜+시간을 한 번에 버튼으로 보여줌
  const timeOptions = screenings.map(s => ({
    value: s.startTime,
    label: s.startTime ? s.startTime.replace('T', ' ').slice(0, 16) : '',
    screeningId: s.id
  }));


  // 시간 선택 시 screeningId 저장
  const handleTimeSelect = (timeObj) => {
    setSelectedTime(timeObj.value);
    setSelectedScreeningId(timeObj.screeningId);
    setSelectedSeats([]); // 시간 바뀌면 좌석 초기화
  };

  // 좌석 정보 받아오기
  useEffect(() => {
    if (!selectedScreeningId) {
      setSeatInfo([]);
      return;
    }
    fetch(`http://localhost:80/api/screenings/${selectedScreeningId}/seats`, {
      credentials: 'include',
    })
      .then((res) => {
        if (!res.ok) throw new Error('좌석 정보를 불러오지 못했습니다.');
        return res.json();
      })
      .then((data) => {
        setSeatInfo(data);
      })
      .catch((err) => {
        console.error('좌석 정보 불러오기 실패:', err);
        setSeatInfo([]);
      });
  }, [selectedScreeningId]);

  // 좌석 선택 처리
  const handleSeatClick = (seatId) => {
    setSelectedSeats(prev => {
      if (prev.includes(seatId)) {
        return prev.filter(id => id !== seatId);
      } else {
        return [...prev, seatId];
      }
    });
  };

  // 실제 좌석 상태 반환 함수
  const getSeatStatus = (seatId) => {
    const seat = seatInfo.find(s => s.seatNumber === seatId);
    if (!seat) return 'available';
    if (seat.status === 'RESERVED') return 'reserved';
    if (seat.status === 'DISABLED') return 'disabled';
    return 'available';
  };

  // 예매 진행
  const handleBooking = async () => {
    if (!selectedScreeningId || selectedSeats.length === 0) {
      alert('모든 항목을 선택해주세요.');
      return;
    }

    // 선택된 좌석의 DB id만 추출
    const selectedSeatIds = seatInfo
      .filter(seat => selectedSeats.includes(seat.seatNumber))
      .map(seat => seat.seatId);
      console.log('selectedSeats:', selectedSeats);
      console.log('seatInfo:', seatInfo);
      console.log('seatInfo seatIds:', seatInfo.map(seat => seat.seatId)); // 추가!
      console.log('selectedSeatIds:', selectedSeatIds);
    
    // 결제 금액 계산 (좌석당 10,000원)
    const totalPrice = selectedSeats.length * 10000;
    
    try {
      setLoading(true);
      // 1. LOCKED API 먼저 호출 (좌석 홀드)
      const lockRes = await axios.post('http://localhost:80/api/bookings/lock-seats', {
        screeningId: selectedScreeningId,
        seatIds: selectedSeatIds
      }, { withCredentials: true });
      
      if (!lockRes.data.success) {
        alert('좌석 임시 홀드에 실패했습니다. 이미 예매 중이거나 사용할 수 없는 좌석이 있습니다.');
        setLoading(false);
        return;
      }
      
      // 2. 좌석 홀드 성공 시 결제 모달 열기
      setIsPaymentModalOpen(true);
      
    } catch (error) {
      alert('좌석 홀드 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 결제 처리
  const handlePayment = async (paymentResult) => {
    try {
      // 선택된 좌석의 DB id만 추출
      const selectedSeatIds = seatInfo
        .filter(seat => selectedSeats.includes(seat.seatNumber))
        .map(seat => seat.seatId);
      
      const totalPrice = selectedSeats.length * 10000;
      
      const bookingData = {
        movieId,
        screeningId: selectedScreeningId,
        seatIds: selectedSeatIds,
        totalPrice: totalPrice
      };

      // 3. 예매 확정 API 호출
      const response = await axios.post('http://localhost:80/api/bookings', bookingData, { withCredentials: true });
      
      if (response.data.success) {
        // 4. 결제정보 저장 API 호출
        await axios.post('http://localhost:80/api/payments/complete', {
          imp_uid: paymentResult.imp_uid,
          merchant_uid: paymentResult.merchant_uid,
          userId: 1, // TODO: 실제 로그인 정보로 대체
          reservationId: response.data.reservationId
        }, { withCredentials: true });
        
        alert('예매가 완료되었습니다.');
        setSelectedSeats([]); // 결제 완료 후에만 초기화
        navigate(`/reservations/${response.data.reservationId}`); // 예매 상세 페이지로 이동
      } else {
        alert(response.data.message || '예매 처리 중 오류가 발생했습니다.');
      }
    } catch (error) {
      // 예매 실패 시 좌석 잠금 해제
      try {
        const selectedSeatIds = seatInfo
          .filter(seat => selectedSeats.includes(seat.seatNumber))
          .map(seat => seat.seatId);
        
        await axios.post('http://localhost:80/api/bookings/unlock-seats', {
          screeningId: selectedScreeningId,
          seatIds: selectedSeatIds
        }, { withCredentials: true });
      } catch (unlockError) {
        console.error('좌석 잠금 해제 실패:', unlockError);
      }
      alert('예매 처리 중 오류가 발생했습니다.');
    }
  };

  if (loading) {
    return <div className={styles.loading}>로딩 중...</div>;
  }

  return (
    <div className={styles.bookingPage}>
      <div className={styles.container}>
        {/* 헤더 */}
        <div className={styles.header}>
          
          <h1 className={styles.title}>영화 예매</h1>
        </div>
        {/* 영화 정보 */}
        <div className={styles.movieInfoHeader}>
          <div className={styles.movieInfoHeaderInner}>
            <div className={styles.movieInfoContent}>
              <div className={styles.moviePosterWrap}>
                <img
                  src={movieInfo?.posterUrl || movieInfo?.poster || '/default_poster.png'}
                  alt={movieInfo?.title || movieInfo?.movieNm}
                  className={styles.moviePoster}
                />
              </div>
              <div className={styles.movieInfoText}>
                <h3 className={styles.movieTitle}>{movieInfo?.title || movieInfo?.movieNm}</h3>
                <div className={styles.movieMetaLine}>
                  <span>{movieInfo?.openDt}</span>
                  {movieInfo?.genreNm && <span> · {movieInfo.genreNm}</span>}
                  {movieInfo?.genre && <span> · {movieInfo.genre}</span>}
                </div>
                <div className={styles.movieMetaLine}>
                  <span>{movieInfo?.showTm ? `${movieInfo.showTm}분` : movieInfo?.duration}</span>
                  {movieInfo?.watchGradeNm && <span> · {movieInfo.watchGradeNm}</span>}
                  {movieInfo?.rating && <span> · {movieInfo.rating}</span>}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 영화관 선택 */}
        <div className={styles.section}>
          <h3 className={styles.sectionTitle}>영화관 선택</h3>
          <div className={styles.theaterList}>
            {cinemas.map(cinema => (
              <button
                key={cinema.id}
                className={`${styles.theaterButton} ${selectedCinemaId === String(cinema.id) ? styles.selected : ''}`}
                onClick={() => setSelectedCinemaId(String(cinema.id))}
              >
                <div className={styles.theaterName}>{cinema.name}</div>
                <div className={styles.theaterLocation}>{cinema.location}</div>
              </button>
            ))}
          </div>
        </div>

        {/* 상영관(관) 선택 */}
        <div className={styles.section}>
          <h3 className={styles.sectionTitle}>상영관 선택</h3>
          <div className={styles.theaterList}>
            {!selectedCinemaId ? (
              <div style={{ color: '#b0b8c1', padding: '6px 0', width: '110%', textAlign: 'left' }}>
                영화관을 먼저 선택해주세요.
              </div>
            ) : theaterList.length === 0 ? (
              <div style={{ color: '#b0b8c1', padding: '6px 0', width: '110%', textAlign: 'left' }}>
                상영관 정보가 없습니다.
              </div>
            ) : (
              theaterList.map(theater => (
                <button
                  key={theater.id}
                  className={`${styles.theaterButton} ${selectedTheater === String(theater.id) ? styles.selected : ''}`}
                  onClick={() => setSelectedTheater(String(theater.id))}
                >
                  <div className={styles.theaterName}>{theater.name}</div>
                </button>
              ))
            )}
          </div>
        </div>

        {/* 날짜/시간 통합 선택 (한 번에 모두 노출) */}
        <div className={styles.section}>
          <h3 className={styles.sectionTitle}>날짜/시간 선택</h3>
          <div className={styles.timeList}>
            {timeOptions.length === 0 ? (
              <div className={styles.noScheduleMsg}>상영 스케줄이 없습니다.</div>
            ) : timeOptions.map(time => (
              <button
                key={time.screeningId}
                className={`${styles.timeButton} ${selectedTime === time.value ? styles.selected : ''}`}
                onClick={() => handleTimeSelect(time)}
              >
                {time.label}
              </button>
            ))}
          </div>
        </div>

        {/* 좌석 선택 */}
        <div className={styles.section}>
          <h3 className={styles.sectionTitle}>좌석 선택</h3>
          <div className={styles.seatMap}>
            <div className={styles.screen}>SCREEN</div>
            {seatInfo.length === 0 ? (
              <div className={styles.noScheduleMsg}>좌석 정보를 불러올 수 없습니다.</div>
            ) : (
              <div className={styles.seats}>
                {Array.from({ length: 5 }, (_, row) => (
                  <div key={row} className={styles.seatRow}>
                    {Array.from({ length: 10 }, (_, col) => {
                      const seatId = `${String.fromCharCode(65 + row)}${col + 1}`;
                      const seatStatus = getSeatStatus(seatId);
                      return (
                        <button
                          key={seatId}
                          className={
                            `${styles.seat} ` +
                            (selectedSeats.includes(seatId) && seatStatus === 'available' ? styles.selected + ' ' : '') +
                            (seatStatus === 'reserved' ? styles.reserved + ' ' : '') +
                            (seatStatus === 'disabled' ? styles.disabled + ' ' : '')
                          }
                          onClick={() => seatStatus === 'available' && handleSeatClick(seatId)}
                          disabled={seatStatus !== 'available'}
                        >
                          {seatStatus === 'disabled' ? '✕' : seatId}
                        </button>
                      );
                    })}
                  </div>
                ))}
              </div>
            )}
          </div>
          {/* 좌석 상태 legend */}
          <div className={styles.seatLegend}>
            <div><span className={`${styles.seat} ${styles.selected}`}></span> 선택</div>
            <div><span className={`${styles.seat} ${styles.reserved}`}></span> 예매 완료</div>
            <div><span className={`${styles.seat} ${styles.disabled}`}>✕</span> 선택불가</div>
          </div>
        </div>

        {/* 선택된 좌석 표시 */}
        {/* {selectedSeats.length > 0 && (
          <div className={styles.selectedSeats}>
            <h4>선택된 좌석: {selectedSeats.join(', ')}</h4>
          </div>
        )} */}

        {/* 예매 정보 요약 박스 */}
        <BookingSummary
          movieInfo={movieInfo || {}}
          selectedCinema={cinemas.find(c => String(c.id) === selectedCinemaId)?.name || ''}
          selectedTheater={theaterList.find(t => String(t.id) === selectedTheater)?.name || ''}
          selectedTime={selectedTime || ''}
          selectedSeats={selectedSeats || []}
          // totalPrice={selectedSeats.length > 0 ? selectedSeats.length * 10000 : null}
        />

        {/* 예매/취소 버튼 */}
        <div className={styles.bookingButtonContainer}>
          <button
            className={styles.cancelButton}
            onClick={() => navigate(-1)}
            style={{marginRight: '16px'}}
          >
            취소
          </button>
          <button 
            className={styles.bookingButton}
            onClick={handleBooking}
            disabled={!selectedScreeningId || selectedSeats.length === 0}
          >
            예매하기
          </button>
        </div>
      </div>

      {/* 결제 모달 */}
      <PaymentModal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        bookingInfo={{
          movieInfo: movieInfo || {},
          selectedCinema: cinemas.find(c => String(c.id) === selectedCinemaId)?.name || '',
          selectedTheater: theaterList.find(t => String(t.id) === selectedTheater)?.name || '',
          selectedTime: selectedTime || '',
          selectedSeats: selectedSeats || [],
          totalPrice: selectedSeats.length * 10000,
          screeningId: selectedScreeningId,
          seatInfo: seatInfo
        }}
        onPay={handlePayment}
      />
    </div>
  );
};

export default BookingPage; 