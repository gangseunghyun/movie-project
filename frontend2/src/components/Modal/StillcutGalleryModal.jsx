import React, { useState } from 'react';
import styles from './StillcutGalleryModal.module.css';
import closeIcon from '../../assets/close_icon.png';
import previousIcon from '../../assets/previous_icon.png';
import nextIcon from '../../assets/next_icon.png';

const StillcutGalleryModal = ({ open, onClose, stillcuts, initialIndex = 0 }) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);

  // 모달이 열릴 때마다 initialIndex로 초기화
  React.useEffect(() => {
    if (open) {
      setCurrentIndex(initialIndex);
    }
  }, [open, initialIndex]);

  if (!open || !stillcuts || stillcuts.length === 0) return null;

  const handlePrevious = () => {
    setCurrentIndex(prev => prev > 0 ? prev - 1 : stillcuts.length - 1);
  };

  const handleNext = () => {
    setCurrentIndex(prev => prev < stillcuts.length - 1 ? prev + 1 : 0);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      onClose();
    } else if (e.key === 'ArrowLeft') {
      handlePrevious();
    } else if (e.key === 'ArrowRight') {
      handleNext();
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={e => e.stopPropagation()}>
        <div className={styles.header}>
          <h3 className={styles.title}>스틸컷 갤러리</h3>
          <button className={styles.closeButton} onClick={onClose}>
            <img src={closeIcon} alt="닫기" />
          </button>
        </div>
        
        <div className={styles.content}>
          <div className={styles.mainImageContainer}>
            <button 
              className={`${styles.navButton} ${styles.prevButton}`} 
              onClick={handlePrevious}
              disabled={stillcuts.length <= 1}
            >
              <img src={previousIcon} alt="이전" />
            </button>
            
            <div className={styles.mainImageWrapper}>
              <img 
                src={stillcuts[currentIndex].imageUrl} 
                alt={`스틸컷 ${currentIndex + 1}`} 
                className={styles.mainImage}
              />
            </div>
            
            <button 
              className={`${styles.navButton} ${styles.nextButton}`} 
              onClick={handleNext}
              disabled={stillcuts.length <= 1}
            >
              <img src={nextIcon} alt="다음" />
            </button>
          </div>
          
          <div className={styles.counter}>
            {currentIndex + 1} / {stillcuts.length}
          </div>
        </div>
      </div>
    </div>
  );
};

export default StillcutGalleryModal; 