import React, { useEffect, useState } from 'react';
import axios from 'axios';

function ReservationReceipt({ reservationId, onBack, currentUser }) {
  const [receipt, setReceipt] = useState(null);
  const [loading, setLoading] = useState(true);
  const userId = currentUser?.id;
  const [showFullReceipt, setShowFullReceipt] = useState(false);
  const [showFullImpUid, setShowFullImpUid] = useState(false);
  const [showFullMerchantUid, setShowFullMerchantUid] = useState(false);

  useEffect(() => {
    if (userId && reservationId) {
      axios.get(`http://localhost/api/users/${userId}/reservations/${reservationId}`)
        .then(res => {
          setReceipt(res.data);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [userId, reservationId]);

  if (loading) return <div>로딩중...</div>;
  if (!receipt) return <div>예매정보를 불러올 수 없습니다.</div>;

  const { screening, cinema, theater, seats, payments } = receipt;
  const payment = payments?.[0];

  function shortId(id) {
    if (!id) return '';
    if (id.length <= 16) return id;
    return id.slice(0, 8) + '...' + id.slice(-8);
  }

  return (
    <div style={{
      maxWidth: 480,
      margin: '0 auto',
      background: '#fff',
      borderRadius: 16,
      boxShadow: '0 4px 24px rgba(0,0,0,0.12)',
      padding: 32,
      color: '#222',
      position: 'relative'
    }}>
      <button onClick={onBack} style={{
        background: 'none', border: 'none', color: '#5c6bc0', fontWeight: 600, fontSize: 16, marginBottom: 16, cursor: 'pointer'
      }}>← 목록으로</button>
      <h2 style={{marginBottom: 12}}>🎟️ 예매 영수증</h2>
      <div style={{marginBottom: 18, borderBottom: '1px solid #e0e0e0', paddingBottom: 12}}>
        <div><b>🎬 영화명:</b> {screening?.movieNm}</div>
        <div><b>🏢 영화관:</b> {cinema?.name}</div>
        <div><b>🏟️ 상영관:</b> {theater?.name}</div>
        <div><b>🕒 상영일시:</b> {screening?.startTime?.replace('T', ' ').slice(0, 16)}</div>
        <div><b>💺 좌석:</b> {seats?.map(s => s.seatNumber).join(', ')}</div>
        <div><b>🗓️ 예매일시:</b> {receipt.reservedAt?.replace('T', ' ').slice(0, 16)}</div>
      </div>
      <div style={{marginBottom: 18}}>
        <h3 style={{margin: '12px 0 8px 0'}}>💳 결제 상세정보</h3>
        <div style={{display: 'flex', flexWrap: 'wrap', gap: 8}}>
          <div style={{flex: '1 1 45%'}}><b>결제금액:</b> <span style={{color:'#1976d2', fontWeight:700}}>{receipt.totalAmount?.toLocaleString()}원</span></div>
          <div style={{flex: '1 1 45%'}}><b>결제수단:</b> {payment?.method}</div>
          <div style={{flex: '1 1 45%'}}><b>결제상태:</b> <span style={{
            color: payment?.status === 'PAID' ? '#388e3c' : payment?.status === 'CANCELLED' ? '#d32f2f' : '#888',
            fontWeight: 700
          }}>{payment?.status}</span></div>
          <div style={{flex: '1 1 45%'}}><b>결제일시:</b> {payment?.paidAt?.replace('T', ' ').slice(0, 16)}</div>
          <div style={{flex: '1 1 45%'}}><b>결제번호:</b> <span style={{fontFamily: 'monospace', fontSize: 13, color: '#555'}}>{payment?.impUid}</span></div>
          <div style={{flex: '1 1 45%'}}><b>주문번호:</b> <span style={{fontFamily: 'monospace', fontSize: 13, color: '#555'}}>{payment?.merchantUid}</span></div>
          <div style={{flex: '1 1 45%'}}><b>영수증번호:</b> <span title={payment?.receiptNumber} style={{fontFamily: 'monospace', fontSize: 13, color: '#555', cursor: 'pointer'}} onClick={() => setShowFullReceipt(v => !v)}>{showFullReceipt ? payment?.receiptNumber : shortId(payment?.receiptNumber)}</span></div>
          <div style={{flex: '1 1 45%'}}><b>결제자명:</b> {payment?.userName}</div>
          <div style={{flex: '1 1 45%'}}><b>카드사명:</b> {payment?.cardName}</div>
          <div style={{flex: '1 1 45%'}}><b>카드번호(끝4자리):</b> {payment?.cardNumberSuffix}</div>
          <div style={{flex: '1 1 45%'}}><b>승인번호:</b> {payment?.approvalNumber}</div>
          <div style={{flex: '1 1 45%'}}><b>PG응답코드:</b> {payment?.pgResponseCode}</div>
          <div style={{flex: '1 1 45%'}}><b>PG응답메시지:</b> {payment?.pgResponseMessage}</div>
          <div style={{flex: '1 1 45%'}}><b>환불정보:</b> {payment?.cancelled ? `취소됨 (${payment?.cancelReason || ''} ${payment?.cancelledAt ? payment.cancelledAt.replace('T', ' ').slice(0, 16) : ''})` : '정상'}</div>
        </div>
        {payment?.receiptUrl && <div style={{marginTop: 12}}><a href={payment.receiptUrl} target="_blank" rel="noopener noreferrer" style={{color:'#1976d2', fontWeight:600}}>영수증 바로가기</a></div>}
      </div>
    </div>
  );
}

export default ReservationReceipt; 