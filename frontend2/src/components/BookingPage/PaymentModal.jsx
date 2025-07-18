import React, { useState } from "react";
import styles from "./payment.module.css";
import BookingSummary from "./BookingSummary";
import { Scrollbar } from "react-scrollbars-custom";
import checkTrue from '../../assets/check_true.png';
import checkFalse from '../../assets/check_false.png';
import checkTrue2 from '../../assets/check_true2.png';
import checkFalse2 from '../../assets/check_false2.png';
import PaymentService from '../../services/PaymentService';
import { PAYMENT_METHODS } from '../../services/paymentConfig';

const PaymentModal = ({ isOpen, onClose, bookingInfo, onPay }) => {
  const [selectedMethod, setSelectedMethod] = useState("naverpay");
  const [agreed, setAgreed] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
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
      handleClose();
    }
  };

  // 모달 닫기 시 좌석 잠금 해제
  const handleClose = async () => {
    try {
      // 좌석 잠금 해제
      const selectedSeatIds = bookingInfo.selectedSeats
        .map(seatNumber => {
          const seat = bookingInfo.seatInfo?.find(s => s.seatNumber === seatNumber);
          return seat?.seatId;
        })
        .filter(id => id);
      
      if (selectedSeatIds.length > 0) {
        await fetch('http://localhost:80/api/bookings/unlock-seats', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({
            screeningId: bookingInfo.screeningId,
            seatIds: selectedSeatIds
          })
        });
        console.log('좌석 잠금이 해제되었습니다.');
      }
    } catch (unlockError) {
      console.error('좌석 잠금 해제 실패:', unlockError);
    }
    onClose();
  };

  // 실제 결제 처리 함수
  const handlePay = async () => {
    if (!agreements.agreeAll) {
      alert('모든 약관에 동의해주세요.');
      return;
    }

    setIsProcessing(true);

    try {
      // 아임포트 설정 가져오기 (임시로 하드코딩)
      const { IMP } = window;
      IMP.init('imp45866522'); // 임시 하드코딩

      // 결제 요청
      IMP.request_pay({
        pg: getPaymentPG(selectedMethod),
        pay_method: 'card',
        merchant_uid: `order_${Date.now()}`,
        name: `${bookingInfo.movieInfo?.title || '영화'} 예매`,
        amount: bookingInfo.totalPrice,
        buyer_email: localStorage.getItem('userEmail') || 'test@example.com',
        buyer_name: localStorage.getItem('userName') || '홍길동',
        buyer_tel: '010-1234-5678',
        buyer_addr: '서울특별시',
        buyer_postcode: '123-456'
      }, async function (rsp) {
        if (rsp.success) {
          // 결제 성공 시 예매 확정
          onPay({
            imp_uid: rsp.imp_uid,
            merchant_uid: rsp.merchant_uid,
            success: true
          });
          onClose();
        } else {
          // 결제 실패 시 좌석 잠금 해제
          try {
            const selectedSeatIds = bookingInfo.selectedSeats
              .map(seatNumber => {
                const seat = bookingInfo.seatInfo?.find(s => s.seatNumber === seatNumber);
                return seat?.seatId;
              })
              .filter(id => id);
            
            await fetch('http://localhost:80/api/bookings/unlock-seats', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
              },
              credentials: 'include',
              body: JSON.stringify({
                screeningId: bookingInfo.screeningId,
                seatIds: selectedSeatIds
              })
            });
          } catch (unlockError) {
            console.error('좌석 잠금 해제 실패:', unlockError);
          }
          alert('결제 실패: ' + rsp.error_msg);
        }
        setIsProcessing(false);
      });
      
    } catch (error) {
      console.error('결제 오류:', error);
      alert('결제 중 오류가 발생했습니다. 다시 시도해주세요.');
      setIsProcessing(false);
    }
  };

  // 결제 수단별 PG 설정
  const getPaymentPG = (method) => {
    switch (method) {
      case 'kakaopay':
        return 'kakaopay';
      case 'tosspay':
        return 'uplus'; // 토스페이먼츠
      case 'naverpay':
        return 'nice'; // 나이스페이먼츠
      case 'card':
        return 'nice'; // 신용카드
      default:
        return 'nice';
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
          <BookingSummary
            movieInfo={bookingInfo.movieInfo || {}}
            selectedCinema={bookingInfo.selectedCinema || ''}
            selectedTheater={bookingInfo.selectedTheater || ''}
            selectedTime={bookingInfo.selectedTime || ''}
            selectedSeats={bookingInfo.selectedSeats || []}
            totalPrice={bookingInfo.totalPrice}
          />

          {/* 결제수단 선택 */}
          <div className={styles.paymentSectionTitle} style={{marginBottom: '10px'}}>결제수단 선택</div>
          <div className={styles.paymentMethodList}>
            {Object.entries(PAYMENT_METHODS).map(([key, method]) => (
              <button
                key={key}
                className={`${styles.paymentMethodBtn} ${selectedMethod === key ? styles.selected : ""}`}
                onClick={(e) => {
                  e.preventDefault(); // 기본 동작 방지
                  e.currentTarget.blur(); // 포커스 제거
                  setSelectedMethod(key);
                }}
              >
                {method.name}
              </button>
            ))}
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
          <button className={styles.cancelButton} onClick={handleClose} disabled={isProcessing}>
            이전
          </button>
          <button
            className={styles.bookingButton}
            onClick={handlePay}
            disabled={!agreements.agreeAll || isProcessing}
          >
            {isProcessing ? '결제 처리 중...' : '결제하기'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentModal; 