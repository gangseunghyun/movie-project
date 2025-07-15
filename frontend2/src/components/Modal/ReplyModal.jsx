import React, { useState } from 'react';
import styles from './ReplyModal.module.css';
import { useUser } from '../../contexts/UserContext';

const ReplyModal = ({ open, onClose, reviewId, parentId, isReply = false, onSave, editMode = false, editTarget = null, onUpdate }) => {
  const [comment, setComment] = useState(editMode && editTarget ? editTarget.content : '');
  const [loading, setLoading] = useState(false);
  const maxLength = 1000;
  const { user } = useUser();
  const userId = user?.id;

  React.useEffect(() => {
    if (editMode && editTarget) {
      setComment(editTarget.content || '');
    } else {
      setComment('');
    }
  }, [editMode, editTarget, open]);

  if (!open) return null;

  const handleSave = async () => {
    if (!userId) {
      alert('로그인 후 이용해주세요.');
      return;
    }
    if (!reviewId) {
      alert('리뷰 ID가 없습니다. 새로고침 후 다시 시도해주세요.');
      return;
    }
    setLoading(true);
    try {
      if (editMode && editTarget) {
        // 수정 모드: PUT
        const response = await fetch(`http://localhost:80/api/comments/${editTarget.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId,
            content: comment,
          }),
          credentials: 'include',
        });
        const data = await response.json();
        if (data.success) {
          //alert(data.message);
          if (onUpdate) onUpdate(comment, editTarget.id);
          onClose();
          setComment('');
        } else {
          alert(data.message || '댓글 수정에 실패했습니다.');
        }
      } else {
        // 작성 모드: POST
        const response = await fetch('http://localhost:80/api/comments', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            reviewId,
            content: comment,
            parentId: parentId || null,
            userId, // userId 추가
          }),
          credentials: 'include',
        });
        const data = await response.json();
        if (data.success) {
          //alert(data.message);
          if (onSave) onSave(comment, data.commentId, data.commentType);
          onClose();
          setComment('');
        } else {
          alert(data.message || '댓글 작성에 실패했습니다.');
        }
      }
    } catch (e) {
      alert(editMode ? '댓글 수정 중 오류가 발생했습니다.' : '댓글 작성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setComment('');
    onClose();
  };

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalContainer}>
        <div className={styles.header}>
          <span className={styles.title}>
            {editMode ? '댓글 수정' : (isReply ? '댓글 작성' : '댓글 작성')}
          </span>
          <button className={styles.closeBtn} onClick={handleClose}>×</button>
        </div>
        <hr className={styles.divider} />
        <textarea
          className={styles.textarea}
          placeholder={editMode ? "댓글을 수정해주세요." : (isReply ? "댓글을 작성해주세요." : "댓글을 작성해주세요.")}
          value={comment}
          onChange={e => setComment(e.target.value)}
          maxLength={maxLength}
          disabled={loading}
        />
        <hr className={styles.divider} />
        <div className={styles.footer}>
          <span className={styles.length}>{comment.length}/{maxLength}</span>
          <button
            className={styles.saveBtn}
            onClick={handleSave}
            disabled={comment.length === 0 || loading}
          >
            {loading ? (editMode ? '수정 중...' : '저장 중...') : (editMode ? '수정' : '저장')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ReplyModal; 