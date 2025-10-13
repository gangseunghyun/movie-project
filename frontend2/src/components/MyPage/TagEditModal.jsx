import React, { useState, useEffect } from "react";
import styles from "./TagEditModal.module.css";
import { useUser } from "../../contexts/UserContext";

const TagEditModal = ({ initialSelected = [], onClose, onSave }) => {
  const { user } = useUser();
  const [selected, setSelected] = useState(initialSelected);
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchTags = async () => {
      try {
        const response = await fetch(`/api/genre-tags`, {
          credentials: 'include',
        });
        if (response.ok) {
          const tagNames = await response.json();
          console.log(tagNames);
          setTags(tagNames);
        } else {
          console.error('태그 정보를 가져오는데 실패했습니다.');
        }
      } catch (error) {
        console.error('태그 정보를 가져오는 중 오류가 발생했습니다:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchTags();
  }, []);

  // 현재 로그인한 사용자의 선호 태그 가져오기
  useEffect(() => {
    const fetchUserPreferredGenres = async () => {
      if (!user || !user.id) {
        console.log('사용자 정보가 없습니다.');
        return;
      }

      try {
        const response = await fetch(`/api/users/${user.id}/preferred-genres`, {
          credentials: 'include',
        });
        if (response.ok) {
          const preferredGenres = await response.json();
          console.log('사용자 선호 태그:', preferredGenres);
          setSelected(preferredGenres);
        } else {
          console.error('사용자 선호 태그를 가져오는데 실패했습니다.');
        }
      } catch (error) {
        console.error('사용자 선호 태그를 가져오는 중 오류가 발생했습니다:', error);
      }
    };

    fetchUserPreferredGenres();
  }, [user]);

  const handleTagClick = (tag) => {
    if (selected.includes(tag)) {
      setSelected(selected.filter((t) => t !== tag));
    } else if (selected.length < 4) {
      setSelected([...selected, tag]);
    }
  };

  const handleSave = async () => {
    if (!user || !user.id) {
      console.error('사용자 정보가 없습니다.');
      return;
    }

    try {
      const response = await fetch(`/api/users/${user.id}/preferred-genres`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(selected)
      });

      if (response.ok) {
        console.log('선호 태그가 성공적으로 저장되었습니다.');
        onSave(selected);
      } else {
        console.error('선호 태그 저장에 실패했습니다.');
        alert('선호 태그 저장에 실패했습니다. 다시 시도해주세요.');
      }
    } catch (error) {
      console.error('선호 태그 저장 중 오류가 발생했습니다:', error);
      alert('서버 오류가 발생했습니다. 다시 시도해주세요.');
    }
  };

  if (loading) {
    return (
      <div className={styles.modalOverlay}>
        <div className={styles.modalBox}>
          <div className={styles.modalHeader}>
            <span className={styles.title}>선호 태그 선택 <span className={styles.limit}>(최대 4개)</span></span>
            <button className={styles.closeBtn} onClick={onClose} aria-label="닫기">×</button>
          </div>
          <div className={styles.loadingMessage}>태그 정보를 불러오는 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalBox}>
        <div className={styles.modalHeader}>
          <span className={styles.title}>선호 태그 선택 <span className={styles.limit}>(최대 4개)</span></span>
          <button className={styles.closeBtn} onClick={onClose} aria-label="닫기">×</button>
        </div>
        <div className={styles.tagsGrid}>
          {tags.map((tag, idx) => (
            <button
              key={tag + idx}
              className={selected.includes(tag) ? styles.tagSelected : styles.tag}
              onClick={() => handleTagClick(tag)}
              type="button"
            >
              #{tag}
            </button>
          ))}
        </div>
        <div className={styles.buttonRow}>
          <button className={styles.cancelBtn} onClick={onClose}>취소</button>
          <button className={styles.saveBtn} onClick={handleSave}>저장</button>
        </div>
      </div>
    </div>
  );
};

export default TagEditModal; 