import React, { useState } from "react";
import styles from "./payment.module.css";
import BookingSummary from "./BookingSummary";
import { Scrollbar } from "react-scrollbars-custom";
import checkTrue from '../../assets/check_true.png';
import checkFalse from '../../assets/check_false.png';
import checkTrue2 from '../../assets/check_true2.png';
import checkFalse2 from '../../assets/check_false2.png';

const PaymentModal = ({ isOpen, onClose, bookingInfo, onPay }) => {
  const [selectedMethod, setSelectedMethod] = useState("naverpay");
  const [agreed, setAgreed] = useState(false);
  const [agreements, setAgreements] = useState({
    agreeAll: false,
    agreeThirdParty: false,
    agreePayment: false,
    agreeConsignment: false
  });

  // 임시 더미 영화 정보
  const dummyMovieInfo = {
    title: "영화 제목",
    poster: "/path/to/poster.jpg",
    duration: "120분",
    rating: "12세 이상",
    genre: "액션/드라마"
  };

  if (!isOpen) return null;

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const handlePay = () => {
    if (agreements.agreeAll) {
      onPay();
      onClose();
    }
  };

  // 체크박스 토글 함수
  const handleToggle = (key) => {
    if (key === 'agreeAll') {
      const newValue = !agreements.agreeAll;
      setAgreements(prev => ({
        ...prev,
        agreeAll: newValue,
        agreeThirdParty: newValue,
        agreePayment: newValue,
        agreeConsignment: newValue,
      }));
      setAgreed(newValue);
    } else {
      const newValue = !agreements[key];
      setAgreements(prev => ({
        ...prev,
        [key]: newValue,
      }));
      
      // 개별 항목이 변경될 때 모두 동의 상태 업데이트
      const updatedAgreements = {
        ...agreements,
        [key]: newValue,
      };
      const allChecked = updatedAgreements.agreeThirdParty && 
                        updatedAgreements.agreePayment && 
                        updatedAgreements.agreeConsignment;
      
      setAgreements(prev => ({
        ...prev,
        [key]: newValue,
        agreeAll: allChecked
      }));
      setAgreed(allChecked);
    }
  };

  return (
    <div className={styles.modalOverlay} onClick={handleBackdropClick}>
      <div className={styles.paymentPage}>
        <h3 className={styles.paymentTitle}>결제정보 입력</h3>
        <div className={styles.paymentSubTitle}>
          영화예매 결제에 사용할 결제정보를 입력해주세요.
        </div>

        <Scrollbar
          style={{ height: '60vh', width: '100%' }}
          trackYProps={{
            style: {
              right: '-15px',
              width: '6px',
              background: '#2a2a2a',
              borderRadius: '3px'
            }
          }}
          thumbYProps={{
            style: {
              background: '#666',
              borderRadius: '3px',
              width: '6px'
            }
          }}
        >
          {/* 예매 정보 요약 */}
          <div className={styles.paymentSummaryBox}>
            <BookingSummary
              movieInfo={bookingInfo.movieInfo || {}}
              selectedCinema={bookingInfo.selectedCinema || ''}
              selectedTheater={bookingInfo.selectedTheater || ''}
              selectedTime={bookingInfo.selectedTime || ''}
              selectedSeats={bookingInfo.selectedSeats || []}
            />
            <div className={styles.paymentSummaryRow}>
              <span>결제금액</span>
              <span className={styles.paymentPrice}>7,900원</span>
            </div>
          </div>

          {/* 결제수단 선택 */}
          <div className={styles.paymentSectionTitle} style={{marginBottom: '10px'}}>결제수단 선택</div>
          <div className={styles.paymentMethodList}>
            <button
              className={`${styles.paymentMethodBtn} ${selectedMethod === "naverpay" ? styles.selected : ""}`}
              onClick={() => setSelectedMethod("naverpay")}
            >N pay</button>
            <button
              className={`${styles.paymentMethodBtn} ${selectedMethod === "kakaopay" ? styles.selected : ""}`}
              onClick={() => setSelectedMethod("kakaopay")}
            >카카오 pay</button>
            <button
              className={`${styles.paymentMethodBtn} ${selectedMethod === "tosspay" ? styles.selected : ""}`}
              onClick={() => setSelectedMethod("tosspay")}
            >toss pay</button>
            <button
              className={`${styles.paymentMethodBtn} ${selectedMethod === "card" ? styles.selected : ""}`}
              onClick={() => setSelectedMethod("card")}
            >신용카드</button>
          </div>

          {/* 약관 동의 */}
          <div className={styles.paymentAgreement}>
            <div className={styles.checkboxRow} onClick={() => handleToggle('agreeAll')}>
              <img className={styles.bigCheckIcon} src={agreements.agreeAll ? checkTrue : checkFalse} alt="check" />
              <span>모든 약관에 동의합니다.</span>
            </div>
            <ul className={styles.paymentAgreementList}>
              <li onClick={() => handleToggle('agreeThirdParty')}>
                <img src={agreements.agreeThirdParty ? checkTrue2 : checkFalse2} alt="check" />
                <span>개인정보 제3자 제공 동의</span>
              </li>
              <li onClick={() => handleToggle('agreePayment')}>
                <img src={agreements.agreePayment ? checkTrue2 : checkFalse2} alt="check" />
                <span>결제 정보 수집 및 이용 동의</span>
              </li>
              <li onClick={() => handleToggle('agreeConsignment')}>
                <img src={agreements.agreeConsignment ? checkTrue2 : checkFalse2} alt="check" />
                <span>개인정보 취급 위탁 동의</span>
              </li>
            </ul>
          </div>

          {/* 안내문구 */}
          <div className={styles.paymentNotice}>
            <p>- 만 14세 이상만 구매가 가능합니다.</p>
            <p>- 미성년자의 경우 "본인 사전동의"의 동의를 받아야 하며, 동의를 받지 않은 경우 법정대리인이 이용을 취소할 수 있습니다.</p>
            <p>- 결제된 상품 반납이 어렵습니다. 예매와 다른 결제가 추가 구성되어도 이용이 불가합니다.</p>
            <p>- 결제 후 환불은 불가합니다.</p>
            <p>- 결제수단에 따라 추가 인증이 필요할 수 있습니다.</p>
            <p>- 상영관 사정에 따라 영화 상영이 취소될 경우, 전액 환불 처리됩니다.</p>
            <p>- 영화 시작 20분 전까지 예매 취소가 가능합니다. (상영관 정책에 따라 다를 수 있음)</p>
            <p>- 상영관 정책에 따라 음식물 반입이 제한될 수 있습니다.</p>
            <p>- 모바일 티켓은 문자 또는 앱을 통해 발급됩니다.</p>
            <p>- 예매 티켓은 타인에게 양도할 수 없습니다.</p>
            <p>- 기타 자세한 사항은 별도 이용정책 및 구매 페이지를 참고해 주세요.</p>
            <p>- 결제 오류 발생 시 고객센터로 문의해 주세요.</p>
            
          </div>
        </Scrollbar>

        {/* 하단 버튼 */}
        <div className={styles.paymentButtonContainer}>
          <button className={styles.cancelButton} onClick={onClose}>이전</button>
          <button
            className={styles.bookingButton}
            onClick={handlePay}
            disabled={!agreements.agreeAll}
          >
            결제하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentModal; 