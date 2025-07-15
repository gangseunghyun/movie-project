import React, { useState, useEffect } from 'react';
import styles from './CommentModal.module.css';
import starFull from '../../assets/star_full.svg';
import starHalf from '../../assets/star_half.svg';
import starEmpty from '../../assets/star_empty.svg';

const CommentModal = ({
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
  const maxLength = 10000;

  useEffect(() => {
    if (editMode) {
      setComment(initialContent);
      setRating(initialRating);
    } else {
      setComment('');
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
      const response = await fetch('http://localhost:80/api/ratings', {
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
        const ratingResponse = await fetch(`http://localhost:80/api/ratings/${movieCd}`, {
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
        //alert(data.message || '별점이 저장되었습니다.');
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

  const handleSave = async () => {
    // 코멘트 작성 시 별점이 0이면 경고
    if (!editMode && userRating === 0) {
      alert('별점을 먼저 입력해주세요.');
      return;
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
          //alert('리뷰가 수정되었습니다!');
          if (onEditSave) onEditSave(comment, latestRating);
          onClose();
        } else {
          alert(data.message || '리뷰 수정에 실패했습니다.');
        }
      } else {
        // POST 요청 (작성) - MovieDetailHeader에서 설정한 별점 사용
        const response = await fetch('http://localhost:80/api/reviews', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            movieCd,
            content: comment,
            rating: Number(userRating), // 숫자로 변환
          }),
          credentials: 'include',
        });
        const data = await response.json();
        if (data.success) {
          //alert('리뷰가 작성되었습니다!');
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
        <div className={styles.footer}>
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
              {/* {ratingLoading && <span className={styles.loadingText}>별점 저장 중...</span>} */}
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

export default CommentModal; 