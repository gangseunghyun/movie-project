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