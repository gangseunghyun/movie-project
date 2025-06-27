export async function safeFetch(url, options) {
  // url이 /로 시작하면 절대 경로로 변환
  const fullUrl = url.startsWith('/') ? `http://localhost:80${url}` : url;
  console.log("=== safeFetch 호출 ===");
  console.log("원본 URL:", url);
  console.log("변환된 URL:", fullUrl);
  console.log("요청 옵션:", options);
  
  try {
    const res = await fetch(fullUrl, { ...options, credentials: 'include' });
    console.log("응답 상태:", res.status);
    console.log("응답 헤더:", res.headers);
    
    const text = await res.text();
    console.log("응답 텍스트 길이:", text.length);
    console.log("응답 텍스트 처음 200자:", text.substring(0, 200));
    
    let data;
    try {
      data = JSON.parse(text);
      console.log("JSON 파싱 성공:", data);
    } catch {
      console.log("JSON 파싱 실패, 텍스트 그대로 반환");
      data = text;
    }
    return data;
  } catch (err) {
    console.error("safeFetch 에러:", err);
    return { error: err.message };
  }
} 