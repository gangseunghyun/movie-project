// src/index.js
import React from 'react';
import { createRoot } from 'react-dom/client';  // 변경: react-dom → react-dom/client
import App from './App';
import './styles/index.css';  // 전역 CSS

// 1) HTML의 <div id="root"> 요소를 가져옵니다
const container = document.getElementById('root');

// 2) createRoot()로 루트를 만들고
const root = createRoot(container);

// 3) root.render()로 App 컴포넌트를 렌더링합니다
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
