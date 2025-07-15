import React, { useState } from 'react';
import styles from './CommentModal.module.css';

const CommentModal = ({
  open,
  onClose,
  reviewId,
  onSave,
  userId
}) => {
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const maxLength = 1000;

  // 욕설 필터링 (ReviewModal과 동일한 리스트 사용)
  const forbiddenWords = [
    "씨발","시발", "병신", "개새끼", "미친", "바보", "멍청이", "돌아이", "등신", "호구", "찌질이",
    "fuck", "shit", "bitch", "asshole", "damn", "hell", "bastard", "dick", "pussy", "cock",
    "씨발놈", "씨발년", "씨팔", "씨빨", "씨바", "ㅆㅂ",
    "좆", "좃", "존나", "개년", "개같", "미친놈", "미친년",
    "ㅈㄴ", "ㅈ같", "븅신", "병쉰", "ㅂㅅ",
    "씹", "씹새끼", "씹년", "씹할", "쌍놈", "쌍년", "죽어버려",
    "꺼져", "좇같", "좇같이", "좇같은", "개씨발", "애미", "애비",
    "좆같", "좃같", "좆빠", "좃빠", "좃빨", "좆빨",
    "빨아", "걸레", "보지", "보짓", "보져", "보전",
    "애미뒤진", "애비뒤진", "엿같", "엿머",
    "닥쳐", "지랄", "지럴", "ㅈㄹ", "몰라씨발",
    "헐좃", "지같", "후장", "뒈져", "뒤져",
    "니미", "니미럴", "니애미", "니애비",
    "개노답", "좆노답", "썅", "ㅅㅂ", "ㅄ",
    "꺼지라", "개지랄", "대가리깨져", "꺼지라고", "개빡쳐",
    "씨댕", "시댕", "씨댕이", "시댕이",
    "똥같", "지랄맞", "개도살", "개패듯", "졸라",
    "지옥가라", "개후려", "후려패", "싸가지", "개망나니",
    "지랄발광", "미친개", "개지옥", "좇밥", "좃밥",
    "개털려", "개처맞", "처맞는다", "처발린다",
    "개쳐맞", "쳐죽일", "좆빨아", "좇빨아", "개한심", "극혐"
  ];

  const containsForbiddenWords = (text) => {
    if (!text) return false;
    return forbiddenWords.some(word => text.includes(word));
  };

  const handleSave = async () => {
    if (!comment.trim()) {
      alert('댓글 내용을 입력해주세요.');
      return;
    }

    // 욕설 필터링
    if (containsForbiddenWords(comment)) {
      const proceed = window.confirm('클린봇에 의해 게시가 제한될 수 있습니다. 그래도 작성하시겠습니까?');
      if (!proceed) return;
    }

    setLoading(true);
    try {
      const response = await fetch('http://localhost:80/api/comments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          reviewId: reviewId,
          userId: userId,
          content: comment.trim(),
        }),
        credentials: 'include',
      });

      const data = await response.json();
      
      if (data.success) {
        alert('댓글이 작성되었습니다!');
        if (onSave) onSave(comment);
        onClose();
      } else {
        alert(data.message || '댓글 작성에 실패했습니다.');
      }
    } catch (error) {
      console.error('댓글 작성 오류:', error);
      alert('댓글 작성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setComment('');
    onClose();
  };

  if (!open) return null;

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContainer}>
        <div className={styles.header}>
          <span className={styles.title}>댓글</span>
          <button className={styles.closeBtn} onClick={handleClose}>×</button>
        </div>
        <hr className={styles.divider} />
        <textarea
          className={styles.textarea}
          placeholder="이 작품에 대한 생각을 자유롭게 표현해주세요."
          value={comment}
          onChange={e => setComment(e.target.value)}
          maxLength={maxLength}
          disabled={loading}
        />
        <div className={styles.footer}>
          <span className={styles.length}>{comment.length}/{maxLength}</span>
          <button
            className={styles.saveBtn}
            onClick={handleSave}
            disabled={comment.length === 0 || loading}
          >
            {loading ? '작성 중...' : '저장'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CommentModal; 