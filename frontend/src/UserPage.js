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
          
          // 기존 태그 중 장르 태그만 필터링
          const genreTagNames = genres.map(tag => tag.name);
          const filteredTags = tags.filter(tag => genreTagNames.includes(tag.name));
          
          // 필터링된 태그가 원본과 다르면 특징 태그가 있었던 것이므로 제거
          if (filteredTags.length !== tags.length) {
            try {
              await axios.delete(`/api/users/${data.id}/feature-tags`);
              setPreferredTags(filteredTags);
              setSelectedTags(filteredTags.map(tag => tag.name));
            } catch (err) {
              console.error('특징 태그 제거 실패:', err);
              setPreferredTags(filteredTags);
              setSelectedTags(filteredTags.map(tag => tag.name));
            }
          } else {
            setPreferredTags(tags);
            setSelectedTags(tags.map(tag => tag.name));
          }
          
          setGenreTags(genres);
        }
      } catch (err) {
        setError('유저 정보를 불러올 수 없습니다.');
      }
      setLoading(false);
    };
    fetchUser();
  }, [nickname]);

  const handleTagChange = (tagName) => {
    setSelectedTags(prev => {
      if (prev.includes(tagName)) {
        // 이미 선택된 태그는 해제 가능
        return prev.filter(name => name !== tagName);
      } else {
        // 5개 이상 선택 불가
        if (prev.length >= 5) {
          alert('최대 5개까지만 선택할 수 있습니다.');
          return prev;
        }
        return [...prev, tagName];
      }
    });
  };

  const handleSaveTags = async () => {
    setSaving(true);
    try {
      await axios.put(`/api/users/${user.id}/preferred-tags`, selectedTags);
      setPreferredTags(genreTags.filter(tag => selectedTags.includes(tag.name)));
      setEditMode(false);
      alert('선호 태그가 저장되었습니다!');
    } catch (e) {
      alert('저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setSelectedTags(preferredTags.map(tag => tag.name));
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
                        <span key={tag.id} className="tag-tag">
                          {tag.name}
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
                선택: {selectedTags.length}/5
              </div>
              <div className="tag-selection">
                {genreTags.map(tag => (
                  <label key={typeof tag === 'string' ? tag : tag.id} className="tag-checkbox">
                    <input
                      type="checkbox"
                      checked={selectedTags.includes(typeof tag === 'string' ? tag : tag.name)}
                      onChange={() => handleTagChange(typeof tag === 'string' ? tag : tag.name)}
                    />
                    <span className="checkbox-label">{typeof tag === 'string' ? tag : tag.name}</span>
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