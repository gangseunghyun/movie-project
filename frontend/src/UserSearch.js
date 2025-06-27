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
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {results.map(nickname => (
          <li key={nickname} style={{ padding: 8, borderBottom: '1px solid #eee', cursor: 'pointer' }} onClick={() => handleClick(nickname)}>
            {nickname}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default UserSearch; 