import React, { useState, useEffect } from 'react';
import styles from './ReviewModal.module.css';
import starFull from '../../assets/star_full.svg';
import starHalf from '../../assets/star_half.svg';
import starEmpty from '../../assets/star_empty.svg';

const ReviewModal = ({
  open,
  onClose,
  movieTitle,
  movieCd,
  userRating,
  onSave,
  // 수정 모드 관련 props
  editMode = false,
  initialContent = '',
  initialRating = 0,
  onEditSave,
  reviewId
}) => {
  const [comment, setComment] = useState(initialContent);
  const [rating, setRating] = useState(editMode ? initialRating : userRating);
  const [hoverRating, setHoverRating] = useState(0);
  const [loading, setLoading] = useState(false);
  const [ratingLoading, setRatingLoading] = useState(false);
  const [spoiler, setSpoiler] = useState(false);
  const [isChecking, setIsChecking] = useState(true);
  const [hasExistingReview, setHasExistingReview] = useState(false);
  const [existingReviewMessage, setExistingReviewMessage] = useState('');
  const maxLength = 10000;

  // 욕설 리스트 (백엔드와 동일하게 맞추는 것이 이상적)
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

  // 기존 리뷰 확인
  useEffect(() => {
    const checkExistingReview = async () => {
      if (!movieCd || editMode) {
        setIsChecking(false);
        return;
      }

      try {
        const response = await fetch(`/api/reviews/movie/${movieCd}/check-user-review`, {
          credentials: 'include',
        });
        const data = await response.json();
        
        if (data.success) {
          if (data.hasReview) {
            setHasExistingReview(true);
            setExistingReviewMessage(data.message);
          }
        }
      } catch (error) {
        console.error('리뷰 확인 중 오류:', error);
      } finally {
        setIsChecking(false);
      }
    };

    if (open && !editMode) {
      checkExistingReview();
    } else {
      setIsChecking(false);
    }
  }, [open, movieCd, editMode]);

  useEffect(() => {
    if (editMode) {
      setComment(initialContent);
      setRating(initialRating);
    } else {
      setComment('');
      setSpoiler(false);
      // 코멘트 작성 시에는 MovieDetailHeader에서 설정한 별점 사용
      setRating(userRating);
    }
  }, [open, editMode, initialContent, initialRating, userRating]);

  // 별점 저장 API 호출 함수
  const saveRating = async (score) => {
    if (!movieCd) {
      alert('영화 정보가 없습니다.');
      return score;
    }

    setRatingLoading(true);
    try {
      const response = await fetch('/api/ratings', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          movieCd: movieCd,
          score: score
        })
      });

      const data = await response.json();

      // 별점 저장 성공/실패와 관계없이 항상 최신 별점 재조회
      try {
        const ratingResponse = await fetch(`/api/ratings/${movieCd}`, {
          credentials: 'include',
        });
        if (ratingResponse.ok) {
          const ratingData = await ratingResponse.json();
          if (ratingData.success && ratingData.data) {
            setRating(ratingData.data.score);
            return ratingData.data.score;
          } else {
            setRating(score);
            return score;
          }
        } else {
          setRating(score);
          return score;
        }
      } catch (error) {
        setRating(score);
        return score;
      }

      if (data.success) {
        alert(data.message || '별점이 저장되었습니다.');
      } else {
        alert(data.message || '별점 저장에 실패했습니다.');
      }
    } catch (error) {
      setRating(score);
      alert('별점 저장 중 오류가 발생했습니다.');
      return score;
    } finally {
      setRatingLoading(false);
    }
  };

  // 별점 클릭 핸들러
  const handleStarClick = async (e, value) => {
    if (!editMode || ratingLoading) return; // 수정 모드 & 저장 중 아닐 때만 가능

    const rect = e.target.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const score = x < rect.width / 2 ? value - 0.5 : value;
    
    setRating(Number(score));
    await saveRating(score); // 별점 저장 및 최신화 (항상 서버에서 받아옴)
  };

  if (!open) return null;

  // 기존 리뷰 확인 중일 때
  if (isChecking) {
    return (
      <div className={styles.modalOverlay}>
        <div className={styles.modalContainer}>
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <div>리뷰 작성 여부를 확인하고 있습니다...</div>
          </div>
        </div>
      </div>
    );
  }

  // 기존 리뷰가 있을 때
  if (hasExistingReview) {
    return (
      <div className={styles.modalOverlay}>
        <div className={styles.modalContainer}>
          <div className={styles.header}>
            <span className={styles.title}>{movieTitle}</span>
            <button className={styles.closeBtn} onClick={onClose}>×</button>
          </div>
          <hr className={styles.divider} />
          <div style={{ 
            textAlign: 'center', 
            padding: '40px 20px',
            color: '#ff2f6e',
            fontSize: '18px',
            fontWeight: 'bold'
          }}>
            <div style={{ marginBottom: '20px' }}>
              <span style={{ fontSize: '48px', display: 'block', marginBottom: '16px' }}>⚠️</span>
              {existingReviewMessage}
            </div>
            <div style={{ 
              color: '#666', 
              fontSize: '14px',
              fontWeight: 'normal'
            }}>
              한 영화당 하나의 리뷰만 작성할 수 있습니다.
            </div>
          </div>
          <div style={{ textAlign: 'center', marginTop: '20px' }}>
            <button
              className={styles.saveBtn}
              onClick={onClose}
              style={{
                background: '#ff2f6e',
                color: 'white',
                border: 'none',
                borderRadius: 10,
                padding: '12px 32px',
                fontSize: 16,
                cursor: 'pointer'
              }}
            >
              확인
            </button>
          </div>
        </div>
      </div>
    );
  }

  const handleSave = async () => {
    // 코멘트 작성 시 별점이 0이면 경고
    if (!editMode && userRating === 0) {
      alert('별점을 먼저 입력해주세요.');
      return;
    }

    // 욕설 필터링
    if (containsForbiddenWords(comment)) {
      const proceed = window.confirm('클린봇에 의해 게시가 제한될 수 있습니다. 그래도 작성하시겠습니까?');
      if (!proceed) return;
    }

    setLoading(true);
    try {
      if (editMode) {
        // 별점이 바뀌었으면 별점 저장 API 호출
        let latestRating = rating;
        if (rating !== initialRating) {
          latestRating = await saveRating(rating); // 별점 저장 및 최신화
        }
        // 리뷰 수정 요청 (별점도 같이 넘김)
        const response = await fetch(`/api/reviews/${reviewId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({
            content: comment,
            rating: Number(latestRating), // 숫자로 변환 (소수점 허용)
          }),
        });
        const data = await response.json();
        if (data.success) {
          alert('리뷰가 수정되었습니다!');
          if (onEditSave) onEditSave(comment, latestRating);
          // 평점 변경 시 페이지 새로고침으로 댓글 모달 업데이트
          window.location.reload();
          onClose();
        } else {
          alert(data.message || '리뷰 수정에 실패했습니다.');
        }
      } else {
        // POST 요청 (작성) - MovieDetailHeader에서 설정한 별점 사용
        const response = await fetch('/api/reviews', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            movieCd,
            content: comment,
            rating: Number(userRating), // 숫자로 변환
            spoiler: spoiler, // 스포일러 옵션 추가
          }),
          credentials: 'include',
        });
        const data = await response.json();
        if (data.success) {
          alert('리뷰가 작성되었습니다!');
          if (onSave) onSave(comment, userRating);
          onClose();
        } else {
          alert(data.message || '리뷰 작성에 실패했습니다.');
        }
      }
    } catch (e) {
      alert('리뷰 저장 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContainer}>
        <div className={styles.header}>
          <span className={styles.title}>
            {movieTitle}
            <span className={styles.star}>★ {editMode ? rating : userRating}</span>
          </span>
          <button className={styles.closeBtn} onClick={onClose}>×</button>
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

        <hr className={styles.divider} />
        
        {/* 스포일러 옵션 */}


        <div className={styles.footer}>
            {!editMode && (
                      <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        marginRight: 8
                      }}>
                        <span style={{ fontSize: 24, marginRight: 8 }}>✗</span>
                        <span style={{ marginRight: 8 }}>스포일러</span>
                        <label className="switch">
                          <input
                            type="checkbox"
                            checked={spoiler}
                            onChange={e => setSpoiler(e.target.checked)}
                            disabled={loading}
                          />
                          <span className="slider round"></span>
                        </label>
                      </div>
                    )}
          {editMode && (
            <div className={styles.starInputRow}>
              <label className={styles.starInputLabel}>평가하기 </label>
              <div className={styles.starInputIcons}>
                {[...Array(5)].map((_, i) => {
                  const value = i + 1;
                  let starImg = starEmpty;
                  if ((hoverRating ? hoverRating : rating) >= value) {
                    starImg = starFull;
                  } else if ((hoverRating ? hoverRating : rating) >= value - 0.5) {
                    starImg = starHalf;
                  }
                  return (
                    <img
                      key={i}
                      src={starImg}
                      alt={`${value}점`}
                      onClick={e => handleStarClick(e, value)}
                      onMouseMove={e => {
                        const rect = e.target.getBoundingClientRect();
                        const x = e.clientX - rect.left;
                        if (x < rect.width / 2) {
                          setHoverRating(value - 0.5);
                        } else {
                          setHoverRating(value);
                        }
                      }}
                      onMouseLeave={() => setHoverRating(0)}
                      className={`${styles.starIcon}${(loading || ratingLoading) ? ' ' + styles.disabled : ''}`}
                      role="button"
                      aria-label={`${value}점 주기`}
                      style={{ cursor: (loading || ratingLoading) ? 'not-allowed' : 'pointer' }}
                      disabled={loading || ratingLoading}
                    />
                  );
                })}
              </div>
              {ratingLoading && <span className={styles.loadingText}>별점 저장 중...</span>}
            </div>
          )}
          <span className={styles.length}>{comment.length}/{maxLength}</span>
          <button
            className={styles.saveBtn}
            onClick={handleSave}
            disabled={comment.length === 0 || loading || ratingLoading}
          >
            {loading ? (editMode ? '수정 중...' : '저장 중...') : (editMode ? '수정' : '저장')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ReviewModal; 