import React, { useState, useEffect } from 'react';
import nextIcon from '../assets/next_icon.png';

const ScrollToTopButton = () => {
  const [isVisible, setIsVisible] = useState(false);

  // 스크롤 위치에 따라 버튼 표시/숨김
  useEffect(() => {
    const toggleVisibility = () => {
      if (window.pageYOffset > 300) {
        setIsVisible(true);
      } else {
        setIsVisible(false);
      }
    };

    window.addEventListener('scroll', toggleVisibility);
    return () => window.removeEventListener('scroll', toggleVisibility);
  }, []);

  // 화면 상단으로 스크롤
  const scrollToTop = () => {
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  };

  return (
    <>
      {isVisible && (
        <div
          onClick={scrollToTop}
          style={{
            position: 'fixed',
            bottom: '30px',
            right: '110px', // 왼쪽에 배치
            width: '60px',
            height: '60px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            boxShadow: '0 4px 20px rgba(76, 175, 80, 0.4)',
            zIndex: 9998,
            transition: 'all 0.3s ease',
            color: 'white',
            userSelect: 'none'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.transform = 'scale(1.1)';
            e.currentTarget.style.boxShadow = '0 6px 25px rgba(76, 175, 80, 0.6)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = 'scale(1)';
            e.currentTarget.style.boxShadow = '0 4px 20px rgba(76, 175, 80, 0.4)';
          }}
        >
          <img 
            src={nextIcon} 
            alt="스크롤 탑" 
            style={{
              width: '20px',
              height: '20px',
              transform: 'rotate(-90deg)',
              filter: 'brightness(0) invert(1)' // 흰색으로 변경
            }}
          />
        </div>
      )}
    </>
  );
};

export default ScrollToTopButton; 