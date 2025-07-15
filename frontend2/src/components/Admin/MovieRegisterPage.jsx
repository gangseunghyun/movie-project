import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUser } from '../../contexts/UserContext';
import styles from './MovieRegisterPage.module.css';

const MovieRegisterPage = () => {
    const navigate = useNavigate();
    const { user } = useUser();
    
    const [movieData, setMovieData] = useState({
        movieNm: '',
        movieNmEn: '',
        showTm: '',
        openDt: '',
        genreNm: '',
        watchGradeNm: '',
        companyNm: '',
        description: '',
        posterUrl: ''
    });
    
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // 관리자 권한 확인
    if (!user || user.role !== 'ADMIN') {
        alert('관리자만 접근할 수 있습니다.');
        navigate('/');
        return null;
    }

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setMovieData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!user || user.role !== 'ADMIN') {
            alert('관리자만 영화를 등록할 수 있습니다.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            console.log('영화 등록 시작');
            console.log('전송할 데이터:', movieData);
            
            const response = await fetch(`http://localhost:80/api/movies`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(movieData)
            });

            console.log('API 응답 상태:', response.status);
            const data = await response.json();
            console.log('API 응답 데이터:', data);

            if (response.ok && data.success) {
                alert('영화가 성공적으로 등록되었습니다.');
                navigate('/'); // 메인 페이지로 이동
            } else {
                console.error('API 오류:', data);
                setError(data.message || '영화 등록에 실패했습니다.');
            }
        } catch (error) {
            console.error('영화 등록 실패:', error);
            setError('영화 등록 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        navigate('/');
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <h1>영화 등록</h1>
                <p>새로운 영화를 등록할 수 있습니다.</p>
            </div>

            {error && (
                <div className={styles.error}>
                    {error}
                </div>
            )}

            <form onSubmit={handleSubmit} className={styles.form}>
                <div className={styles.formGroup}>
                    <label htmlFor="movieNm">영화 제목 (한글) *</label>
                    <input
                        type="text"
                        id="movieNm"
                        name="movieNm"
                        value={movieData.movieNm}
                        onChange={handleInputChange}
                        required
                    />
                </div>

                <div className={styles.formGroup}>
                    <label htmlFor="movieNmEn">영화 제목 (영문)</label>
                    <input
                        type="text"
                        id="movieNmEn"
                        name="movieNmEn"
                        value={movieData.movieNmEn}
                        onChange={handleInputChange}
                    />
                </div>

                <div className={styles.formRow}>
                    <div className={styles.formGroup}>
                        <label htmlFor="showTm">상영시간 (분)</label>
                        <input
                            type="text"
                            id="showTm"
                            name="showTm"
                            value={movieData.showTm}
                            onChange={handleInputChange}
                        />
                    </div>

                    <div className={styles.formGroup}>
                        <label htmlFor="openDt">개봉일</label>
                        <input
                            type="text"
                            id="openDt"
                            name="openDt"
                            value={movieData.openDt}
                            onChange={handleInputChange}
                            placeholder="YYYY-MM-DD"
                        />
                    </div>
                </div>

                <div className={styles.formRow}>
                    <div className={styles.formGroup}>
                        <label htmlFor="genreNm">장르</label>
                        <input
                            type="text"
                            id="genreNm"
                            name="genreNm"
                            value={movieData.genreNm}
                            onChange={handleInputChange}
                            placeholder="예: 액션, 드라마, 코미디"
                        />
                    </div>

                    <div className={styles.formGroup}>
                        <label htmlFor="watchGradeNm">관람등급</label>
                        <input
                            type="text"
                            id="watchGradeNm"
                            name="watchGradeNm"
                            value={movieData.watchGradeNm}
                            onChange={handleInputChange}
                            placeholder="예: 전체관람가, 12세이상관람가"
                        />
                    </div>
                </div>

                <div className={styles.formGroup}>
                    <label htmlFor="companyNm">제작사</label>
                    <input
                        type="text"
                        id="companyNm"
                        name="companyNm"
                        value={movieData.companyNm}
                        onChange={handleInputChange}
                    />
                </div>

                <div className={styles.formGroup}>
                    <label htmlFor="description">줄거리</label>
                    <textarea
                        id="description"
                        name="description"
                        value={movieData.description}
                        onChange={handleInputChange}
                        rows="6"
                        placeholder="영화의 줄거리를 입력하세요..."
                    />
                </div>

                <div className={styles.formGroup}>
                    <label htmlFor="posterUrl">포스터 URL</label>
                    <input
                        type="url"
                        id="posterUrl"
                        name="posterUrl"
                        value={movieData.posterUrl}
                        onChange={handleInputChange}
                        placeholder="https://example.com/poster.jpg"
                    />
                </div>

                <div className={styles.buttonGroup}>
                    <button
                        type="button"
                        onClick={handleCancel}
                        className={styles.cancelButton}
                        disabled={loading}
                    >
                        취소
                    </button>
                    <button
                        type="submit"
                        className={styles.submitButton}
                        disabled={loading}
                    >
                        {loading ? '등록 중...' : '영화 등록'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default MovieRegisterPage; 