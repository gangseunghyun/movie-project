import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { API_BASE_URL } from './App';

function UserReservations({ onSelectReservation, currentUser }) {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const userId = currentUser?.id;
  const [cancelSuccess, setCancelSuccess] = useState(false);
  const [pendingReservationId, setPendingReservationId] = useState(null);

  useEffect(() => {
    if (userId) {
      fetchReservations();
    }
  }, [userId]);

  const fetchReservations = () => {
    axios.get(`${API_BASE_URL}/users/${userId}/reservations`, { withCredentials: true })
      .then(res => {
        setReservations(res.data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  const handleCancelPayment = async (payment, reservationId) => {
    const reason = window.prompt('결제 취소 사유를 입력하세요 (선택)') || '';
    const impUid = payment.impUid || payment.imp_uid;
    if (!impUid) return alert('결제정보가 없습니다.');
    try {
      const res = await axios.post(
        `${API_BASE_URL}/payments/cancel`,
        { imp_uid: impUid, reason },
        { withCredentials: true }
      );
      if (res.data.success) {
        setCancelSuccess(true);
        setPendingReservationId(reservationId);
      } else {
        alert('결제취소 실패: ' + res.data.message);
      }
    } catch (e) {
      alert('결제취소 중 오류 발생');
    }
  };

  if (loading) return <div>로딩중...</div>;
  if (!reservations.length) return <div>예매내역이 없습니다.</div>;

  return (
    <div className="reservation-list" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {cancelSuccess && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          background: 'rgba(0,0,0,0.3)',
          zIndex: 2000,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <div style={{ background: '#fff', padding: 32, borderRadius: 12, boxShadow: '0 2px 12px rgba(0,0,0,0.18)', minWidth: 280 }}>
            <div style={{ fontSize: 20, fontWeight: 600, marginBottom: 16 }}>결제 취소가 완료되었습니다.</div>
            <button onClick={() => {
              setCancelSuccess(false);
              if (pendingReservationId) {
                onSelectReservation(pendingReservationId);
                setPendingReservationId(null);
              }
            }} style={{ padding: '8px 24px', borderRadius: 8, background: '#5c6bc0', color: '#fff', border: 'none', fontWeight: 600, fontSize: 16 }}>확인</button>
          </div>
        </div>
      )}
      {reservations.slice().reverse().map(r => {
        const payment = r.payments?.[0];
        const isCancellable = (payment?.status === 'PAID' || payment?.status === 'SUCCESS') && !payment?.cancelled;
        return (
          <div
            className="reservation-card"
            key={r.reservationId}
            style={{
              display: 'flex',
              alignItems: 'center',
              background: '#fff',
              borderRadius: '12px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
              padding: '20px',
              gap: '24px'
            }}
          >
            <img
              src={r.screening?.posterUrl || '/placeholder-actor.png'}
              alt="포스터"
              style={{ width: 90, height: 120, objectFit: 'cover', borderRadius: '8px', background: '#eee' }}
            />
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 'bold', fontSize: 18 }}>{r.screening?.movieNm}</div>
              <div style={{ color: '#666', margin: '4px 0' }}>
                {r.cinema?.name} / {r.theater?.name}
              </div>
              <div style={{ color: '#666', marginBottom: 4 }}>
                {r.screening?.startTime?.replace('T', ' ').slice(0, 16)}
              </div>
              <div style={{ color: '#333', marginBottom: 4 }}>
                좌석: <b>{r.seats?.map(s => s.seatNumber).join(', ')}</b>
              </div>
              <div>
                결제상태:{' '}
                <span
                  style={{
                    display: 'inline-block',
                    padding: '2px 10px',
                    borderRadius: 8,
                    background: payment?.status === 'PAID' ? '#e0f7fa' : payment?.status === 'SUCCESS' ? '#ffe0e0' : '#eee',
                    color: payment?.status === 'PAID' ? '#00796b' : payment?.status === 'SUCCESS' ? '#c62828' : '#888',
                    fontWeight: 600,
                    fontSize: 13
                  }}
                >
                  {payment?.status || 'N/A'}
                </span>
              </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <button
                style={{
                  background: '#d32f2f',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 8,
                  padding: '10px 18px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  marginBottom: 8
                }}
                onClick={() => onSelectReservation(r.reservationId)}
              >
                예매상세/영수증
              </button>
              {isCancellable && (
                <button
                  style={{
                    background: '#888',
                    color: '#fff',
                    border: 'none',
                    borderRadius: 8,
                    padding: '8px 14px',
                    fontWeight: 500,
                    cursor: 'pointer',
                  }}
                  onClick={() => handleCancelPayment(payment, r.reservationId)}
                >
                  결제취소
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default UserReservations; 