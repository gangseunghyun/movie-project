import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  getUserByNickname, 
  getUserPreferredTags, 
  getGenreTags
} from './services/userService';
import axios from 'axios';
import './UserPage.css';

const UserPage = () => {
  const { nickname } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [preferredTags, setPreferredTags] = useState([]);
  const [genreTags, setGenreTags] = useState([]);
  const [editMode, setEditMode] = useState(false);
  const [selectedTags, setSelectedTags] = useState([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getUserByNickname(nickname);
        setUser(data);
        if (data && data.id) {
          const [tags, genres] = await Promise.all([
            getUserPreferredTags(data.id),
            getGenreTags()
          ]);
          setGenreTags(genres);
          // preferredTags, selectedTags는 genreTags에 존재하는 값만
          const validTags = tags.filter(tag => genres.includes(tag));
          setPreferredTags(validTags);
          setSelectedTags(validTags);
        }
      } catch (err) {
        setError('유저 정보를 불러올 수 없습니다.');
      }
      setLoading(false);
    };
    fetchUser();
  }, [nickname]);

  const handleTagChange = (tag) => {
    setSelectedTags(prev => {
      if (prev.includes(tag)) {
        return prev.filter(name => name !== tag);
      } else {
        if (prev.length >= 4) {
          alert('최대 4개까지만 선택할 수 있습니다.');
          return prev;
        }
        return [...prev, tag];
      }
    });
  };

  const handleSaveTags = async () => {
    setSaving(true);
    try {
      // genreTags에 존재하는 selectedTags만 저장
      const validTags = selectedTags.filter(tag => genreTags.includes(tag));
      await axios.put(`/api/users/${user.id}/preferred-tags`, validTags);
      setPreferredTags(validTags);
      setEditMode(false);
      alert('선호 태그가 저장되었습니다!');
    } catch (e) {
      alert('저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setSelectedTags(preferredTags);
    setEditMode(false);
  };

  const handleGoHome = () => {
    navigate('/');
  };

  if (loading) return (
    <div className="user-page-container">
      <div className="loading-spinner">로딩 중...</div>
    </div>
  );
  
  if (error) return (
    <div className="user-page-container">
      <div className="error-message">{error}</div>
    </div>
  );
  
  if (!user) return (
    <div className="user-page-container">
      <div className="error-message">유저 정보가 없습니다.</div>
    </div>
  );

  return (
    <div className="user-page-container">
      <div className="user-profile-card">
        <div className="profile-header" style={{ justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
            <div className="profile-avatar">
              <span>{user.nickname.charAt(0).toUpperCase()}</span>
            </div>
            <h2 className="profile-title">마이페이지</h2>
          </div>
          <button className="home-button" onClick={handleGoHome}>홈으로</button>
        </div>
        
        <div className="profile-info">
          <div className="info-item">
            <label>닉네임</label>
            <span>{user.nickname}</span>
          </div>
          <div className="info-item">
            <label>이메일</label>
            <span>{user.email}</span>
          </div>
        </div>

        <div className="preferred-tags-section">
          <div className="section-header">
            <h3>선호 태그</h3>
            {!editMode && user && user.id && (
              <button 
                className="edit-button"
                onClick={() => setEditMode(true)}
              >
                수정
              </button>
            )}
          </div>
          
          {!editMode ? (
            <div className="tags-display">
              {preferredTags.length === 0 ? (
                <div className="no-tags">
                  <span>설정된 선호 태그가 없습니다.</span>
                  <p>수정 버튼을 눌러서 선호하는 장르 태그를 설정해보세요!</p>
                </div>
              ) : (
                <div className="tag-categories">
                  <div className="tag-category">
                    <h4>장르</h4>
                    <div className="tag-tags">
                      {preferredTags.map(tag => (
                        <span key={tag} className="tag-tag">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="tags-edit">
              <div style={{ marginBottom: 10, color: '#a18cd1', fontWeight: 'bold', fontSize: '15px' }}>
                선택: {selectedTags.filter(tag => genreTags.includes(tag)).length}/4
              </div>
              <div className="tag-selection">
                {genreTags.map(tag => (
                  <label key={tag} className="tag-checkbox">
                    <input
                      type="checkbox"
                      checked={selectedTags.includes(tag)}
                      onChange={() => handleTagChange(tag)}
                    />
                    <span className="checkbox-label">{tag}</span>
                  </label>
                ))}
              </div>
              <div className="edit-actions">
                <button 
                  className="save-button"
                  onClick={handleSaveTags}
                  disabled={saving}
                >
                  {saving ? '저장 중...' : '저장'}
                </button>
                <button 
                  className="cancel-button"
                  onClick={handleCancel}
                  disabled={saving}
                >
                  취소
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserPage; 