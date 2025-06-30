import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  getUserByNickname, 
  getUserPreferredTags, 
  getGenreTags, 
  getFeatureTags
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
  const [allTags, setAllTags] = useState({
    genres: [],
    features: []
  });
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
          const [tags, genres, features] = await Promise.all([
            getUserPreferredTags(data.id),
            getGenreTags(),
            getFeatureTags()
          ]);
          setPreferredTags(tags);
          setSelectedTags(tags.map(tag => tag.name));
          setAllTags({
            genres,
            features
          });
        }
      } catch (err) {
        setError('유저 정보를 불러올 수 없습니다.');
      }
      setLoading(false);
    };
    fetchUser();
  }, [nickname]);

  const handleTagChange = (tagName) => {
    setSelectedTags(prev =>
      prev.includes(tagName)
        ? prev.filter(name => name !== tagName)
        : [...prev, tagName]
    );
  };

  const handleSaveTags = async () => {
    setSaving(true);
    try {
      await axios.put(`/api/users/${user.id}/preferred-tags`, selectedTags);
      setPreferredTags(Object.values(allTags).flat().filter(tag => selectedTags.includes(tag.name)));
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

  // 중복 제거된 태그 리스트 만들기
  const mergedTags = React.useMemo(() => {
    const all = [...allTags.genres, ...allTags.features];
    const unique = [];
    const seen = new Set();
    for (const tag of all) {
      if (!seen.has(tag.name)) {
        unique.push(tag);
        seen.add(tag.name);
      }
    }
    return unique;
  }, [allTags]);

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
                  <p>수정 버튼을 눌러서 선호하는 태그를 설정해보세요!</p>
                </div>
              ) : (
                <div className="tag-categories">
                  {['genres', 'features'].map(category => {
                    const categoryTags = preferredTags.filter(tag => 
                      allTags[category].some(catTag => catTag.name === tag.name)
                    );
                    if (categoryTags.length === 0) return null;
                    
                    return (
                      <div key={category} className="tag-category">
                        <h4>{category === 'genres' ? '장르' : '특징'}</h4>
                        <div className="tag-tags">
                          {categoryTags.map(tag => (
                            <span key={tag.id} className="tag-tag">
                              {tag.name}
                            </span>
                          ))}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          ) : (
            <div className="tags-edit">
              <div className="tag-selection">
                {mergedTags.map(tag => (
                  <label key={tag.id + '-' + tag.name} className="tag-checkbox">
                    <input
                      type="checkbox"
                      checked={selectedTags.includes(tag.name)}
                      onChange={() => handleTagChange(tag.name)}
                    />
                    <span className="checkbox-label">{tag.name}</span>
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