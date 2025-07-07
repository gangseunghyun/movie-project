import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';

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
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage = { type: 'user', content: input };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      const response = await axios.post('http://localhost:80/api/chatbot/chat', {
        message: input
      }, { withCredentials: true });

      const botMessage = {
        type: 'bot',
        content: response.data.message,
        movies: response.data.movies || [],
        type: response.data.type
      };

      setMessages(prev => [...prev, botMessage]);
    } catch (error) {
      console.error('챗봇 API 호출 실패:', error);
      const errorMessage = {
        type: 'bot',
        content: '죄송합니다. AI 서비스에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.',
        movies: [],
        isError: true
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

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
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 
          borderRadius: '4px', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center', 
          fontSize: '20px',
          color: 'white'
        }}>
          🎬
        </div>
        <div style={{ flex: 1 }}>
          <h4 style={{ margin: '0 0 4px 0', fontSize: '14px', fontWeight: '600', color: '#333' }}>{movie.movieNm}</h4>
          <p style={{ margin: '0 0 2px 0', fontSize: '12px', color: '#666' }}>{movie.genreNm}</p>
          {movie.averageRating && (
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
        <div style={{ padding: 16, borderTop: '1px solid #eee', background: 'white' }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="영화에 대해 무엇이든 물어보세요..."
              style={{
                flex: 1,
                padding: '12px 16px',
                border: '1px solid #ddd',
                borderRadius: 24,
                fontSize: 14,
                outline: 'none'
              }}
            />
            <button
              onClick={sendMessage}
              disabled={isLoading || !input.trim()}
              style={{
                padding: '12px 20px',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
                border: 'none',
                borderRadius: 24,
                fontSize: 14,
                fontWeight: 600,
                cursor: isLoading || !input.trim() ? 'not-allowed' : 'pointer',
                opacity: isLoading || !input.trim() ? 0.6 : 1
              }}
            >
              전송
            </button>
          </div>
        </div>
      </div>
      <style jsx>{`
        @keyframes typing {
          0%, 60%, 100% { transform: translateY(0); }
          30% { transform: translateY(-10px); }
        }
      `}</style>
    </div>
  );
};

export default ChatbotModal; 