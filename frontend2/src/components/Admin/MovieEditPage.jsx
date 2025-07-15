import React, { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useUser } from '../../contexts/UserContext';
import styles from './MovieEditPage.module.css';

const MovieEditPage = () => {
    const { movieCd } = useParams();
    const location = useLocation();
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
    useEffect(() => {
        if (!user || user.role !== 'ADMIN') {
            alert('관리자만 접근할 수 있습니다.');
            navigate('/');
            return;
        }
    }, [user, navigate]);

    // 영화 데이터 로드
    useEffect(() => {
        const loadMovieData = async () => {
            if (!movieCd) return;

            // location.state에서 전달받은 영화 정보가 있으면 우선 사용
            if (location.state?.movieDetail) {
                setMovieData(location.state.movieDetail);
                return;
            }

            try {
                const response = await fetch(`http://localhost:80/api/movies/${movieCd}`, {
                    credentials: 'include'
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.success && data.data) {
                        setMovieData(data.data);
                    } else {
                        setError('영화 정보를 불러올 수 없습니다.');
                    }
                } else {
                    setError('영화 정보를 불러올 수 없습니다.');
                }
            } catch (error) {
                console.error('영화 정보 로드 실패:', error);
                setError('영화 정보를 불러오는 중 오류가 발생했습니다.');
            }
        };

        loadMovieData();
    }, [movieCd, location.state]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setMovieData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        console.log('수정 버튼 클릭됨');
        console.log('전송할 데이터:', movieData);
        
        if (!user || user.role !== 'ADMIN') {
            alert('관리자만 수정할 수 있습니다.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            console.log('API 호출 시작:', `http://localhost:80/api/admin/movies/${movieCd}`);
            
            const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(movieData)
            });

            console.log('API 응답 상태:', response.status);
            console.log('API 응답 헤더:', response.headers);

            const data = await response.json();
            console.log('API 응답 데이터:', data);

            console.log('응답 데이터 전체:', data);
            
            // 백엔드 API는 성공 시 AdminMovieDto 객체를 직접 반환
            if (response.ok && data.movieCd) {
                alert('영화가 성공적으로 수정되었습니다.');
                navigate(`/movie-detail/${movieCd}`);
            } else {
                console.error('API 오류:', data);
                const errorMessage = data.message || data.error || '영화 수정에 실패했습니다.';
                setError(errorMessage);
            }
        } catch (error) {
            console.error('영화 수정 실패:', error);
            setError('영화 수정 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        navigate(`/movie-detail/${movieCd}`);
    };

    if (!user || user.role !== 'ADMIN') {
        return null;
    }

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <h1>영화 수정</h1>
                <p>영화 정보를 수정할 수 있습니다.</p>
            </div>

            {error && (
                <div className={styles.error}>
                    {error}
                </div>
            )}

                        <form onSubmit={handleSubmit} className={styles.form}>
                <div className={styles.formGroup}>
                    <label htmlFor="movieNm">영화 제목 (한글)</label>
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
                        {loading ? '수정 중...' : '수정 완료'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default MovieEditPage; 