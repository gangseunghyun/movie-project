import React from 'react';
import { Link } from 'react-router-dom';
import styles from './Footer.module.css';

export default function Footer() {
  return (
    <footer className={styles.footer}>
      {/* ① 내용만 감싸는 래퍼 추가 */}
      <div className={styles.inner}>
        <hr className={styles.divider} />

        {/* 상단 메뉴 */}
        <nav className={styles.menu}>
          <Link to="/customer">고객센터</Link>
          <Link to="/terms">이용약관</Link>
          <Link to="/privacy">개인정보처리방침</Link>  {/* 클릭 가능 */}
          <Link to="/youth">청소년 보호정책</Link>
          <Link to="/legal">법적고지</Link>
          <Link to="/event">이벤트</Link>
        </nav>

        {/* 사업자 정보 */}
        <div className={styles.info}>
          <p>
            대표이사: 이순호 |
            <Link to="/business-info" className={styles.linkInfo}>
              사업자정보확인
            </Link> | 사업자등록번호: 110-85-34274 | 통신판매신고번호: 2024-인천부평-2315호
          </p>
          <p>
            사업장: 인천시 부평구 경원대로 1366,(부평동, 스테이션타워 7F) | 호스팅사업자: 아마존웹서비스코리아 유한책임회사
          </p>
          <p>
            고객센터(평일 09시–17시/점심 13시–14시) | <Link to="/support-board" className={styles.linkInfo}>
              1:1 게시판 문의
            </Link> | 전화번호: 032-521-8889
          </p>
          <p>
            ENM 시청자 상담실(편성 문의 및 시청자 의견): 080-080-0780 | 제휴&마케팅 문의 : partner@megaclab.com
          </p>
        </div>

        <div className={styles.copy}>
          Copyright © MEGACLAB. All Rights Reserved.
        </div>

      </div>
    </footer>
  );
}
