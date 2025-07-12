import React, { useState } from 'react';
import { userSearch } from './services/userService';
import { useNavigate } from 'react-router-dom';

const UserSearch = () => {
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const handleSearch = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await userSearch(keyword);
      setResults(data);
    } catch (err) {
      setError('검색 중 오류가 발생했습니다.');
    }
    setLoading(false);
  };

  const handleClick = (nickname) => {
    navigate(`/user/${nickname}`);
  };

  return (
    <div style={{ maxWidth: 400, margin: '0 auto' }}>
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: 8 }}>
        <input
          type="text"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          placeholder="닉네임 검색"
          style={{ flex: 1 }}
        />
        <button type="submit">검색</button>
      </form>
      {loading && <div>검색 중...</div>}
      {error && <div style={{ color: 'red' }}>{error}</div>}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {results.map(user => (
          <div 
            key={user.id} 
            style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: 12, 
              padding: 12, 
              border: '1px solid #eee', 
              borderRadius: 8, 
              cursor: 'pointer',
              background: 'white',
              transition: 'transform 0.2s, box-shadow 0.2s'
            }} 
            onMouseEnter={e => {
              e.currentTarget.style.transform = 'translateY(-2px)';
              e.currentTarget.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = 'none';
            }}
            onClick={() => handleClick(user.nickname)}
          >
            {/* 프로필 이미지 */}
            <div style={{ 
              width: 48, 
              height: 48, 
              borderRadius: '50%', 
              overflow: 'hidden', 
              background: '#f0f0f0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}>
              {user.profileImageUrl ? (
                <img 
                  src={user.profileImageUrl} 
                  alt={user.nickname} 
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={e => {
                    e.target.style.display = 'none';
                    e.target.nextSibling.style.display = 'flex';
                  }}
                />
              ) : null}
              <div style={{ 
                display: user.profileImageUrl ? 'none' : 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%',
                height: '100%',
                fontSize: '18px',
                fontWeight: 'bold',
                color: '#6c63ff'
              }}>
                {user.nickname?.charAt(0)?.toUpperCase()}
              </div>
            </div>
            
            {/* 유저 정보 */}
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 'bold', fontSize: '16px', marginBottom: '4px' }}>
                {user.nickname}
              </div>
              <div style={{ display: 'flex', gap: '16px', fontSize: '14px', color: '#666' }}>
                <span>팔로잉 {user.followingCount}</span>
                <span>팔로워 {user.followersCount}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default UserSearch; 