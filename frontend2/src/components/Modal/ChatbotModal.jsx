import React, { useState, useRef, useEffect } from 'react';
import styles from './ChatbotModal.module.css';

const ChatbotModal = ({ isOpen, onClose }) => {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // 챗봇 안내 메시지 (실제 동작과 맞게 수정)
  const SYSTEM_GUIDE = `💡 시스템 안내\n안녕하세요! MCP 기반 AI 영화 챗봇입니다! 🤖\n\n자연어로 영화에 대해 무엇이든 물어보세요!\n\n예시:\n• "오늘 기분이 우울한데 영화 추천해줘"\n• "로맨스 영화 중에서 가장 재밌는 거 뭐야?"\n• "인터스텔라 영화에 대해 자세히 알려줘"\n• "F! 더 무비 정보 알려줘"\n• "액션 영화 추천해줘"\n\n💡 감정, 상황, 장르, 영화명 등 어떤 방식으로도 물어보면 챗봇이 알아서 맞춤 추천 또는 영화 정보를 안내합니다!`;

  useEffect(() => {
    if (isOpen) {
      setMessages([
        {
          type: 'bot',
          content: SYSTEM_GUIDE,
          isSystem: true
        }
      ]);
      setInput('');
      // 모달이 열릴 때 입력창에 포커스 주기
      setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus();
        }
      }, 100);
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

  // 추천/감정/상황/장르 키워드 (더 구체적으로 정리)
  const recommendKeywords = [
    '우울', '스트레스', '슬플', '힐링', '행복', '기분이 좋',
    '로맨스', '액션', '코미디', '공포', '스릴러', '사랑', '러브', '스릴', '어드벤처', '웃음', '재미', '호러',
    '인기', '최고', '베스트', '추천', '볼만한', '재밌는', '좋은'
  ];
  // 불용어
  const stopwords = [
    '볼', '볼만한', '추천', '추천해줘', '할 때', '영화', '정보', '알려줘', '뭐야', '어떤가', '어떤지', '어때', '어떻니', '어떠니'
  ];

  // 영화명 추출 패턴 (특수문자 포함)
  const patterns = [
    /([가-힣A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~` ]+)(?:은|는|이|가)?\s*(?:무슨|어떤)?\s*영화/,
    /([가-힣A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~` ]+)\s*에 대해/,
    /([가-힣A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~` ]+)\s*알려줘/,
    /([가-힣A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~` ]+)\s*정보/,
    /([가-힣A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~` ]+)\s*뭐야/,
    /([가-힣A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~` ]+)\s*영화/
  ];

  // 영화명 추출 (실제 서비스 수준)
  const extractMovieName = (msg) => {
    let simpleMovieName = msg.trim().replace(/[는은이가을를의에에서로으로]$/, '');
    // 불용어/추천어 포함시 영화명으로 인정하지 않음
    if (
      simpleMovieName.length >= 2 &&
      simpleMovieName.length <= 30 &&
      !stopwords.some(word => simpleMovieName.includes(word)) &&
      !recommendKeywords.some(word => simpleMovieName.includes(word))
    ) {
      return simpleMovieName;
    }
    for (const pattern of patterns) {
      const match = msg.match(pattern);
      if (match && match[1]) {
        let extracted = match[1].trim().replace(/[는은이가을를의에에서로으로]$/, '');
        if (
          extracted.length >= 2 &&
          extracted.length <= 30 &&
          !stopwords.some(word => extracted.includes(word)) &&
          !recommendKeywords.some(word => extracted.includes(word))
        ) {
          return extracted;
        }
      }
    }
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

  // classifyQuery 개선: 감정/상황별로 situation 파라미터를 넘김
  const classifyQuery = (input) => {
    const lowerInput = input.toLowerCase();
    // 0. 인기/박스오피스 키워드 우선 분기
    if (lowerInput.includes('인기') || lowerInput.includes('요즘 인기') || lowerInput.includes('박스오피스')) {
      return { type: 'BOXOFFICE_RECOMMENDATION' };
    }
    // 1. 감정/상황/장르 키워드가 있으면 situation 파라미터로 분기
    if (recommendKeywords.some(word => lowerInput.includes(word))) {
      if (lowerInput.includes('우울')) return { type: 'EMOTIONAL_RECOMMENDATION', situation: '우울할 때' };
      if (lowerInput.includes('스트레스')) return { type: 'EMOTIONAL_RECOMMENDATION', situation: '스트레스 받을 때' };
      if (lowerInput.includes('행복') || lowerInput.includes('기분이 좋')) return { type: 'EMOTIONAL_RECOMMENDATION', situation: '기분이 좋을 때' };
      if (lowerInput.includes('힐링')) return { type: 'EMOTIONAL_RECOMMENDATION', situation: '힐링이 필요할 때' };
      if (lowerInput.includes('슬픔') || lowerInput.includes('슬플')) return { type: 'EMOTIONAL_RECOMMENDATION', situation: '슬플 때' };
      // 장르별
      if (lowerInput.includes('로맨스') || lowerInput.includes('사랑') || lowerInput.includes('러브')) return { type: 'GENRE_RECOMMENDATION', genre: '로맨스' };
      if (lowerInput.includes('액션') || lowerInput.includes('스릴') || lowerInput.includes('어드벤처')) return { type: 'GENRE_RECOMMENDATION', genre: '액션' };
      if (lowerInput.includes('코미디') || lowerInput.includes('웃음') || lowerInput.includes('재미')) return { type: 'GENRE_RECOMMENDATION', genre: '코미디' };
      if (lowerInput.includes('공포') || lowerInput.includes('호러')) return { type: 'GENRE_RECOMMENDATION', genre: '공포' };
      if (lowerInput.includes('스릴러') || lowerInput.includes('thriller')) return { type: 'GENRE_RECOMMENDATION', genre: '스릴러' };
      // 기타 상황/감정은 일반 추천
      return { type: 'RECOMMENDATION' };
    }
    // 2. 영화명 추출 (불용어/추천어 포함시 제외)
    const movieName = extractMovieName(input);
    if (movieName && movieName.length >= 2) {
      return { type: 'MOVIE_INFO', movieName: movieName };
    }
    // 3. 아무것도 아니면 일반 추천
    return { type: 'RECOMMENDATION' };
  };

  // 상황별 안내 멘트 및 추천 장르 매핑
  const situationMessages = {
    '우울할 때': '기분이 우울하시군요! 😔 그런 날에는 마음을 따뜻하게 해주는 영화가 최고예요.\n제가 특별히 힐링 영화를 찾아봤어요!',
    '힐링이 필요할 때': '마음이 지칠 땐 이런 힐링 영화가 도움이 될 거예요! 🌱',
    '슬플 때': '마음을 위로해줄 영화들을 추천해드릴게요! 💧',
    '스트레스 받을 때': '스트레스를 확 날려줄 영화들을 추천해드릴게요! 💥\n액션이나 코미디 영화로 기분 전환하세요!',
    '기분이 좋을 때': '기분 좋은 날엔 이런 영화들이 잘 어울려요! 😄',
  };
  const situationGenres = {
    '우울할 때': 'healing',
    '힐링이 필요할 때': 'healing',
    '슬플 때': 'healing',
    '스트레스 받을 때': 'action_comedy',
    '기분이 좋을 때': 'bright',
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
        // 카테고리성 키워드가 영화명으로 들어온 경우 searchMovies로 분기
        const lowerMovieName = queryType.movieName.toLowerCase();
        if (['개봉예정작', '개봉 예정작', 'coming soon'].includes(lowerMovieName)) {
          // 개봉예정작은 searchMovies(type: 'coming_soon')로 분기
          const aiResponse = await generateRecommendationResponse({ type: 'RECOMMENDATION' }, input, '개봉 예정 영화들을 추천해드릴게요! 📅', { type: 'coming_soon' });
          setMessages(prev => [...prev, {
            type: 'bot',
            content: aiResponse.message,
            movies: aiResponse.movies || [],
            endedMovies: aiResponse.endedMovies || [],
            messageType: aiResponse.type
          }]);
          setIsLoading(false);
          return;
        } else if ([ '상영중', '상영 중', 'now playing', '현재 상영작', '현재 상영중' ].includes(lowerMovieName)) {
          // 상영중은 searchMovies(type: 'now_playing')로 분기
          const aiResponse = await generateRecommendationResponse({ type: 'RECOMMENDATION' }, input, '현재 상영중인 영화들을 추천해드릴게요! 🎬', { type: 'now_playing' });
          setMessages(prev => [...prev, {
            type: 'bot',
            content: aiResponse.message,
            movies: aiResponse.movies || [],
            endedMovies: aiResponse.endedMovies || [],
            messageType: aiResponse.type
          }]);
          setIsLoading(false);
          return;
        }
        // 일반 영화명은 기존대로 getMovieInfo 사용
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
        let message = '';
        let apiParameters = {};
        if (queryType.type === 'BOXOFFICE_RECOMMENDATION') {
          message = '요즘 박스오피스 상위 영화를 추천해드릴게요! 🎬';
          apiParameters = { type: 'boxoffice' };
        } else if (queryType.type === 'EMOTIONAL_RECOMMENDATION' && queryType.situation) {
          message = situationMessages[queryType.situation] || '흥미로운 질문이네요! 🤔\n제가 인기 있는 영화들을 추천해드릴게요!';
          // 상황별 추천 장르 파라미터 추가
          if (situationGenres[queryType.situation] === 'healing') {
            apiParameters = { situation: queryType.situation };
          } else if (situationGenres[queryType.situation] === 'action_comedy') {
            apiParameters = { genre: '액션,코미디' };
          } else if (situationGenres[queryType.situation] === 'bright') {
            apiParameters = { genre: '코미디,음악,가족,모험' };
          } else {
            apiParameters = { situation: queryType.situation };
          }
        } else if (queryType.type === 'GENRE_RECOMMENDATION') {
          message = `장르별 추천! \"${queryType.genre}\" 영화를 추천해드릴게요! 🎬`;
          apiParameters = { genre: queryType.genre };
        } else {
          // 기본 추천: 다양한 카테고리로 분산 추천
          const randomCategory = Math.floor(Math.random() * 4);
          switch (randomCategory) {
            case 0:
              message = '인기 있는 영화들을 추천해드릴게요! 🎬\n현재 가장 많은 관객들이 찾고 있는 영화들입니다.';
              apiParameters = { type: 'popular' };
              break;
            case 1:
              message = '최신 개봉 영화들을 추천해드릴게요! 🆕\n새롭게 개봉한 영화들로 신선한 감동을 느껴보세요.';
              apiParameters = { type: 'latest' };
              break;
            case 2:
              message = '개봉 예정 영화들을 추천해드릴게요! 📅\n곧 개봉할 영화들을 미리 확인해보세요.';
              apiParameters = { type: 'coming_soon' };
              break;
            case 3:
              message = '다양한 장르의 영화들을 추천해드릴게요! 🎭\n로맨스, 액션, 코미디 등 다양한 장르를 골고루 준비했어요.';
              apiParameters = { genre: '로맨스,액션,코미디' };
              break;
          }
        }
        // 추천 응답 생성 (message, apiParameters를 인자로 전달)
        const aiResponse = await generateRecommendationResponse(queryType, input, message, apiParameters);
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
      // 메시지 전송 후 입력창에 포커스 다시 주기
      setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus();
        }
      }, 100);
    }
  };

  // generateRecommendationResponse에서 상황별로 안내 멘트/추천 장르 분기
  const generateRecommendationResponse = async (queryType, originalInput, message, apiParameters) => {
    try {
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
      if (data.success && data.result && data.result.movies) {
        let movies = data.result.movies || [];
        // 중복 제거: movieCd 기준으로 중복 제거
        const uniqueMovies = Array.from(new Map(movies.map(m => [m.movieCd, m])).values());
        const nowOrSoon = uniqueMovies.filter(m => {
          if (!m.status) return true;
          const s = (m.status || '').toUpperCase();
          return s === 'NOW_PLAYING' || s === 'COMING_SOON' || s === '개봉중' || s === '개봉예정' || s === 'NOWPLAYING' || s === 'COMINGSOON';
        });
        const ended = uniqueMovies.filter(m => {
          if (!m.status) return false;
          const s = (m.status || '').toUpperCase();
          return s === 'ENDED' || s === '상영종료' || s === 'ENDED';
        });
        if (nowOrSoon.length > 0) {
          return {
            message: message,
            movies: nowOrSoon,
            endedMovies: ended,
            type: queryType.type
          };
        } else if (ended.length > 0) {
          message += `\n\n⚠️ 현재 상영중이거나 개봉예정인 영화가 없어 상영종료 영화를 안내합니다.`;
          return {
            message: message,
            movies: ended,
            endedMovies: [],
            type: queryType.type
          };
        } else if (movies.length > 0) {
          message += `\n\n⚠️ 추천할 영화가 없습니다. 하지만 아래 영화들을 참고해보세요.`;
          return {
            message: message,
            movies: movies,
            endedMovies: [],
            type: queryType.type
          };
        } else {
          message += `\n\n⚠️ 추천할 영화가 없습니다.`;
          return {
            message: message,
            movies: [],
            endedMovies: [],
            type: queryType.type
          };
        }
      } else {
        throw new Error(data.error || '영화 추천을 가져오는데 실패했습니다.');
      }
    } catch (error) {
      return {
        message: `죄송합니다. 영화 추천을 가져오는 중에 문제가 발생했습니다. 😅\n\n잠시 후 다시 시도해주세요!`,
        movies: [],
        endedMovies: [],
        type: 'ERROR'
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
    <div key={movie.movieCd} className={styles.movieCard}>
      <div className={styles.movieCardContent}>
        <div className={styles.moviePoster}>
          {movie.posterUrl ? (
            <img 
              src={movie.posterUrl} 
              alt={movie.movieNm}
            />
          ) : (
            <div className={styles.moviePosterPlaceholder}>
              🎬
            </div>
          )}
        </div>
        <div className={styles.movieInfo}>
          <h4 className={styles.movieTitle}>{movie.movieNm}</h4>
          <p className={styles.movieGenre}>{movie.genreNm}</p>
          {movie.averageRating && movie.averageRating > 0 && (
            <p className={styles.movieRating}>
              ⭐ {movie.averageRating.toFixed(1)}
            </p>
          )}
          {movie.openDt && (
            <p className={styles.movieDate}>
              📅 {movie.openDt}
            </p>
          )}
        </div>
      </div>
    </div>
  );

  if (!isOpen) return null;

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modalContainer} onClick={e => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          🤖 MCP AI 영화 챗봇
          <div className={styles.headerSubtitle}>
            Model Context Protocol 기반
          </div>
          <button onClick={onClose} className={styles.closeButton}>✕</button>
        </div>
        <div className={styles.messagesContainer}>
          {messages.map((msg, i) => (
            <div key={i} className={`${styles.messageWrapper} ${msg.type === 'user' ? styles.user : ''}`}>
              <div className={`${styles.messageBubble} ${styles[msg.type]} ${msg.isSystem ? styles.system : ''} ${msg.isError ? styles.error : ''}`}>
                {msg.isSystem && (
                  <div className={styles.messageLabel}>
                    💡 시스템 안내
                  </div>
                )}
                {msg.isError && (
                  <div className={styles.messageLabel}>
                    ⚠️ 오류 발생
                  </div>
                )}
                {msg.content.split('\n').map((line, index) => (
                  <div key={index}>{line}</div>
                ))}
                {msg.movies && msg.movies.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <div className={styles.messageLabel}>
                      🎬 추천 영화
                    </div>
                    {Array.from(new Map(msg.movies.map(m => [m.movieCd, m])).values()).map(renderMovieCard)}
                  </div>
                )}
                {msg.endedMovies && msg.endedMovies.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <div className={styles.messageLabel}>
                      ⚠️ 상영이 종료된 영화
                    </div>
                    {Array.from(new Map(msg.endedMovies.map(m => [m.movieCd, m])).values()).map(renderMovieCard)}
                  </div>
                )}
              </div>
            </div>
          ))}
          {isLoading && (
            <div className={styles.loadingWrapper}>
              <div className={styles.loadingBubble}>
                <div className={styles.loadingContent}>
                  <div style={{ fontSize: 16 }}>🤖</div>
                  <div className={styles.loadingDots}>
                    <div className={styles.loadingDot}></div>
                    <div className={styles.loadingDot}></div>
                    <div className={styles.loadingDot}></div>
                  </div>
                </div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
        <div className={styles.inputContainer}>
          <div className={styles.inputWrapper}>
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="영화에 대해 무엇이든 물어보세요..."
              disabled={isLoading}
              className={styles.input}
            />
            <button
              onClick={sendMessage}
              disabled={!input.trim() || isLoading}
              className={styles.sendButton}
            >
              전송
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatbotModal; 