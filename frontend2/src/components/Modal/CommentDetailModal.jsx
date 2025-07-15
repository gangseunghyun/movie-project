import React, { useEffect, useState, useCallback } from 'react';
import styles from './CommentDetailModal.module.css';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import userIcon from '../../assets/user_icon.png';

import commentIcon2 from '../../assets/comment_icon2.png';
import ReplyModal from './ReplyModal';
import { useUser } from '../../contexts/UserContext';
import previousIcon from '../../assets/previous_icon_c.png';

// 대댓글(Reply) 카드 컴포넌트
function ReplyCard({ reply, userId, onEdit, onDelete, onLike }) {
  const isMine = userId && (reply.userId === userId);

  const handleLikeClick = () => {
    if (onLike) {
      onLike(reply.id, reply.likedByMe);
    }
  };

  return (
    <div className={styles.replyCard}>

      <div className={styles.replyContentWrap}>
        <div className={styles.replyHeader}>
          <img
            src={reply.userProfileImageUrl && reply.userProfileImageUrl.trim() !== '' ? reply.userProfileImageUrl : userIcon}
            alt="프로필"
            className={styles.replyUserIcon}
          />
          <span className={styles.replyUser}>{reply.userNickname || reply.user || '익명'}</span>
          <span className={styles.replyDate}>{getRelativeDate(reply.updatedAt || reply.createdDate)}</span>
          <div className={styles.replyBtnGroup}>
            {isMine && (
              <>
                <button className={styles.replyEditBtn} onClick={onEdit}>수정</button>
                <button className={styles.replyDeleteBtn} onClick={onDelete}>삭제</button>
              </>
            )}
          </div>
        </div>
        <div className={styles.replyContent}>{reply.content}</div>
        <div className={styles.replyFooter}>
          <img
            src={reply.likedByMe ? likeIconTrue : likeIcon}
            alt="좋아요"
            className={styles.replyLikeIcon}
            onClick={handleLikeClick}
            style={{ cursor: 'pointer' }}
          />
          <span>{reply.likeCount ?? 0}</span>

        </div>
        <hr className={styles.replyDivider} />
      </div>
    </div>
  );
}

function getRelativeDate(dateString) {
  if (!dateString) return '';
  const date = new Date(dateString);
  const now = new Date();

  // 오늘 날짜(연, 월, 일)만 비교
  const dateYMD = date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
  const nowYMD = now.getFullYear() + '-' + (now.getMonth() + 1) + '-' + now.getDate();

  if (dateYMD === nowYMD) return '오늘';

  // 며칠 전 계산
  const diffTime = now.setHours(0, 0, 0, 0) - date.setHours(0, 0, 0, 0);
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
  return `${diffDays}일 전`;
}
// 코멘트 상세 + 대댓글 리스트 모달
const CommentDetailModal = ({ open, onClose, onBack, comment, reviewId, fetchComments, fetchMyComments, fetchLikedComments }) => {
  const { user } = useUser();
  const myUserId = user?.id;
  const [localComment, setLocalComment] = useState(comment);
  useEffect(() => {
    setLocalComment(comment);
  }, [comment]);
  const [replies, setReplies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [replyModalOpen, setReplyModalOpen] = useState(false);
  const [editReplyModalOpen, setEditReplyModalOpen] = useState(false);
  const [editTargetReply, setEditTargetReply] = useState(null);
  // 좋아요 상태를 로컬에서 관리하기 위한 상태
  const [localLikeStates, setLocalLikeStates] = useState({});
  // 좋아요 수를 로컬에서 관리하기 위한 상태
  const [localLikeCounts, setLocalLikeCounts] = useState({});

  useEffect(() => {
    if (open && comment) {
      console.log('[CommentDetailModal] 전달받은 comment:', comment);
    }
  }, [open, comment]);

  // 대댓글 목록 불러오기 함수 분리
  const fetchReplies = useCallback(async () => {
    if (!open || !reviewId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`http://localhost:80/api/comments/review/${reviewId}/all${myUserId ? `?userId=${myUserId}` : ''}`, {
        credentials: 'include',
      });
      const data = await res.json();
      if (data.success) {
       
        // 각 대댓글의 최신 좋아요 수를 병렬로 가져오기
        const replies = data.data || [];
        const repliesWithLikeCount = await Promise.all(
          replies.map(async reply => {
            try {
              const resLike = await fetch(`http://localhost:80/api/comments/${reply.id}/like-count`, {
                credentials: 'include',
              });
              const likeData = await resLike.json();
              return {
                ...reply,
                likeCount: likeData.success ? likeData.likeCount : reply.likeCount
              };
            } catch {
              return reply;
            }
          })
        );
        // 기존 병합 로직 적용
        const repliesWithLikeStatus = repliesWithLikeCount.map(reply => {
          const finalLikeCount = (localLikeCounts[reply.id] !== undefined)
            ? localLikeCounts[reply.id]
            : reply.likeCount;
          const finalLikedByMe = (localLikeStates[reply.id] !== undefined)
            ? localLikeStates[reply.id]
            : (reply.likedByMe !== undefined ? reply.likedByMe : false);
          return {
            ...reply,
            likedByMe: finalLikedByMe,
            likeCount: finalLikeCount
          };
        });
        setReplies(repliesWithLikeStatus);
      } else {
        setError(data.message || '대댓글 불러오기 실패');
      }
    } catch (e) {
      setError('대댓글 불러오기 실패');
    } finally {
      setLoading(false);
    }
  }, [open, reviewId, localLikeStates, localLikeCounts, myUserId]);

  // 좋아요 클릭 핸들러
  const handleLike = async () => {
    if (!reviewId) return;
    try {
      let res;
      if (localComment?.likedByMe) {
        // 좋아요 취소 (DELETE)
        res = await fetch(`http://localhost:80/api/reviews/dto/${reviewId}/like`, {
          method: 'DELETE',
          credentials: 'include',
        });
      } else {
        // 좋아요 (POST)
        res = await fetch(`http://localhost:80/api/reviews/dto/${reviewId}/like`, {
          method: 'POST',
          credentials: 'include',
        });
      }
      if (res.ok) {
        setLocalComment(prev => ({
          ...prev,
          likedByMe: !prev.likedByMe,
          likeCount: prev.likedByMe
            ? (prev.likeCount ?? 1) - 1
            : (prev.likeCount ?? 0) + 1,
        }));
        if (fetchComments) fetchComments();
        if (fetchMyComments) fetchMyComments();
        if (fetchLikedComments) fetchLikedComments();
        fetchReplies();
      } else if (res.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('좋아요 처리 실패');
      }
    } catch (e) {
      alert('네트워크 오류');
    }
  };

  // 수정 버튼 클릭 핸들러
  const handleEditReply = (reply) => {
    setEditTargetReply(reply);
    setEditReplyModalOpen(true);
  };

  // 삭제 버튼 클릭 핸들러
  const handleDeleteReply = async (reply) => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return;
    try {
      const res = await fetch(`http://localhost:80/api/comments/${reply.id}?userId=${myUserId}`, {
        method: 'DELETE',
        credentials: 'include',
      });
      const data = await res.json();
      if (data.success) {
        //alert('댓글이 삭제되었습니다.');
        fetchReplies();
        if (fetchComments) fetchComments();
        setLocalComment(prev => ({
          ...prev,
          commentCount: Math.max((prev?.commentCount ?? 1) - 1, 0)
        }));
      } else {
        alert(data.message || '삭제 실패');
      }
    } catch (e) {
      alert('네트워크 오류');
    }
  };

  // 좋아요 수를 서버에서 받아와 동기화하는 함수
  const updateLikeCountFromServer = async (commentId) => {
    try {
      const res = await fetch(`http://localhost:80/api/comments/${commentId}/like-count`, {
        credentials: 'include',
      });
      const data = await res.json();
      if (data.success) {
        console.log(data.data);
        setLocalLikeCounts(prev => ({
          ...prev,
          [commentId]: data.likeCount
        }));
        setReplies(prevReplies =>
          prevReplies.map(reply =>
            reply.id === commentId
              ? { ...reply, likeCount: data.likeCount }
              : reply
          )
        );
      }
    } catch (e) {
      console.log('실패');
      // 실패 시 무시(프론트 값 유지)
    }
  };

  // 대댓글 좋아요 클릭 핸들러
  const handleReplyLike = async (commentId, likedByMe) => {
    if (!myUserId) {
      alert('로그인이 필요합니다.');
      return;
    }
    // 현재 likeCount 가져오기
    const replyLikeCount = replies.find(r => r.id === commentId)?.likeCount;
    try {
      let res;
      if (likedByMe) {
        // 좋아요 취소 (DELETE)
        res = await fetch(`http://localhost:80/api/comments/${commentId}/like?userId=${myUserId}`, {
          method: 'DELETE',
          credentials: 'include',
        });
      } else {
        // 좋아요 (POST)
        res = await fetch(`http://localhost:80/api/comments/${commentId}/like`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ userId: myUserId }),
          credentials: 'include',
        });
      }
      const data = await res.json();
      if (data.success) {
        // 로컬 좋아요 상태 및 카운트 업데이트
        setLocalLikeStates(prev => ({
          ...prev,
          [commentId]: !likedByMe
        }));
        setLocalLikeCounts(prev => ({
          ...prev,
          [commentId]: !likedByMe
            ? (replyLikeCount ?? 0) + 1
            : Math.max((replyLikeCount ?? 1) - 1, 0)
        }));
        setReplies(prevReplies =>
          prevReplies.map(reply =>
            reply.id === commentId
              ? {
                ...reply,
                likedByMe: !likedByMe,
                likeCount: !likedByMe
                  ? (replyLikeCount ?? 0) + 1
                  : Math.max((replyLikeCount ?? 1) - 1, 0)
              }
              : reply
          )
        );
        // 서버에서 최신 좋아요 수 동기화
        await updateLikeCountFromServer(commentId);
      } else {
        alert(data.message || '좋아요 처리 실패');
      }
    } catch (e) {
      alert('네트워크 오류');
    }
  };

  useEffect(() => {
    if (open) {
      fetchReplies();
    }
  }, [fetchReplies]);

  if (!open || !comment) return null;

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContainer}>
        {/* 상단: 코멘트 카드 (MovieDetailBody 스타일 재사용) */}
        <div className={styles.modalHeaderButtons}>
          <button className={styles.prevBtn} onClick={onBack}>
            <img src={previousIcon} alt="이전" className={styles.prevIconBtnImg} />
          </button>
          <button className={styles.closeBtn} onClick={onClose}>×</button>
        </div>
        <div className={styles.commentCard}>
          <div className={styles.commentHeader}>
            <div className={styles.commentHeaderLeft}>
              <img
                src={comment.userProfileImageUrl && comment.userProfileImageUrl.trim() !== '' ? comment.userProfileImageUrl : userIcon}
                alt="프로필"
                className={styles.commentUserProfileImage}
              />
              <span className={styles.commentUser}>{comment.userNickname || comment.user || '익명'}</span>
              <span className={styles.commentDate}>{getRelativeDate(comment.updatedAt || comment.createdDate || comment.date)}</span>
            </div>
            <span className={styles.commentRating}>★ {comment.rating ? comment.rating.toFixed(1) : '-'}</span>
          </div>
          <hr className={styles.commentDivider} />
          <div className={styles.commentContent}>{comment.content}</div>
          <hr className={styles.commentFooterDivider} />
          <div className={styles.commentFooter}>
            <span>좋아요 {localComment?.likeCount ?? 0}</span>
            <span>댓글 {localComment?.commentCount ?? 0}</span>
            <span className={styles.commentDate}>{localComment?.createdDate || localComment?.date}</span>
          </div>
          <div className={styles.commentIconRow}>
            <img
              src={localComment?.likedByMe ? likeIconTrue : likeIcon}
              alt="좋아요"
              className={styles.commentIcon}
              onClick={handleLike}
              style={{ cursor: 'pointer' }}
            />
            <img
              src={commentIcon2}
              alt="댓글"
              className={styles.commentIcon}
              style={{ cursor: 'pointer' }}
              onClick={() => setReplyModalOpen(true)}
            />
          </div>
          {/* 대댓글 작성 모달 */}
          <ReplyModal
            open={replyModalOpen}
            onClose={() => setReplyModalOpen(false)}
            reviewId={reviewId}
            parentId={null}
            isReply={true}
            onSave={() => {
              setReplyModalOpen(false);
              fetchReplies();
              if (fetchComments) fetchComments();
              setLocalComment(prev => ({
                ...prev,
                commentCount: (prev?.commentCount ?? 0) + 1
              }));
            }}
          />
        </div>

        {/* 하단: 대댓글 리스트 */}
        <div className={styles.replySection}>

          {/* {loading && <div>로딩 중...</div>} */}
          {error && <div style={{ color: 'red' }}>{error}</div>}
          {!loading && !error && replies.length === 0 && <div>아직 댓글이 없습니다.</div>}
          <div className={styles.replyList}>
            {replies.map((reply, idx) => (
              <ReplyCard
                key={`reply-${reply.id}-${idx}`}
                reply={reply}
                userId={myUserId}
                onEdit={() => handleEditReply(reply)}
                onDelete={() => handleDeleteReply(reply)}
                onLike={handleReplyLike}
              />
            ))}
          </div>
        </div>
        {/* 대댓글 수정 모달 */}
        <ReplyModal
          open={editReplyModalOpen}
          onClose={() => setEditReplyModalOpen(false)}
          reviewId={reviewId}
          parentId={null}
          isReply={true}
          editMode={true}
          editTarget={editTargetReply}
          onUpdate={() => {
            setEditReplyModalOpen(false);
            fetchReplies();
            if (fetchComments) fetchComments();
          }}
        />
      </div>
    </div>
  );
};

export default CommentDetailModal; 