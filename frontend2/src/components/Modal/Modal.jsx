import React from "react";
import styles from "./Modal.module.css";

export default function Modal({ show, onClose, title, children }) {
  if (!show) return null;
  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.content} onClick={e => e.stopPropagation()}>
        <div className={styles.header}>
          <p>{title}</p>
          <span className={styles.close} onClick={onClose}>&#10005;</span>
        </div>
        {children}
      </div>
    </div>
  );
}