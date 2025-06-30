import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getUserByNickname, getUserPreferredGenres, getGenreTags } from './services/userService';
import axios from 'axios';

const UserPage = () => {
  const { nickname } = useParams();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [preferredGenres, setPreferredGenres] = useState([]);
  const [allGenres, setAllGenres] = useState([]);
  const [editMode, setEditMode] = useState(false);
  const [selectedGenres, setSelectedGenres] = useState([]);

  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getUserByNickname(nickname);
        setUser(data);
        if (data && data.id) {
          const genres = await getUserPreferredGenres(data.id);
          setPreferredGenres(genres);
          setSelectedGenres(genres.map(tag => tag.name));
          const all = await getGenreTags();
          setAllGenres(all);
        }
      } catch (err) {
        setError('유저 정보를 불러올 수 없습니다.');
      }
      setLoading(false);
    };
    fetchUser();
  }, [nickname]);

  const handleGenreChange = (tagName) => {
    setSelectedGenres(prev =>
      prev.includes(tagName)
        ? prev.filter(name => name !== tagName)
        : [...prev, tagName]
    );
  };

  const handleSaveGenres = async () => {
    try {
      await axios.put(`/api/users/${user.id}/preferred-genres`, selectedGenres);
      setPreferredGenres(allGenres.filter(tag => selectedGenres.includes(tag.name)));
      setEditMode(false);
      alert('선호 장르 태그가 저장되었습니다!');
    } catch (e) {
      alert('저장에 실패했습니다.');
    }
  };

  if (loading) return <div>로딩 중...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!user) return <div>유저 정보가 없습니다.</div>;

  return (
    <div style={{ maxWidth: 400, margin: '0 auto', border: '1px solid #eee', borderRadius: 8, padding: 24, marginTop: 32 }}>
      <h2>마이페이지</h2>
      <div><b>닉네임:</b> {user.nickname}</div>
      <div><b>이메일:</b> {user.email}</div>
      <div style={{ marginTop: 16 }}>
        <b>선호 장르 태그:</b>
        {!editMode ? (
          <>
            {preferredGenres.length === 0 ? (
              <span style={{ color: '#aaa', marginLeft: 8 }}>설정된 장르 태그가 없습니다.</span>
            ) : (
              <span style={{ marginLeft: 8 }}>
                {preferredGenres.map(tag => tag.name).join(', ')}
              </span>
            )}
            {user && user.id && (
              <button style={{ marginLeft: 12 }} onClick={() => setEditMode(true)}>수정</button>
            )}
          </>
        ) : (
          <div style={{ marginTop: 8 }}>
            {allGenres.map(tag => (
              <label key={tag.id} style={{ marginRight: 12, display: 'inline-block', marginBottom: 6 }}>
                <input
                  type="checkbox"
                  checked={selectedGenres.includes(tag.name)}
                  onChange={() => handleGenreChange(tag.name)}
                />
                {tag.name}
              </label>
            ))}
            <div style={{ marginTop: 12 }}>
              <button onClick={handleSaveGenres} style={{ marginRight: 8 }}>저장</button>
              <button onClick={() => setEditMode(false)}>취소</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default UserPage; 