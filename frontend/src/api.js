export async function safeFetch(url, options) {
  // url을 그대로 사용 (절대경로로 바꾸지 않음)
  try {
    const res = await fetch(url, { ...options, credentials: 'include' });
    const text = await res.text();
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
    return data;
  } catch (err) {
    return { error: err.message };
  }
}

// 사용자가 특정 영화에 리뷰를 작성했는지 확인
export async function checkUserReview(movieCd) {
  try {
    const response = await safeFetch(`http://localhost/api/reviews/movie/${movieCd}/check-user-review`);
    return response;
  } catch (error) {
    console.error('리뷰 확인 중 오류:', error);
    return { success: false, message: '리뷰 확인 중 오류가 발생했습니다.' };
  }
} 