import React, { useEffect, useState } from "react";
import styles from "./MyPageFooter.module.css";
import { useUser } from '../../contexts/UserContext';
import likeIcon from '../../assets/like_icon.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import commentIcon2 from '../../assets/comment_icon2.png';
import CommentDetailModal from '../Modal/CommentDetailModal';
import CommentModal from '../Modal/CommentModal';
import LikedCommentsModal from '../Modal/LikedCommentsModal';
import MyCommentsModal from '../Modal/MyCommentsModal';

function formatRelativeTime(dateString) {
  if (!dateString) return '';
  const now = new Date();
  const commentDate = new Date(dateString);
  const diffTime = now.getTime() - commentDate.getTime();
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return '오늘';
  if (diffDays === 1) return '어제';
  if (diffDays < 7) return `${diffDays}일 전`;
  return commentDate.toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' });
}

const MyPageFooter = ({ targetUserId, tempUserInfo, targetUser: propTargetUser }) => {
  const { user } = useUser();
  const [myComments, setMyComments] = useState([]);
  const [likedComments, setLikedComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [likedLoading, setLikedLoading] = useState(true);
  // 모달 상태 및 선택된 코멘트 관리
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedComment, setSelectedComment] = useState(null);
  // 수정 모달 상태 및 타겟
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [localTempUserInfo, setLocalTempUserInfo] = useState(tempUserInfo);
  const [likedModalOpen, setLikedModalOpen] = useState(false);
  const [myCommentsModalOpen, setMyCommentsModalOpen] = useState(false);
  const [lastModalType, setLastModalType] = useState(null);

  // sessionStorage에서 tempUserInfo 확인 (새로고침 시에도 유지)
  useEffect(() => {
    const storedUserInfo = sessionStorage.getItem('tempUserInfo');
    if (storedUserInfo) {
      try {
        const userInfo = JSON.parse(storedUserInfo);
        setLocalTempUserInfo(userInfo);

      } catch (error) {
        console.error('임시 유저 정보 파싱 실패:', error);
      }
    }
  }, []);

  // 표시할 유저 결정 (propTargetUser가 있으면 propTargetUser, localTempUserInfo가 있으면 localTempUserInfo, 없으면 현재 로그인한 user)
  const displayUserId = propTargetUser ? propTargetUser.id : (localTempUserInfo ? localTempUserInfo.id : (targetUserId || user?.id));
  const displayUser = propTargetUser || localTempUserInfo || user;
  const isOwnPage = String(displayUserId) === String(user?.id);


  // 내가 작성한 코멘트 fetch 함수
  const fetchMyComments = () => {
    if (!displayUserId) return;
    setLoading(true);
    fetch(`http://localhost:80/api/users/${displayUserId}/my-comments`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        console.log('내가 작성한 코멘트 데이터:', data);
        setMyComments(data.data || []);
      })
      .catch(() => setMyComments([]))
      .finally(() => setLoading(false));
  };

  // 내가 좋아요한 코멘트 fetch 함수
  const fetchLikedComments = () => {
    if (!displayUserId) return;
    setLikedLoading(true);
    fetch(`http://localhost:80/api/users/${displayUserId}/liked-reviews`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        console.log('내가 좋아요한 코멘트 데이터:', data);
        setLikedComments(data.data || [])
      })
      .catch(() => setLikedComments([]))
      .finally(() => setLikedLoading(false));
  };

  useEffect(() => {
    fetchMyComments();
  }, [displayUserId]);

  useEffect(() => {
    fetchLikedComments();
  }, [displayUserId]);

  // 더보기 버튼 클릭 시 (1차: alert, 2차: 모달 구현 가능)
  const handleMoreMyComments = () => {
    setMyCommentsModalOpen(true);
  };
  const handleMoreLikedComments = () => {
    setLikedModalOpen(true);
  };

  // 코멘트 카드 클릭 핸들러
  const handleCommentClick = (comment) => {
    let mergedComment = {
      ...comment,
      userNickname: comment.authorNickname || user?.nickname || '익명'
    };
    console.log('mergedComment:', mergedComment);
    setSelectedComment(mergedComment);
    setModalOpen(true);
  };

  // 코멘트 수정 핸들러
  const handleEdit = (comment) => {
    setEditTarget(comment);
    setEditModalOpen(true);
  };

  // 코멘트 삭제 핸들러
  const handleDelete = (commentId) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      fetch(`http://localhost:80/api/reviews/${commentId}`, {
        method: 'DELETE',
        credentials: 'include',
      })
        .then(res => res.json())
        .then(data => {
          if (data.success) {
            // 삭제 후 목록 갱신
            setMyComments(prev => prev.filter(c => c.id !== commentId));
          } else {
            alert('삭제 실패: ' + (data.message || ''));
          }
        })
        .catch(() => alert('삭제 중 오류 발생'));
    }
  };

  // MovieDetailBody.jsx의 코멘트 카드 레이아웃 복사
  const renderCommentCard = (comment, showActions = false) => (
    <div
      className={styles.commentCard}
      key={comment.id}
      style={{ cursor: 'pointer' }}
      onClick={() => handleCommentClick(comment)}
    >
      <div className={styles.commentHeader}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <img
            src={comment.authorProfileImageUrl || require('../../assets/user_profile.png')}
            alt="프로필"
            className={styles.profileImage}
            style={{ width: 24, height: 24, borderRadius: '50%' }}
          />
          <span className={styles.commentUser}>{comment.authorNickname || displayUser?.nickname || '익명'}</span>
          <span className={styles.commentDate}>{formatRelativeTime(comment.updatedAt || comment.date)}</span>
        </div>
        <span className={styles.commentRating}>
          ★ {comment.rating ? comment.rating.toFixed(1) : '-'}
        </span>
      </div>
      <hr className={styles.commentDivider} />
      <div className={styles.commentContentWrap}>
        <div className={styles.commentMovieInfo}>
          <span className={styles.commentMovieTitle}>{comment.movieNm}</span>
          <img src={comment.posterUrl} alt="영화 포스터" className={styles.commentMoviePoster} />
        </div>
        <div className={styles.commentContent}>{comment.content}</div>
      </div>
      <hr className={styles.commentFooterDivider} />
      <div className={styles.commentFooter}>
        <span>좋아요 {comment.likeCount ?? '-'}</span>
        <span>댓글 {comment.commentCount ?? '-'}</span>
        {showActions && (
          <div className={styles.commentActions} onClick={e => e.stopPropagation()}>
            <button className={styles.replyEditBtn} onClick={() => handleEdit(comment)}>수정</button>
            <button className={styles.replyDeleteBtn} onClick={() => handleDelete(comment.id)}>삭제</button>
          </div>
        )}
      </div>
    </div>
  );

  return (
    <footer className={styles.footer}>
      <div className={styles.commentSectionHeader}>
        <h2 className={styles.commentSectionTitle}>
          {isOwnPage ? '내가 작성한 코멘트' : `${displayUser?.nickname || '익명'}님이 작성한 코멘트`}
        </h2>
        <span className={styles.commentSectionMore} onClick={handleMoreMyComments}>더보기</span>
      </div>
      <div className={styles.commentGrid}>
        {loading ? <div>로딩 중...</div> : (
          myComments.length === 0 ? <div className={styles.emptyMessage}>아직 코멘트가 없습니다.</div> :
            myComments.slice(0, 8).map(comment => renderCommentCard(comment, isOwnPage))
        )}
      </div>
      <hr className={styles.divider} />
      <div className={styles.commentSectionHeader} style={{ marginTop: '32px' }}>
        <h2 className={styles.commentSectionTitle}>
          {isOwnPage ? '내가 좋아요한 코멘트' : `${displayUser?.nickname || '익명'}님이 좋아요한 코멘트`}
        </h2>
        <span className={styles.commentSectionMore} onClick={handleMoreLikedComments}>더보기</span>
      </div>
      <div className={styles.commentGrid}>
        {likedLoading ? <div>로딩 중...</div> : (
          likedComments.length === 0 ? <div className={styles.emptyMessage}>아직 좋아요한 코멘트가 없습니다.</div> :
            likedComments.slice(0, 8).map(comment => renderCommentCard(comment, false))
        )}
      </div>
      {/* 좋아요한 코멘트 전체보기 모달 */}
      <LikedCommentsModal
        open={likedModalOpen}
        onClose={() => setLikedModalOpen(false)}
        likedComments={likedComments}
        onCommentClick={comment => {
          setSelectedComment(comment);
          setLikedModalOpen(false);
          setLastModalType('liked');
          setModalOpen(true);
        }}
      />
      {/* 내가 작성한 코멘트 전체보기 모달 */}
      <MyCommentsModal
        open={myCommentsModalOpen}
        onClose={() => setMyCommentsModalOpen(false)}
        myComments={myComments}
        title={isOwnPage ? '내가 작성한 코멘트 전체보기' : `${displayUser?.nickname || '익명'}님이 작성한 코멘트 전체보기`}
        onCommentClick={comment => {
          setSelectedComment(comment);
          setMyCommentsModalOpen(false);
          setLastModalType('my');
          setModalOpen(true);
        }}
      />
      {/* 코멘트 상세 모달 */}
      <CommentDetailModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onBack={() => {
          setModalOpen(false);
          if (lastModalType === 'liked') setLikedModalOpen(true);
          else if (lastModalType === 'my') setMyCommentsModalOpen(true);
        }}
        comment={selectedComment}
        reviewId={selectedComment?.id}
        fetchMyComments={fetchMyComments}
        fetchLikedComments={fetchLikedComments}
      />
      {/* 코멘트 수정 모달 */}
      <CommentModal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        movieTitle={editTarget?.movieNm}
        movieCd={editTarget?.movieCd}
        editMode={true}
        initialContent={editTarget?.content || ''}
        initialRating={editTarget?.rating || 0}
        onEditSave={() => {
          setEditModalOpen(false);
          // 수정 후 목록 갱신 (다시 fetch)
          if (user?.id) {
            setLoading(true);
            fetch(`http://localhost:80/api/users/${user.id}/my-comments`, { credentials: 'include' })
              .then(res => res.json())
              .then(data => setMyComments(data.data || []))
              .catch(() => setMyComments([]))
              .finally(() => setLoading(false));
          }
        }}
        reviewId={editTarget?.id}
      />
    </footer>
  );
};

export default MyPageFooter; 