import React, { useState } from 'react';
import ChatbotModal from './Modal/ChatbotModal';

const ChatbotIcon = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const openModal = () => {
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
  };

  return (
    <>
      {/* 챗봇 아이콘 */}
      <div
        onClick={openModal}
        style={{
          position: 'fixed',
          bottom: '30px',
          right: '30px', // 오른쪽에 배치
          width: '60px',
          height: '60px',
          borderRadius: '50%',
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          boxShadow: '0 4px 20px rgba(102, 126, 234, 0.4)',
          zIndex: 9999,
          transition: 'all 0.3s ease',
          fontSize: '24px',
          color: 'white',
          userSelect: 'none'
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.transform = 'scale(1.1)';
          e.currentTarget.style.boxShadow = '0 6px 25px rgba(102, 126, 234, 0.6)';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.transform = 'scale(1)';
          e.currentTarget.style.boxShadow = '0 4px 20px rgba(102, 126, 234, 0.4)';
        }}
      >
        🤖
      </div>

      {/* 챗봇 모달 */}
      <ChatbotModal isOpen={isModalOpen} onClose={closeModal} />
    </>
  );
};

export default ChatbotIcon; 