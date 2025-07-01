import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './BookingModal.css';

const BookingModal = ({ movie, onClose, onBookingComplete }) => {
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

  // 영화관 데이터 로드
  useEffect(() => {
    fetchCinemas();
  }, []);

  // 영화관 목록 가져오기
  const fetchCinemas = async () => {
    try {
      setLoading(true);
      const response = await axios.get('http://localhost:80/api/cinemas');
      setCinemas(response.data);
      if (response.data.length > 0) {
        setSelectedCinema(response.data[0]);
        fetchTheaters(response.data[0].id);
      }
    } catch (error) {
      console.error('영화관 목록 로드 실패:', error);
      setError('영화관 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 상영관 목록 가져오기
  const fetchTheaters = async (cinemaId) => {
    try {
      const response = await axios.get(`http://localhost:80/api/cinemas/${cinemaId}/theaters`);
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
      const response = await axios.get(`http://localhost:80/api/theaters/${theaterId}/screenings?movieId=${movie.movieCd}`);
      setScreenings(response.data);
    } catch (error) {
      console.error('상영 스케줄 로드 실패:', error);
      setError('상영 스케줄을 불러오는데 실패했습니다.');
    }
  };

  // 좌석 정보 가져오기
  const fetchSeats = async (screeningId) => {
    try {
      const response = await axios.get(`http://localhost:80/api/screenings/${screeningId}/seats`);
      setSeats(response.data);
    } catch (error) {
      console.error('좌석 정보 로드 실패:', error);
      setError('좌석 정보를 불러오는데 실패했습니다.');
    }
  };

  // 영화관 선택 핸들러
  const handleCinemaChange = (cinema) => {
    setSelectedCinema(cinema);
    setSelectedTheater(null);
    setSelectedScreening(null);
    setSelectedSeats([]);
    fetchTheaters(cinema.id);
  };

  // 상영관 선택 핸들러
  const handleTheaterChange = (theater) => {
    setSelectedTheater(theater);
    setSelectedScreening(null);
    setSelectedSeats([]);
    fetchScreenings(theater.id);
  };

  // 상영시간 선택 핸들러
  const handleScreeningChange = (screening) => {
    setSelectedScreening(screening);
    setSelectedSeats([]);
    fetchSeats(screening.id);
  };

  // 좌석 선택 핸들러
  const handleSeatClick = (seat) => {
    if (seat.status !== 'BOOKING_AVAILABLE') return;
    
    setSelectedSeats(prev => {
      const isSelected = prev.find(s => s.id === seat.id);
      if (isSelected) {
        return prev.filter(s => s.id !== seat.id);
      } else {
        return [...prev, seat];
      }
    });
  };

  // 예매하기 핸들러
  const handleBooking = async () => {
    if (!selectedSeats.length) {
      alert('좌석을 선택해주세요.');
      return;
    }

    try {
      setLoading(true);
      const bookingData = {
        movieId: movie.movieCd,
        screeningId: selectedScreening.id,
        seatIds: selectedSeats.map(seat => seat.id),
        totalPrice: selectedSeats.length * 12000 // 1좌석당 12,000원
      };

      const response = await axios.post('http://localhost:80/api/bookings', bookingData);
      
      if (response.data.success) {
        alert('예매가 완료되었습니다!');
        onBookingComplete && onBookingComplete(response.data);
        onClose();
      } else {
        alert('예매에 실패했습니다: ' + response.data.message);
      }
    } catch (error) {
      console.error('예매 실패:', error);
      alert('예매 처리 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 좌석 상태에 따른 클래스명
  const getSeatClassName = (seat) => {
    let className = 'seat';
    if (seat.status === 'BOOKED') {
      className += ' booked';
    } else if (selectedSeats.find(s => s.id === seat.id)) {
      className += ' selected';
    } else {
      className += ' available';
    }
    return className;
  };

  if (loading && !cinemas.length) {
    return (
      <div className="booking-modal-overlay">
        <div className="booking-modal-content">
          <div className="loading">로딩 중...</div>
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
          {error && <div className="error-message">{error}</div>}

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
              {cinemas.map(cinema => (
                <button
                  key={cinema.id}
                  className={`cinema-btn ${selectedCinema?.id === cinema.id ? 'selected' : ''}`}
                  onClick={() => handleCinemaChange(cinema)}
                >
                  {cinema.name}
                </button>
              ))}
            </div>
          </div>

          {/* 상영관 선택 */}
          {selectedCinema && (
            <div className="selection-section">
              <h4>상영관 선택</h4>
              <div className="theater-list">
                {theaters.map(theater => (
                  <button
                    key={theater.id}
                    className={`theater-btn ${selectedTheater?.id === theater.id ? 'selected' : ''}`}
                    onClick={() => handleTheaterChange(theater)}
                  >
                    {theater.name}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* 상영시간 선택 */}
          {selectedTheater && (
            <div className="selection-section">
              <h4>상영시간 선택</h4>
              <div className="screening-list">
                {screenings.map(screening => (
                  <button
                    key={screening.id}
                    className={`screening-btn ${selectedScreening?.id === screening.id ? 'selected' : ''}`}
                    onClick={() => handleScreeningChange(screening)}
                  >
                    {new Date(screening.screeningTime).toLocaleString('ko-KR', {
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* 좌석 선택 */}
          {selectedScreening && (
            <div className="selection-section">
              <h4>좌석 선택</h4>
              <div className="seat-selection">
                <div className="seat-legend">
                  <div className="legend-item">
                    <div className="seat-legend-available"></div>
                    <span>선택 가능</span>
                  </div>
                  <div className="legend-item">
                    <div className="seat-legend-selected"></div>
                    <span>선택됨</span>
                  </div>
                  <div className="legend-item">
                    <div className="seat-legend-booked"></div>
                    <span>예매됨</span>
                  </div>
                </div>
                <div className="seat-grid">
                  {seats.map(seat => (
                    <button
                      key={seat.id}
                      className={getSeatClassName(seat)}
                      onClick={() => handleSeatClick(seat)}
                      disabled={seat.status === 'BOOKED'}
                    >
                      {seat.seatNumber}
                    </button>
                  ))}
                </div>
                {selectedSeats.length > 0 && (
                  <div className="selected-seats-info">
                    <p>선택된 좌석: {selectedSeats.map(seat => seat.seatNumber).join(', ')}</p>
                    <p>총 금액: {(selectedSeats.length * 12000).toLocaleString()}원</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 예매 버튼 */}
          {selectedSeats.length > 0 && (
            <div className="booking-actions">
              <button 
                className="booking-btn"
                onClick={handleBooking}
                disabled={loading}
              >
                {loading ? '예매 처리 중...' : '예매하기'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default BookingModal; 