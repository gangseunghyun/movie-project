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

// 장르 태그 전체 조회
export const getGenreTags = async () => {
  const res = await axios.get('/api/genre-tags');
  return res.data; // [{id, name} ...]
};

// 특징 태그 전체 조회
export const getFeatureTags = async () => {
  const res = await axios.get('/api/feature-tags');
  return res.data; // [{id, name} ...]
};

// 연도 태그 전체 조회
export const getYearTags = async () => {
  const res = await axios.get('/api/year-tags');
  return res.data; // [{id, name} ...]
};

// 국가 태그 전체 조회
export const getCountryTags = async () => {
  const res = await axios.get('/api/country-tags');
  return res.data; // [{id, name} ...]
};

// 모든 카테고리 태그 조회
export const getAllCategoryTags = async () => {
  const res = await axios.get('/api/all-category-tags');
  return res.data; // [{id, name} ...]
};

// 사용자 선호 장르 태그 조회
export const getUserPreferredGenres = async (userId) => {
  const res = await axios.get(`/api/users/${userId}/preferred-genres`);
  return res.data; // [{id, name} ...]
};

// 사용자 선호 태그 조회 (모든 카테고리)
export const getUserPreferredTags = async (userId) => {
  const res = await axios.get(`/api/users/${userId}/preferred-tags`);
  return res.data; // [{id, name} ...]
}; 