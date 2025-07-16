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
        posterUrl: '',
        directorName: '',
        actorNames: '',
        tags: '',
        nationNm: '',
        prdtYear: '',
        prdtStatNm: '',
        typeNm: ''
    });
    
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // 이미지 업로드 관련 상태
    const [posterFile, setPosterFile] = useState(null);
    const [posterPreview, setPosterPreview] = useState('');
    const [posterUploadMethod, setPosterUploadMethod] = useState('url');
    const [posterUrl, setPosterUrl] = useState('');
    
    const [stillcutFiles, setStillcutFiles] = useState([]);
    const [stillcutUrls, setStillcutUrls] = useState([]);
    const [stillcutUploadMethod, setStillcutUploadMethod] = useState('file');
    const [stillcutUrlInputs, setStillcutUrlInputs] = useState(['']);
    
    const [directorImageFile, setDirectorImageFile] = useState(null);
    const [directorImagePreview, setDirectorImagePreview] = useState('');
    const [directorImageUrl, setDirectorImageUrl] = useState('');
    const [directorImageUploadMethod, setDirectorImageUploadMethod] = useState('url');
    const [directorImageUrlInput, setDirectorImageUrlInput] = useState('');
    
    const [actorImageFiles, setActorImageFiles] = useState([]);
    const [actorImagePreviews, setActorImagePreviews] = useState([]);
    const [actorImageUrls, setActorImageUrls] = useState([]);
    const [actorImageUploadMethod, setActorImageUploadMethod] = useState('file');
    const [actorImageUrlInputs, setActorImageUrlInputs] = useState(['']);

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
                const detail = location.state.movieDetail;
                setMovieData(detail);
                setPosterUrl(detail.posterUrl || '');
                setStillcutUrls(detail.stillcuts?.map(s => s.imageUrl) || []);
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
                        setPosterUrl(data.data.posterUrl || '');
                        setStillcutUrls(data.data.stillcuts?.map(s => s.imageUrl) || []);
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

    // 포스터 파일 선택
    const handlePosterFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setPosterFile(file);
            setPosterPreview(URL.createObjectURL(file));
            setPosterUploadMethod('file');
            setPosterUrl('');
        }
    };

    // 포스터 URL 입력
    const handlePosterUrlChange = (e) => {
        const { value } = e.target;
        setPosterUrl(value);
        setPosterUploadMethod('url');
        setPosterFile(null);
        setPosterPreview('');
    };

    // 포스터 업로드
    const handlePosterUpload = async () => {
        if (posterUploadMethod === 'file') {
            if (!posterFile) {
                alert('포스터 파일을 선택해주세요.');
                return;
            }

            const formData = new FormData();
            formData.append('image', posterFile);

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/poster`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setPosterUrl(data.imageUrl);
                    setMovieData(prev => ({ ...prev, posterUrl: data.imageUrl }));
                    alert('포스터 업로드 성공!');
                } else {
                    alert('포스터 업로드 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('포스터 업로드 실패:', error);
                alert('포스터 업로드 중 오류가 발생했습니다.');
            }
        } else {
            // URL 방식
            if (!posterUrl || !posterUrl.trim()) {
                alert('포스터 URL을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/poster-url`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify({ posterUrl: posterUrl })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setMovieData(prev => ({ ...prev, posterUrl: posterUrl }));
                    alert('포스터 URL이 설정되었습니다!');
                } else {
                    alert('포스터 URL 설정 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('포스터 URL 설정 실패:', error);
                alert('포스터 URL 설정 중 오류가 발생했습니다.');
            }
        }
    };

    // 스틸컷 파일 선택
    const handleStillcutChange = (e) => {
        setStillcutFiles([...e.target.files]);
        setStillcutUploadMethod('file');
        setStillcutUrlInputs(['']);
    };

    // 스틸컷 URL 입력
    const handleStillcutUrlChange = (index, value) => {
        const newInputs = [...stillcutUrlInputs];
        newInputs[index] = value;
        setStillcutUrlInputs(newInputs);
        setStillcutUploadMethod('url');
        setStillcutFiles([]);
    };

    const addStillcutUrlInput = () => {
        setStillcutUrlInputs([...stillcutUrlInputs, '']);
    };

    const removeStillcutUrlInput = (index) => {
        const newInputs = stillcutUrlInputs.filter((_, i) => i !== index);
        setStillcutUrlInputs(newInputs);
    };

    // 스틸컷 업로드
    const handleStillcutUpload = async () => {
        if (stillcutUploadMethod === 'file') {
            if (stillcutFiles.length === 0) {
                alert('스틸컷 파일을 선택해주세요.');
                return;
            }

            const formData = new FormData();
            stillcutFiles.forEach(file => {
                formData.append('images', file);
            });

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/stillcuts`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    const newUrls = data.imageUrls || [];
                    setStillcutUrls(newUrls);
                    setMovieData(prev => ({ ...prev, stillcutUrls: newUrls }));
                    alert('스틸컷 업로드 성공!');
                } else {
                    alert('스틸컷 업로드 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('스틸컷 업로드 실패:', error);
                alert('스틸컷 업로드 중 오류가 발생했습니다.');
            }
        } else {
            // URL 방식
            const urls = stillcutUrlInputs.filter(url => url.trim() !== '');
            if (urls.length === 0) {
                alert('스틸컷 URL을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/stillcut-urls`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify({ imageUrls: urls })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    const newUrls = data.imageUrls || [];
                    setStillcutUrls(newUrls);
                    setMovieData(prev => ({ ...prev, stillcutUrls: newUrls }));
                    alert('스틸컷 URL이 설정되었습니다!');
                } else {
                    alert('스틸컷 URL 설정 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('스틸컷 URL 설정 실패:', error);
                alert('스틸컷 URL 설정 중 오류가 발생했습니다.');
            }
        }
    };

    // 감독 이미지 파일 선택
    const handleDirectorImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setDirectorImageFile(file);
            setDirectorImagePreview(URL.createObjectURL(file));
            setDirectorImageUploadMethod('file');
            setDirectorImageUrlInput('');
        }
    };

    // 감독 이미지 URL 입력
    const handleDirectorImageUrlChange = (e) => {
        const { value } = e.target;
        setDirectorImageUrlInput(value);
        setDirectorImageUploadMethod('url');
        setDirectorImageFile(null);
        setDirectorImagePreview('');
    };

    // 감독 이미지 업로드
    const handleDirectorImageUpload = async () => {
        if (directorImageUploadMethod === 'file') {
            if (!directorImageFile) {
                alert('감독 이미지 파일을 선택해주세요.');
                return;
            }

            const formData = new FormData();
            formData.append('image', directorImageFile);

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/director-image`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setDirectorImageUrl(data.imageUrl);
                    alert('감독 이미지 업로드 성공!');
                } else {
                    alert('감독 이미지 업로드 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('감독 이미지 업로드 실패:', error);
                alert('감독 이미지 업로드 중 오류가 발생했습니다.');
            }
        } else {
            // URL 방식
            if (!directorImageUrlInput || !directorImageUrlInput.trim()) {
                alert('감독 이미지 URL을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/director-image-url`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify({ imageUrl: directorImageUrlInput })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setDirectorImageUrl(data.imageUrl);
                    alert('감독 이미지 URL이 설정되었습니다!');
                } else {
                    alert('감독 이미지 URL 설정 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('감독 이미지 URL 설정 실패:', error);
                alert('감독 이미지 URL 설정 중 오류가 발생했습니다.');
            }
        }
    };

    // 배우 이미지 파일 선택
    const handleActorImageChange = (e) => {
        setActorImageFiles([...e.target.files]);
        setActorImageUploadMethod('file');
        setActorImageUrlInputs(['']);
    };

    // 배우 이미지 URL 입력
    const handleActorImageUrlChange = (index, value) => {
        const newInputs = [...actorImageUrlInputs];
        newInputs[index] = value;
        setActorImageUrlInputs(newInputs);
        setActorImageUploadMethod('url');
        setActorImageFiles([]);
    };

    const addActorImageUrlInput = () => {
        setActorImageUrlInputs([...actorImageUrlInputs, '']);
    };

    const removeActorImageUrlInput = (index) => {
        const newInputs = actorImageUrlInputs.filter((_, i) => i !== index);
        setActorImageUrlInputs(newInputs);
    };

    // 배우 이미지 업로드
    const handleActorImageUpload = async () => {
        if (actorImageUploadMethod === 'file') {
            if (actorImageFiles.length === 0) {
                alert('배우 이미지 파일을 선택해주세요.');
                return;
            }

            const formData = new FormData();
            actorImageFiles.forEach(file => {
                formData.append('images', file);
            });

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/actor-images`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setActorImageUrls(data.imageUrls || []);
                    alert('배우 이미지 업로드 성공!');
                } else {
                    alert('배우 이미지 업로드 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('배우 이미지 업로드 실패:', error);
                alert('배우 이미지 업로드 중 오류가 발생했습니다.');
            }
        } else {
            // URL 방식
            const urls = actorImageUrlInputs.filter(url => url.trim() !== '');
            if (urls.length === 0) {
                alert('배우 이미지 URL을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`http://localhost:80/api/admin/movies/${movieCd}/actor-image-urls`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify({ imageUrls: urls })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setActorImageUrls(data.imageUrls || []);
                    alert('배우 이미지 URL이 설정되었습니다!');
                } else {
                    alert('배우 이미지 URL 설정 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('배우 이미지 URL 설정 실패:', error);
                alert('배우 이미지 URL 설정 중 오류가 발생했습니다.');
            }
        }
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

                {/* 포스터 업로드 섹션 */}
                <div className={styles.imageSection}>
                    <h3>포스터 이미지</h3>
                    <div className={styles.uploadMethod}>
                        <label>
                            <input
                                type="radio"
                                name="posterUploadMethod"
                                value="file"
                                checked={posterUploadMethod === 'file'}
                                onChange={() => setPosterUploadMethod('file')}
                            />
                            파일 업로드
                        </label>
                        <label>
                            <input
                                type="radio"
                                name="posterUploadMethod"
                                value="url"
                                checked={posterUploadMethod === 'url'}
                                onChange={() => setPosterUploadMethod('url')}
                            />
                            URL 입력
                        </label>
                    </div>

                    {posterUploadMethod === 'file' ? (
                        <div className={styles.fileUpload}>
                            <input
                                type="file"
                                accept="image/*"
                                onChange={handlePosterFileChange}
                            />
                            {posterPreview && (
                                <img src={posterPreview} alt="포스터 미리보기" className={styles.preview} />
                            )}
                        </div>
                    ) : (
                        <div className={styles.urlInput}>
                            <input
                                type="url"
                                placeholder="포스터 이미지 URL을 입력하세요"
                                value={posterUrl}
                                onChange={handlePosterUrlChange}
                            />
                        </div>
                    )}
                    <button type="button" onClick={handlePosterUpload} className={styles.uploadButton}>
                        포스터 업로드
                    </button>
                </div>

                {/* 감독 이미지 업로드 섹션 */}
                <div className={styles.imageSection}>
                    <h3>감독 이미지</h3>
                    <div className={styles.uploadMethod}>
                        <label>
                            <input
                                type="radio"
                                name="directorImageUploadMethod"
                                value="file"
                                checked={directorImageUploadMethod === 'file'}
                                onChange={() => setDirectorImageUploadMethod('file')}
                            />
                            파일 업로드
                        </label>
                        <label>
                            <input
                                type="radio"
                                name="directorImageUploadMethod"
                                value="url"
                                checked={directorImageUploadMethod === 'url'}
                                onChange={() => setDirectorImageUploadMethod('url')}
                            />
                            URL 입력
                        </label>
                    </div>

                    {directorImageUploadMethod === 'file' ? (
                        <div className={styles.fileUpload}>
                            <input
                                type="file"
                                accept="image/*"
                                onChange={handleDirectorImageChange}
                            />
                            {directorImagePreview && (
                                <img src={directorImagePreview} alt="감독 이미지 미리보기" className={styles.preview} />
                            )}
                        </div>
                    ) : (
                        <div className={styles.urlInput}>
                            <input
                                type="url"
                                placeholder="감독 이미지 URL을 입력하세요"
                                value={directorImageUrlInput}
                                onChange={handleDirectorImageUrlChange}
                            />
                        </div>
                    )}
                    <button type="button" onClick={handleDirectorImageUpload} className={styles.uploadButton}>
                        감독 이미지 업로드
                    </button>
                </div>

                {/* 배우 이미지 업로드 섹션 */}
                <div className={styles.imageSection}>
                    <h3>배우 이미지</h3>
                    <div className={styles.uploadMethod}>
                        <label>
                            <input
                                type="radio"
                                name="actorImageUploadMethod"
                                value="file"
                                checked={actorImageUploadMethod === 'file'}
                                onChange={() => setActorImageUploadMethod('file')}
                            />
                            파일 업로드
                        </label>
                        <label>
                            <input
                                type="radio"
                                name="actorImageUploadMethod"
                                value="url"
                                checked={actorImageUploadMethod === 'url'}
                                onChange={() => setActorImageUploadMethod('url')}
                            />
                            URL 입력
                        </label>
                    </div>

                    {actorImageUploadMethod === 'file' ? (
                        <div className={styles.fileUpload}>
                            <input
                                type="file"
                                accept="image/*"
                                multiple
                                onChange={handleActorImageChange}
                            />
                            {actorImagePreviews.length > 0 && (
                                <div className={styles.previewGrid}>
                                    {actorImagePreviews.map((preview, index) => (
                                        <img key={index} src={preview} alt={`배우 이미지 미리보기 ${index + 1}`} className={styles.preview} />
                                    ))}
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className={styles.urlInputs}>
                            {actorImageUrlInputs.map((url, index) => (
                                <div key={index} className={styles.urlInputRow}>
                                    <input
                                        type="url"
                                        placeholder={`배우 ${index + 1} 이미지 URL을 입력하세요`}
                                        value={url}
                                        onChange={(e) => handleActorImageUrlChange(index, e.target.value)}
                                    />
                                    {actorImageUrlInputs.length > 1 && (
                                        <button type="button" onClick={() => removeActorImageUrlInput(index)} className={styles.removeButton}>
                                            삭제
                                        </button>
                                    )}
                                </div>
                            ))}
                            <button type="button" onClick={addActorImageUrlInput} className={styles.addButton}>
                                URL 추가
                            </button>
                        </div>
                    )}
                    <button type="button" onClick={handleActorImageUpload} className={styles.uploadButton}>
                        배우 이미지 업로드
                    </button>
                </div>

                {/* 스틸컷 업로드 섹션 */}
                <div className={styles.imageSection}>
                    <h3>스틸컷 이미지</h3>
                    <div className={styles.uploadMethod}>
                        <label>
                            <input
                                type="radio"
                                name="stillcutUploadMethod"
                                value="file"
                                checked={stillcutUploadMethod === 'file'}
                                onChange={() => setStillcutUploadMethod('file')}
                            />
                            파일 업로드
                        </label>
                        <label>
                            <input
                                type="radio"
                                name="stillcutUploadMethod"
                                value="url"
                                checked={stillcutUploadMethod === 'url'}
                                onChange={() => setStillcutUploadMethod('url')}
                            />
                            URL 입력
                        </label>
                    </div>

                    {stillcutUploadMethod === 'file' ? (
                        <div className={styles.fileUpload}>
                            <input
                                type="file"
                                accept="image/*"
                                multiple
                                onChange={handleStillcutChange}
                            />
                        </div>
                    ) : (
                        <div className={styles.urlInputs}>
                            {stillcutUrlInputs.map((url, index) => (
                                <div key={index} className={styles.urlInputRow}>
                                    <input
                                        type="url"
                                        placeholder={`스틸컷 ${index + 1} 이미지 URL을 입력하세요`}
                                        value={url}
                                        onChange={(e) => handleStillcutUrlChange(index, e.target.value)}
                                    />
                                    {stillcutUrlInputs.length > 1 && (
                                        <button type="button" onClick={() => removeStillcutUrlInput(index)} className={styles.removeButton}>
                                            삭제
                                        </button>
                                    )}
                                </div>
                            ))}
                            <button type="button" onClick={addStillcutUrlInput} className={styles.addButton}>
                                URL 추가
                            </button>
                        </div>
                    )}
                    <button type="button" onClick={handleStillcutUpload} className={styles.uploadButton}>
                        스틸컷 업로드
                    </button>
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