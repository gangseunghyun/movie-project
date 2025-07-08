import React, { useState, useRef, useEffect } from 'react';

const ChatbotModal = ({ isOpen, onClose }) => {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setMessages([
        { 
          type: 'bot', 
          content: '안녕하세요! MCP 기반 AI 영화 챗봇입니다! 🤖\n\n자연어로 영화에 대해 무엇이든 물어보세요!\n\n예시:\n• "오늘 기분이 우울한데 영화 추천해줘"\n• "로맨스 영화 중에서 가장 재밌는 거 뭐야?"\n• "인터스텔라 영화에 대해 자세히 알려줘"\n\n💡 MCP 도구를 사용하여 정확한 영화 정보를 제공합니다!',
          isSystem: true
        }
      ]);
      setInput('');
    }
  }, [isOpen]);

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  // TMDB API KEY (백엔드 application.properties에서 복사)
  const TMDB_API_KEY = '1bf75ae343366c11df19efc31c31e899';
  const TMDB_BASE_URL = 'https://api.themoviedb.org/3';
  const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p/w500';

  // 영화명으로 TMDB 포스터 URL 가져오기
  async function fetchPosterUrl(movieName) {
    try {
      const url = `${TMDB_BASE_URL}/search/movie?api_key=${TMDB_API_KEY}&query=${encodeURIComponent(movieName)}`;
      const res = await fetch(url);
      const data = await res.json();
      if (data.results && data.results.length > 0 && data.results[0].poster_path) {
        return TMDB_IMAGE_BASE + data.results[0].poster_path;
      }
    } catch (e) {}
    return null;
  }

  // 영화 정보 조회 API 호출
  const fetchMovieInfo = async (movieName) => {
    try {
      console.log('영화 정보 조회 시도:', movieName);
      const response = await fetch('http://localhost:80/api/mcp/tools/getMovieInfo', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ parameters: { movieCd: movieName } })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      console.log('영화 정보 응답:', data);
      
      if (data.success && data.result && data.result.movie) {
        return data.result.movie;
      } else {
        throw new Error(data.error || '해당 영화 정보를 찾을 수 없습니다.');
      }
    } catch (err) {
      console.error('영화 정보 조회 실패:', err);
      return { error: err.message || '영화 정보 조회 중 오류가 발생했습니다.' };
    }
  };

  // 영화명 추출 로직 개선
  const extractMovieName = (msg) => {
    console.log('=== 영화명 추출 시작 ===');
    console.log('입력 메시지:', msg);
    
    // 제외할 단어들 (감정, 상황 관련)
    const excludeWords = [
      '우울', '슬픔', '힐링', '기분', '스트레스', '위로', '추천', '보고싶', '재밌는', '좋은',
      '영화', '무슨', '어떤', '알려줘', '정보', '뭐야', '보고싶어', '어떤가', '로맨스', '액션',
      '코미디', '공포', '호러', '스릴러', '사랑', '러브', '어드벤처', '웃음', '재미'
    ];
    
    // 명확한 영화명 패턴들
    const moviePatterns = [
      /([가-힣A-Za-z0-9\s]+)(?:은|는|이|가)?\s*(?:무슨|어떤)?\s*영화/, // 라라랜드는 무슨 영화
      /([가-힣A-Za-z0-9\s]+)\s*에 대해/, // 라라랜드에 대해
      /([가-힣A-Za-z0-9\s]+)\s*알려줘/, // 라라랜드 알려줘
      /([가-힣A-Za-z0-9\s]+)\s*정보/, // 라라랜드 정보
      /([가-힣A-Za-z0-9\s]+)\s*이 뭐야/, // 라라랜드가 뭐야
      /([가-힣A-Za-z0-9\s]+)\s*에 대해 설명해줘/, // 라라랜드에 대해 설명해줘
      /([가-힣A-Za-z0-9\s]+)\s*movie/i, // 라라랜드 movie
      /([가-힣A-Za-z0-9\s]+)\s*영화/, // 라라랜드 영화
      /([가-힣A-Za-z0-9\s]+)\s*보고싶어/, // 라라랜드 보고싶어
      /([가-힣A-Za-z0-9\s]+)\s*어떤가/, // 라라랜드 어떤가
    ];

    for (let i = 0; i < moviePatterns.length; i++) {
      const pattern = moviePatterns[i];
      const match = msg.match(pattern);
      console.log(`패턴 ${i + 1} 테스트:`, pattern.source);
      console.log('매치 결과:', match);
      
      if (match && match[1]) {
        const extracted = match[1].trim();
        console.log('추출된 텍스트:', extracted);
        
        // 제외 조건들
        if (extracted.length < 3) {
          console.log('길이 부족으로 제외 (3글자 미만)');
          continue; // 최소 3글자
        }
        
        // 제외 단어 목록에 포함된 경우 제외
        const isExcluded = excludeWords.some(word => 
          extracted.toLowerCase().includes(word.toLowerCase())
        );
        if (isExcluded) {
          console.log('제외 단어 포함으로 제외:', excludeWords.find(word => 
            extracted.toLowerCase().includes(word.toLowerCase())
          ));
          continue;
        }
        
        // 숫자만 있는 경우 제외
        if (/^\d+$/.test(extracted)) {
          console.log('숫자만 있어서 제외');
          continue;
        }
        
        console.log('영화명으로 인정됨:', extracted);
        return extracted;
      }
    }
    console.log('영화명 추출 실패');
    return null;
  };

  // 감정 분류 맵
  const emotionMap = [
    { type: 'HEALING', keywords: ['우울', '슬픔', '위로', '힐링', '기분이 안 좋', '기분이 나쁘', '기분이 다운', '기분이 침체', '지침', '피곤', '무기력'] },
    { type: 'HAPPY', keywords: ['기분이 좋', '행복', '설렘', '신남', '기쁨', '즐거움', '좋은 하루', '기분 최고'] },
    { type: 'ANGER', keywords: ['분노', '화남', '짜증', '스트레스', '열받', '빡침', '화가 나', '짜증나'] },
  ];

  const classifyEmotion = (input) => {
    const lower = input.toLowerCase();
    for (const emotion of emotionMap) {
      for (const kw of emotion.keywords) {
        if (lower.includes(kw)) return emotion.type;
      }
    }
    return null;
  };

  // 질문 유형 분류
  const classifyQuery = (input) => {
    const emotionType = classifyEmotion(input);
    if (emotionType === 'HEALING') return { type: 'EMOTIONAL_RECOMMENDATION', emotion: 'healing' };
    if (emotionType === 'HAPPY') return { type: 'HAPPY_RECOMMENDATION', emotion: 'happy' };
    if (emotionType === 'ANGER') return { type: 'ANGER_RECOMMENDATION', emotion: 'anger' };

    // ...장르, 영화명 추출 등 기존 로직 이어서...
    const lowerInput = input.toLowerCase();
    if (lowerInput.includes('로맨스') || lowerInput.includes('사랑') || lowerInput.includes('러브')) {
      return { type: 'GENRE_RECOMMENDATION', genre: 'romance' };
    }
    if (lowerInput.includes('액션') || lowerInput.includes('스릴') || lowerInput.includes('어드벤처')) {
      return { type: 'GENRE_RECOMMENDATION', genre: 'action' };
    }
    if (lowerInput.includes('코미디') || lowerInput.includes('웃음') || lowerInput.includes('재미')) {
      return { type: 'GENRE_RECOMMENDATION', genre: 'comedy' };
    }
    if (lowerInput.includes('공포') || lowerInput.includes('호러') || lowerInput.includes('스릴러')) {
      return { type: 'GENRE_RECOMMENDATION', genre: 'horror' };
    }
    if (lowerInput.includes('추천') || lowerInput.includes('보고싶') || lowerInput.includes('재밌는') || lowerInput.includes('좋은') || lowerInput.includes('인기') || lowerInput.includes('베스트')) {
      return { type: 'GENERAL_RECOMMENDATION' };
    }
    const movieName = extractMovieName(input);
    if (movieName && movieName.length >= 3) {
      return { type: 'MOVIE_INFO', movieName };
    }
    return { type: 'GENERAL_RECOMMENDATION' };
  };

  // 메시지 전송 처리
  const sendMessage = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage = { type: 'user', content: input };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      const queryType = classifyQuery(input);
      console.log('질문 유형:', queryType);

      if (queryType.type === 'MOVIE_INFO') {
        // 영화 정보 조회
        const movieInfo = await fetchMovieInfo(queryType.movieName);
        if (movieInfo.error) {
          setMessages(prev => [...prev, { 
            type: 'bot', 
            content: movieInfo.error, 
            isError: true 
          }]);
        } else {
          setMessages(prev => [...prev, {
            type: 'bot',
            content: `"${movieInfo.movieNm}" 영화 정보입니다!\n\n${movieInfo.description || '상세 설명이 없습니다.'}`,
            movies: [movieInfo],
            messageType: 'MOVIE_INFO'
          }]);
        }
      } else {
        // 추천 응답 생성
        const aiResponse = await generateRecommendationResponse(queryType, input);
        setMessages(prev => [...prev, {
          type: 'bot',
          content: aiResponse.message,
          movies: aiResponse.movies || [],
          endedMovies: aiResponse.endedMovies || [],
          messageType: aiResponse.type
        }]);
      }
    } catch (error) {
      console.error('메시지 처리 오류:', error);
      setMessages(prev => [...prev, {
        type: 'bot',
        content: '죄송합니다. AI 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.',
        movies: [],
        endedMovies: [],
        isError: true
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  // 추천 응답 생성
  const generateRecommendationResponse = async (queryType, originalInput) => {
    try {
      // 백엔드 API 호출을 위한 파라미터 설정
      let apiParameters = {};
      
      switch (queryType.type) {
        case 'EMOTIONAL_RECOMMENDATION':
          apiParameters = { situation: "우울할 때" };
          break;
        case 'HAPPY_RECOMMENDATION':
          apiParameters = { situation: "기분이 좋을 때" };
          break;
        case 'ANGER_RECOMMENDATION':
          apiParameters = { situation: "스트레스 받을 때" };
          break;
        case 'GENRE_RECOMMENDATION':
          if (queryType.genre === 'romance') {
            apiParameters = { genre: "로맨스" };
          } else if (queryType.genre === 'action') {
            apiParameters = { genre: "액션" };
          }
          break;
        case 'GENERAL_RECOMMENDATION':
        default:
          // 일반 추천은 인기 영화로 처리
          apiParameters = {};
          break;
      }

      // 백엔드 API 호출
      const response = await fetch('http://localhost:80/api/mcp/tools/searchMovies', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ parameters: apiParameters })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      console.log('영화 추천 API 응답:', data);

      if (data.success && data.result && data.result.movies) {
        let movies = data.result.movies || [];
        // status 값 실제로 어떻게 오는지 콘솔 출력
        console.log('추천 영화 리스트:', movies.map(m => ({ title: m.movieNm, status: m.status })));
        // status가 없으면 모든 영화를 현재 상영중으로 처리
        const nowOrSoon = movies.filter(m => {
          if (!m.status) return true; // status가 없으면 모든 영화 포함
          const s = (m.status || '').toUpperCase();
          return s === 'NOW_PLAYING' || s === 'COMING_SOON' ||
                 s === '개봉중' || s === '개봉예정' ||
                 s === 'NOWPLAYING' || s === 'COMINGSOON';
        });
        const ended = movies.filter(m => {
          if (!m.status) return false; // status가 없으면 상영종료로 분류하지 않음
          const s = (m.status || '').toUpperCase();
          return s === 'ENDED' || s === '상영종료' || s === 'ENDED';
        });
        let message = '';
        switch (queryType.type) {
          case 'EMOTIONAL_RECOMMENDATION':
            message = `아, 기분이 우울하시군요! 😔 그런 날에는 마음을 따뜻하게 해주는 영화가 최고예요.\n\n제가 특별히 힐링 영화들을 찾아봤어요! 🎬\n\n이 영화들은 우울한 마음을 밝게 해줄 거예요. 오늘 밤에 이 영화들 중 하나를 보시면서 따뜻한 시간 보내세요! ☺️`;
            break;
          case 'HAPPY_RECOMMENDATION':
            message = `기분이 정말 좋으시군요! 😄 이런 날엔 신나고 유쾌한 영화가 제격이죠!\n\n제가 기분 좋은 영화들을 추천해드릴게요! 🎬`;
            break;
          case 'ANGER_RECOMMENDATION':
            message = `스트레스나 분노가 쌓일 땐 시원한 액션이나 빵 터지는 코미디가 최고죠! 💥\n\n제가 기분 전환에 도움이 될 영화들을 찾아봤어요! 🎬`;
            break;
          case 'GENRE_RECOMMENDATION':
            if (queryType.genre === 'romance') {
              message = `로맨스 영화를 찾고 계시는군요! 💕\n\n제가 가장 인기 있는 로맨스 영화들을 추천해드릴게요! 🎬`;
            } else if (queryType.genre === 'action') {
              message = `액션 영화를 원하시는군요! 💥\n\n제가 가장 화끈한 액션 영화들을 추천해드릴게요! 🔥`;
            }
            break;
          case 'GENERAL_RECOMMENDATION':
          default:
            message = `흥미로운 질문이네요! 🤔\n\n제가 인기 있는 영화들을 추천해드릴게요! 🎬`;
            break;
        }
        // 안내 메시지 및 방어적 처리
        if (nowOrSoon.length > 0) {
          return {
            message: message,
            movies: nowOrSoon,
            endedMovies: ended,
            type: "RECOMMENDATION"
          };
        } else if (ended.length > 0) {
          message += `\n\n⚠️ 현재 상영중이거나 개봉예정인 영화가 없어 상영종료 영화를 안내합니다.`;
          return {
            message: message,
            movies: ended,
            endedMovies: [],
            type: "RECOMMENDATION"
          };
        } else if (movies.length > 0) {
          message += `\n\n⚠️ 추천할 영화가 없습니다. 하지만 아래 영화들을 참고해보세요.`;
          return {
            message: message,
            movies: movies,
            endedMovies: [],
            type: "RECOMMENDATION"
          };
        } else {
          message += `\n\n⚠️ 추천할 영화가 없습니다.`;
          return {
            message: message,
            movies: [],
            endedMovies: [],
            type: "RECOMMENDATION"
          };
        }
      } else {
        throw new Error(data.error || '영화 추천을 가져오는데 실패했습니다.');
      }
    } catch (error) {
      console.error('영화 추천 API 호출 실패:', error);
      
      // 에러 발생 시 기본 메시지 반환
      return {
        message: `죄송합니다. 영화 추천을 가져오는 중에 문제가 발생했습니다. 😅\n\n잠시 후 다시 시도해주세요!`,
        movies: [],
        endedMovies: [],
        type: "ERROR"
      };
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // 영화 카드 렌더링 (포스터 포함)
  const renderMovieCard = (movie) => (
    <div key={movie.movieCd} className="chatbot-movie-card" style={{
      background: 'white',
      borderRadius: '8px',
      padding: '12px',
      margin: '8px 0',
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      cursor: 'pointer',
      transition: 'transform 0.2s',
      border: '1px solid #e0e0e0'
    }} onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-2px)'}
       onMouseLeave={e => e.currentTarget.style.transform = 'translateY(0)'}>
      <div style={{ display: 'flex', gap: '12px' }}>
        <div style={{ 
          width: '60px', 
          height: '80px', 
          borderRadius: '4px', 
          overflow: 'hidden',
          flexShrink: 0
        }}>
          {movie.posterUrl ? (
            <img 
              src={movie.posterUrl} 
              alt={movie.movieNm}
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'cover'
              }}
            />
          ) : (
            <div style={{ 
              width: '100%', 
              height: '100%', 
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              fontSize: '20px',
              color: 'white'
            }}>
              🎬
            </div>
          )}
        </div>
        <div style={{ flex: 1 }}>
          <h4 style={{ margin: '0 0 4px 0', fontSize: '14px', fontWeight: '600', color: '#333' }}>{movie.movieNm}</h4>
          <p style={{ margin: '0 0 2px 0', fontSize: '12px', color: '#666' }}>{movie.genreNm}</p>
          {movie.averageRating && movie.averageRating > 0 && (
            <p style={{ margin: '0', fontSize: '12px', color: '#f39c12', fontWeight: '600' }}>
              ⭐ {movie.averageRating.toFixed(1)}
            </p>
          )}
          {movie.openDt && (
            <p style={{ margin: '2px 0 0 0', fontSize: '11px', color: '#999' }}>
              📅 {movie.openDt}
            </p>
          )}
        </div>
      </div>
    </div>
  );

  if (!isOpen) return null;

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
      background: 'rgba(0,0,0,0.3)', zIndex: 10000, display: 'flex', justifyContent: 'center', alignItems: 'center'
    }} onClick={onClose}>
      <div style={{
        width: 450, maxWidth: '95vw', height: 650, background: 'white', borderRadius: 16,
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)', display: 'flex', flexDirection: 'column',
        position: 'relative', overflow: 'hidden'
      }} onClick={e => e.stopPropagation()}>
        <div style={{ 
          padding: '18px 20px', 
          borderBottom: '1px solid #eee', 
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 
          color: 'white', 
          fontWeight: 700, 
          fontSize: 18 
        }}>
          🤖 MCP AI 영화 챗봇
          <div style={{ fontSize: 12, fontWeight: 400, marginTop: 4, opacity: 0.9 }}>
            Model Context Protocol 기반
          </div>
          <button onClick={onClose} style={{ position: 'absolute', right: 18, top: 14, background: 'none', border: 'none', color: 'white', fontSize: 22, cursor: 'pointer' }}>✕</button>
        </div>
        <div style={{ flex: 1, padding: 16, overflowY: 'auto', background: '#f7f8fa' }}>
          {messages.map((msg, i) => (
            <div key={i} style={{
              display: 'flex', flexDirection: msg.type === 'user' ? 'row-reverse' : 'row',
              marginBottom: 16
            }}>
              <div style={{
                background: msg.type === 'user' ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' : 
                          msg.isSystem ? '#e3f2fd' : '#fff',
                color: msg.type === 'user' ? 'white' : 
                       msg.isSystem ? '#1976d2' : '#333',
                borderRadius: 16,
                padding: '12px 16px',
                maxWidth: 320,
                fontSize: 14,
                boxShadow: msg.type === 'bot' ? '0 1px 4px rgba(0,0,0,0.04)' : 'none',
                lineHeight: 1.4,
                border: msg.isError ? '1px solid #ffcdd2' : 'none'
              }}>
                {msg.isSystem && (
                  <div style={{ marginBottom: 8, fontSize: 12, opacity: 0.8 }}>
                    💡 시스템 안내
                  </div>
                )}
                {msg.isError && (
                  <div style={{ marginBottom: 8, fontSize: 12, opacity: 0.8 }}>
                    ⚠️ 오류 발생
                  </div>
                )}
                {msg.content.split('\n').map((line, index) => (
                  <div key={index}>{line}</div>
                ))}
                {msg.movies && msg.movies.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <div style={{ fontSize: 12, marginBottom: 8, opacity: 0.7 }}>
                      🎬 추천 영화
                    </div>
                    {msg.movies.map(renderMovieCard)}
                  </div>
                )}
                {msg.endedMovies && msg.endedMovies.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <div style={{ fontSize: 12, marginBottom: 8, opacity: 0.7 }}>
                      ⚠️ 상영이 종료된 영화
                    </div>
                    {msg.endedMovies.map(renderMovieCard)}
                  </div>
                )}
              </div>
            </div>
          ))}
          {isLoading && (
            <div style={{
              display: 'flex', flexDirection: 'row', marginBottom: 16
            }}>
              <div style={{
                background: '#fff',
                borderRadius: 16,
                padding: '12px 16px',
                fontSize: 14,
                boxShadow: '0 1px 4px rgba(0,0,0,0.04)'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{ fontSize: 16 }}>🤖</div>
                  <div style={{ display: 'flex', gap: 4 }}>
                    <div style={{ width: 8, height: 8, background: '#667eea', borderRadius: '50%', animation: 'typing 1.4s infinite ease-in-out' }}></div>
                    <div style={{ width: 8, height: 8, background: '#667eea', borderRadius: '50%', animation: 'typing 1.4s infinite ease-in-out', animationDelay: '0.2s' }}></div>
                    <div style={{ width: 8, height: 8, background: '#667eea', borderRadius: '50%', animation: 'typing 1.4s infinite ease-in-out', animationDelay: '0.4s' }}></div>
                  </div>
                </div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
        <div style={{ padding: '16px 20px', borderTop: '1px solid #eee', background: '#fff' }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="영화에 대해 무엇이든 물어보세요..."
              disabled={isLoading}
              style={{
                flex: 1,
                padding: '12px 16px',
                border: '1px solid #ddd',
                borderRadius: 24,
                fontSize: 14,
                outline: 'none',
                transition: 'border-color 0.2s'
              }}
            />
            <button
              onClick={sendMessage}
              disabled={!input.trim() || isLoading}
              style={{
                padding: '12px 20px',
                background: input.trim() && !isLoading ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' : '#ccc',
                color: 'white',
                border: 'none',
                borderRadius: 24,
                fontSize: 14,
                fontWeight: 600,
                cursor: input.trim() && !isLoading ? 'pointer' : 'not-allowed',
                transition: 'all 0.2s'
              }}
            >
              전송
            </button>
          </div>
        </div>
      </div>
      <style>{`
        @keyframes typing {
          0%, 60%, 100% { transform: translateY(0); }
          30% { transform: translateY(-10px); }
        }
      `}</style>
    </div>
  );
};

export default ChatbotModal; 