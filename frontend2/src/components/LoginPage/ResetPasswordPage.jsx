import React, { useState } from 'react';
import styles from './ResetPasswordPage.module.css';
import { useNavigate, useLocation } from 'react-router-dom';

export default function ResetPasswordPage() {
    const [password, setPassword] = useState('');
    const [password2, setPassword2] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const navigate = useNavigate();
    const query = new URLSearchParams(useLocation().search);
    const token = query.get('token');

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (password !== password2) {
            setMessage('비밀번호가 일치하지 않습니다.');
            return;
        }
        setLoading(true);
        setMessage('');
        try {
            const formData = new URLSearchParams();
            formData.append('token', token);
            formData.append('newPassword', password);
            formData.append('newPasswordConfirm', password2);
            console.log('token:', token);

            const res = await fetch('http://localhost:80/api/reset-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData.toString(),
            });
            const data = await res.json();
            if (data.success) {
                setMessage('비밀번호가 성공적으로 변경되었습니다.');
                setTimeout(() => navigate('/login'), 2000);
            } else {
                setMessage(data.message || '오류가 발생했습니다.');
            }
        } catch {
            setMessage('서버 오류가 발생했습니다.');
        }
        setLoading(false);
    };

    return (
        <div className="pageWrapper">
            <div className={styles.outer}>
                <h2 className={styles.heading}>비밀번호 재설정</h2>
                <p className={styles.descSub2}>
                    새 비밀번호를 입력해 주세요.
                    비밀번호는 8자 이상이어야 하며,<br /> 영문/숫자/특수문자를조합하는 것을 권장합니다.
                </p>
                <form onSubmit={handleSubmit}>
                    <div className={styles.formBox}>
                        <div className={styles.buttonGroup}>
                            <input
                                className={styles.input}
                                type="password"
                                placeholder="새 비밀번호"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                required
                            />
                            <input
                                className={styles.input}
                                type="password"
                                placeholder="비밀번호 확인"
                                value={password2}
                                onChange={e => setPassword2(e.target.value)}
                                required
                            />
                            <button
                                className={styles.primaryBtn}
                                type="submit"
                                disabled={loading || !password || !password2}
                            >
                                비밀번호 변경
                            </button>
                            <button
                                className={styles.secondaryBtn}
                                type="button"
                                onClick={() => navigate('/login')}
                            >
                                로그인으로 돌아가기
                            </button>
                            {message && <div className={styles.message}>{message}</div>}
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}