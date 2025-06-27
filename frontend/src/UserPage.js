import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getUserByNickname } from './services/userService';

const UserPage = () => {
  const { nickname } = useParams();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getUserByNickname(nickname);
        setUser(data);
      } catch (err) {
        setError('유저 정보를 불러올 수 없습니다.');
      }
      setLoading(false);
    };
    fetchUser();
  }, [nickname]);

  if (loading) return <div>로딩 중...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;
  if (!user) return <div>유저 정보가 없습니다.</div>;

  return (
    <div style={{ maxWidth: 400, margin: '0 auto', border: '1px solid #eee', borderRadius: 8, padding: 24, marginTop: 32 }}>
      <h2>마이페이지</h2>
      <div><b>닉네임:</b> {user.nickname}</div>
      <div><b>이메일:</b> {user.email}</div>
    </div>
  );
};

export default UserPage; 