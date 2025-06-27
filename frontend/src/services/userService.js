import axios from 'axios';

// 닉네임 포함 검색
export const userSearch = async (nickname) => {
  const res = await axios.get(`/api/users/search?nickname=${encodeURIComponent(nickname)}`);
  return res.data; // 닉네임 리스트
};

// 닉네임으로 유저 단일 조회
export const getUserByNickname = async (nickname) => {
  const res = await axios.get(`/api/users/nickname/${encodeURIComponent(nickname)}`);
  return res.data; // { nickname, email }
}; 