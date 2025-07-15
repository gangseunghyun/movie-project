import React from "react";
import styles from "./BookingPage.module.css";

const BookingSummary = ({
  movieInfo,
  selectedCinema,
  selectedTheater,
  selectedTime,
  selectedSeats,
}) => {
  if (!movieInfo) return null;

  return (
    <div className={styles.bookingSummary}>
      <div className={styles.summaryPoster}>
        <img src={movieInfo.posterUrl || movieInfo.poster || '/default_poster.png'} alt={movieInfo.title || movieInfo.movieNm} />
      </div>
      <div className={styles.summaryInfo}>
        <div className={styles.summaryTitle}>{movieInfo.title || movieInfo.movieNm}</div>
        <div className={styles.summaryDetail}>
          <div>
            <b>영화관</b> {selectedCinema || "-"}
          </div>
          <div>
            <b>상영관</b> {selectedTheater || "-"}
          </div>
          <div>
            <b>시간</b> {selectedTime || "-"}
          </div>
          <div>
            <b>좌석</b> {selectedSeats.length > 0 ? selectedSeats.join(", ") : "-"}
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookingSummary; 