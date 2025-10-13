import React, { useState } from 'react';
import styles from './FindPasswordPage.module.css';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

export default function FindPasswordPage() {
    const [loginId, setLoginId] = useState('');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');
        try {
            const res = await fetch('/api/forgot-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email }),
            });
            const data = await res.json();
            if (data.success !== false) {
                toast.success('인증 메일이 발송되었습니다. 메일을 확인해 주세요.');
                //window.location.href = '/login';
            } else {
                setMessage(data.message || '오류가 발생했습니다.');
            }
        } catch (err) {
            setMessage('서버 오류가 발생했습니다.');
        }
        setLoading(false);
    };

    return (
        <div className="pageWrapper">
            <div className={styles.outer}>
                <h3 className={styles.heading}>비밀번호 찾기</h3>
                <p className={styles.descSub2}>
                    아이디와 이메일을 입력해 주세요. 아이디에 등록된 이메일주소로 비밀번호<br></br>재설정을 위한 인증 메일이 발송됩니다.
                    회원님의 이메일을 확인하신 후, 12시간<br></br>이내에 비밀번호 재설정을 완료해 주세요.
                </p>
                <form onSubmit={handleSubmit}>
                    <div className={styles.formBox}>


                        <div className={styles.buttonGroup}>

                            
                            <input
                                className={styles.input}
                                type="email"
                                placeholder="이메일"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                required
                            />
                            <button
                                className={styles.primaryBtn}
                                type="submit"
                                disabled={loading || !email}
                            >
                                인증 메일 받기
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
            </div >
        </div>
    );
}