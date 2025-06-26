export async function safeFetch(url, options) {
  // url이 /로 시작하면 80 포트로 자동 변환
  const fullUrl = url.startsWith('/') ? `http://localhost:80${url}` : url;
  try {
    const res = await fetch(fullUrl, { ...options, credentials: 'include' });
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