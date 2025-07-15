import React, { useState, useRef, useEffect } from 'react';
import styles from './BannerSlider.module.css';
import banners from './banners';
import previousIcon from '../../assets/previous_icon.png';
import nextIcon from '../../assets/next_icon.png';

export default function BannerSlider() {
  const [current, setCurrent] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const bannerWidth = 1760; // px, 필요시 CSS 변수로 관리 가능
  const gap = 15;
  const total = banners.length;

  useEffect(() => {
    if (isPaused) return;
    const interval = setInterval(() => {
      setCurrent(prev => (prev < total - 1 ? prev + 1 : 0));
    }, 3000);
    return () => clearInterval(interval);
  }, [total, isPaused]);

  const prevSlide = () => {
    if (current > 0) {
      setCurrent((prev) => prev - 1);
    }
  };

  const nextSlide = () => {
    if (current < total - 1) {
      setCurrent((prev) => prev + 1);
    }
  };

  return (
    <div
      className={styles.sliderWrapper}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      {current > 0 && (
        <button
          className={`${styles.navBtn} ${styles.left}`}
          onClick={prevSlide}
        >
          <img src={previousIcon} alt="이전" />
        </button>
      )}

      <div className={styles.indicatorWrapper}>
        {banners.map((_, idx) => (
          <span
            key={idx}
            className={`${styles.indicatorDot} ${current === idx ? styles.activeDot : ''}`}
          />
        ))}
      </div>

      <div className={styles.slider} style={{
        transform: `translateX(-${current * (bannerWidth + gap)}px)`,
        transition: 'transform 0.4s',
        width: `${(bannerWidth + gap) * total - gap}px`,
        display: 'flex',
        gap: `${gap}px`
      }}>
        {banners.map((banner, index) => (
          <div
            key={banner.id}
            className={`${styles.slide} ${index === 0 ? styles.firstBanner : ''}`}
            style={{
              backgroundImage: `url(${banner.image})`,
              width: `${bannerWidth}px`,
              flex: '0 0 auto',
            }}
          >
            <div className={styles.title}>{banner.title}</div>
          </div>
        ))}
      </div>

      {current < total - 1 && (
        <button
          className={`${styles.navBtn} ${styles.right}`}
          onClick={nextSlide}
        >
          <img src={nextIcon} alt="다음" />
        </button>
      )}
    </div>
  );
}
