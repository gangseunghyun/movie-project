import React from 'react';
import styles from './ReservationPage.module.css';

const ReservationFilter = ({ filters, onFilterChange }) => {
  const handleFilterChange = (key, value) => {
    onFilterChange({
      ...filters,
      [key]: value
    });
  };

  return (
    <div className={styles.filterContainer}>
      {/* 검색 */}
      <div className={styles.searchSection}>
        <input
          type="text"
          placeholder="영화명 또는 영화관으로 검색"
          value={filters.search}
          onChange={(e) => handleFilterChange('search', e.target.value)}
          className={styles.searchInput}
        />
      </div>

      {/* 필터 옵션 */}
      <div className={styles.filterOptions}>
        {/* 기간 필터 */}
        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>기간</label>
          <select
            value={filters.period}
            onChange={(e) => handleFilterChange('period', e.target.value)}
            className={styles.filterSelect}
          >
            <option value="all">전체</option>
            <option value="thisMonth">이번 달</option>
            <option value="lastMonth">지난 달</option>
          </select>
        </div>

        {/* 상태 필터 */}
        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>상태</label>
          <select
            value={filters.status}
            onChange={(e) => handleFilterChange('status', e.target.value)}
            className={styles.filterSelect}
          >
            <option value="all">전체</option>
            <option value="completed">예매완료</option>
            <option value="cancelled">취소됨</option>
          </select>
        </div>

        {/* 정렬 */}
        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>정렬</label>
          <select
            value={filters.sortBy}
            onChange={(e) => handleFilterChange('sortBy', e.target.value)}
            className={styles.filterSelect}
          >
            <option value="latest">최신순</option>
            <option value="oldest">오래된순</option>
            <option value="amount">금액순</option>
          </select>
        </div>
      </div>
    </div>
  );
};

export default ReservationFilter; 