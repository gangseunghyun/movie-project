import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUser } from '../../contexts/UserContext';
import styles from './MovieRegisterPage.module.css';

const SERVER_URL = "https://ec2-13-222-249-145.compute-1.amazonaws.com";

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
        directorName: '',
        actorNames: '', // 입력용 문자열
        tags: '',
        nationNm: '',
        prdtYear: '',
        prdtStatNm: '',
        typeNm: ''
    });
    
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    
    // 2단계 등록 관련 상태
    const [registrationStep, setRegistrationStep] = useState(1); // 1: 기본정보, 2: 이미지업로드
    const [savedMovieCd, setSavedMovieCd] = useState(null);
    
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
            if (registrationStep === 1) {
                // 1단계: 기본 정보 저장
                console.log('1단계: 기본 정보 저장 시작');
                console.log('전송할 데이터:', movieData);
                
                // 태그 문자열을 Tag 객체 배열로 변환
                const tagsArray = movieData.tags ? 
                    movieData.tags.split(',').map(tag => tag.trim()).filter(tag => tag !== '').map(tagName => ({
                        name: tagName
                    })) : 
                    [];
                
                console.log('변환 전 movieData:', movieData);
                
                // 전송할 데이터 준비 (숫자 필드 변환)
                const requestData = {
                    ...movieData,
                    showTm: parseInt(movieData.showTm) || 0, // 상영시간을 숫자로 변환
                    prdtYear: movieData.prdtYear ? movieData.prdtYear.toString() : '', // 제작연도는 문자열로 유지
                    tags: tagsArray, // Tag 객체 배열로 전송
                    stillcutUrls: stillcutUrls // 스틸컷 URL 배열 추가
                };
                
                console.log('변환된 requestData:', requestData);
                console.log('actorNames:', movieData.actorNames);
                console.log('directorName:', movieData.directorName);
                
                const response = await fetch(`/api/admin/movies`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify(requestData)
                });

                console.log('API 응답 상태:', response.status);
                const data = await response.json();
                console.log('API 응답 데이터:', data);

                if (response.ok && data.movieCd) {
                    setSavedMovieCd(data.movieCd);
                    setRegistrationStep(2);
                    alert('기본 정보가 저장되었습니다. 이제 이미지를 업로드해주세요.');
                    
                    // 페이지 상단으로 스크롤
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                } else {
                    console.error('API 오류:', data);
                    setError(data.message || '영화 등록에 실패했습니다.');
                    
                    // 오류 발생 시에도 상단으로 스크롤하여 오류 메시지 확인
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                }
            } else {
                // 2단계: 이미지 업로드 완료 후 최종 저장
                console.log('2단계: 이미지 업로드 완료, 최종 저장');
                alert('영화 등록이 완료되었습니다!');
                navigate('/');
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

    // 포스터 파일 선택
    const handlePosterFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setPosterFile(file);
            setPosterPreview(URL.createObjectURL(file));
            setPosterUploadMethod('file');
            // URL 입력값 초기화
            setMovieData(prev => ({ ...prev, posterUrl: '' }));
        }
    };

    // 포스터 URL 입력
    const handlePosterUrlChange = (e) => {
        const { value } = e.target;
        setMovieData(prev => ({ ...prev, posterUrl: value }));
        setPosterUploadMethod('url');
        // 파일 선택 초기화
        setPosterFile(null);
        setPosterPreview('');
    };

    // 포스터 파일 업로드
    const handlePosterUpload = async () => {
        if (!savedMovieCd) return;

        if (posterUploadMethod === 'file') {
            if (!posterFile) return;

            const formData = new FormData();
            formData.append('image', posterFile);

            try {
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/poster`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setPosterUrl(data.imageUrl);
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
            if (!movieData.posterUrl || !movieData.posterUrl.trim()) {
                alert('포스터 URL을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/poster-url`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify({ posterUrl: movieData.posterUrl })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setPosterUrl(data.imageUrl);
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

    // 스틸컷 URL 추가
    const addStillcutUrlInput = () => {
        setStillcutUrlInputs([...stillcutUrlInputs, '']);
    };

    // 스틸컷 URL 제거
    const removeStillcutUrlInput = (index) => {
        const newInputs = stillcutUrlInputs.filter((_, i) => i !== index);
        setStillcutUrlInputs(newInputs);
    };

    // 스틸컷 업로드
    const handleStillcutUpload = async () => {
        if (!savedMovieCd) return;

        if (stillcutUploadMethod === 'file') {
            if (!stillcutFiles.length) return;

            const formData = new FormData();
            stillcutFiles.forEach(f => formData.append('images', f));

            try {
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/stillcuts`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setStillcutUrls(prev => [...prev, ...data.imageUrls]); // 기존 스틸컷에 새 스틸컷 추가
                    setStillcutFiles([]); // 파일 선택 초기화
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
            const validUrls = stillcutUrlInputs.filter(url => url && url.trim() !== '');
            if (validUrls.length === 0) {
                alert('스틸컷 URL을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/stillcut-urls`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify({ imageUrls: validUrls })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setStillcutUrls(prev => [...prev, ...data.imageUrls]); // 기존 스틸컷에 새 스틸컷 추가
                    setStillcutUrlInputs(['']);
                    alert('스틸컷 URL이 추가되었습니다!');
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
        if (!savedMovieCd) return;

        if (directorImageUploadMethod === 'file') {
            if (!directorImageFile) return;

            const formData = new FormData();
            formData.append('image', directorImageFile);

            try {
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/director-image`, {
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
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/director-image-url`, {
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
                    setDirectorImageUrlInput('');
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

    // 배우 이미지 파일 선택 (여러 명)
    const handleActorImageChange = (e) => {
        const files = Array.from(e.target.files);
        setActorImageFiles(files);
        setActorImagePreviews(files.map(f => URL.createObjectURL(f)));
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
        setActorImagePreviews([]);
    };

    // 배우 이미지 URL 추가
    const addActorImageUrlInput = () => {
        setActorImageUrlInputs([...actorImageUrlInputs, '']);
    };

    // 배우 이미지 URL 제거
    const removeActorImageUrlInput = (index) => {
        const newInputs = actorImageUrlInputs.filter((_, i) => i !== index);
        setActorImageUrlInputs(newInputs);
    };

    // 배우 이미지 업로드 (여러 명)
    const handleActorImageUpload = async () => {
        if (!savedMovieCd) return;

        if (actorImageUploadMethod === 'file') {
            if (!actorImageFiles.length) return;

            const formData = new FormData();
            actorImageFiles.forEach(f => formData.append('images', f));

            try {
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/actor-images`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setActorImageUrls(data.imageUrls);
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
            const validUrls = actorImageUrlInputs.filter(url => url && url.trim() !== '');
            if (validUrls.length === 0) {
                alert('배우 이미지 URL을 입력해주세요.');
                return;
            }

            try {
                const response = await fetch(`/api/admin/movies/${savedMovieCd}/actor-image-urls`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    credentials: 'include',
                    body: JSON.stringify({ imageUrls: validUrls })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    setActorImageUrls([...actorImageUrls, ...data.imageUrls]);
                    setActorImageUrlInputs(['']);
                    alert('배우 이미지 URL이 추가되었습니다!');
                } else {
                    alert('배우 이미지 URL 설정 실패: ' + (data.message || '알 수 없는 오류'));
                }
            } catch (error) {
                console.error('배우 이미지 URL 설정 실패:', error);
                alert('배우 이미지 URL 설정 중 오류가 발생했습니다.');
            }
        }
    };

    const getImageUrl = (url) => {
      if (!url || url.trim() === '') return '';
      if (url.startsWith('http')) return url;
      return url; // 상대 경로는 그대로 사용 (백엔드에서 정적 리소스로 제공)
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
                {registrationStep === 1 ? (
                    // 1단계: 기본 정보 입력
                    <>
                        <div className={styles.stepIndicator}>
                            <h2>1단계: 영화 기본 정보</h2>
                        </div>

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

                        <div className={styles.formGroup}>
                            <label htmlFor="description">줄거리</label>
                            <textarea
                                id="description"
                                name="description"
                                value={movieData.description}
                                onChange={handleInputChange}
                                rows="4"
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label htmlFor="directorName">감독</label>
                            <input
                                type="text"
                                id="directorName"
                                name="directorName"
                                value={movieData.directorName}
                                onChange={handleInputChange}
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label htmlFor="actorNames">배우 (쉼표로 구분)</label>
                            <input
                                type="text"
                                id="actorNames"
                                name="actorNames"
                                value={movieData.actorNames}
                                onChange={handleInputChange}
                            />
                        </div>

                        <div className={styles.formGroup}>
                            <label htmlFor="tags">태그 (쉼표로 구분)</label>
                            <input
                                type="text"
                                id="tags"
                                name="tags"
                                value={movieData.tags}
                                onChange={handleInputChange}
                            />
                        </div>

                        <div className={styles.formRow}>
                            <div className={styles.formGroup}>
                                <label htmlFor="companyNm">배급사</label>
                                <input
                                    type="text"
                                    id="companyNm"
                                    name="companyNm"
                                    value={movieData.companyNm}
                                    onChange={handleInputChange}
                                />
                            </div>

                            <div className={styles.formGroup}>
                                <label htmlFor="openDt">개봉일</label>
                                <input
                                    type="date"
                                    id="openDt"
                                    name="openDt"
                                    value={movieData.openDt}
                                    onChange={handleInputChange}
                                />
                            </div>
                        </div>

                        <div className={styles.formRow}>
                            <div className={styles.formGroup}>
                                <label htmlFor="showTm">상영시간 (분)</label>
                                <input
                                    type="number"
                                    id="showTm"
                                    name="showTm"
                                    value={movieData.showTm}
                                    onChange={handleInputChange}
                                />
                            </div>

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
                        </div>

                        <div className={styles.formRow}>
                            <div className={styles.formGroup}>
                                <label htmlFor="nationNm">제작국가</label>
                                <input
                                    type="text"
                                    id="nationNm"
                                    name="nationNm"
                                    value={movieData.nationNm}
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

                        <div className={styles.formRow}>
                            <div className={styles.formGroup}>
                                <label htmlFor="prdtYear">제작연도</label>
                                <input
                                    type="text"
                                    id="prdtYear"
                                    name="prdtYear"
                                    value={movieData.prdtYear}
                                    onChange={handleInputChange}
                                />
                            </div>

                            <div className={styles.formGroup}>
                                <label htmlFor="prdtStatNm">제작상태</label>
                                <input
                                    type="text"
                                    id="prdtStatNm"
                                    name="prdtStatNm"
                                    value={movieData.prdtStatNm}
                                    onChange={handleInputChange}
                                />
                            </div>
                        </div>

                        <div className={styles.formGroup}>
                            <label htmlFor="typeNm">영화유형</label>
                            <input
                                type="text"
                                id="typeNm"
                                name="typeNm"
                                value={movieData.typeNm}
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
                                {loading ? '저장 중...' : '기본 정보 저장'}
                            </button>
                        </div>
                    </>
                ) : (
                    // 2단계: 이미지 업로드
                    <>
                        <div className={styles.stepIndicator}>
                            <h2>2단계: 이미지 업로드</h2>
                            <p>영화: {movieData.movieNm}</p>
                        </div>

                        {/* 포스터 업로드 */}
                        <div className={styles.formGroup}>
                            <label>포스터 이미지</label>
                            
                            {/* 업로드 방식 선택 */}
                            <div className={styles.uploadMethodSelector}>
                                <label>
                                    <input
                                        type="radio"
                                        name="posterMethod"
                                        value="file"
                                        checked={posterUploadMethod === 'file'}
                                        onChange={() => setPosterUploadMethod('file')}
                                    />
                                    파일 업로드
                                </label>
                                <label>
                                    <input
                                        type="radio"
                                        name="posterMethod"
                                        value="url"
                                        checked={posterUploadMethod === 'url'}
                                        onChange={() => setPosterUploadMethod('url')}
                                    />
                                    URL 입력
                                </label>
                            </div>

                            {/* 파일 업로드 방식 */}
                            {posterUploadMethod === 'file' && (
                                <div>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={handlePosterFileChange}
                                    />
                                    {posterPreview && (
                                        <div className={styles.previewContainer}>
                                            <img 
                                                src={posterPreview} 
                                                alt="포스터 미리보기" 
                                                className={styles.posterPreview}
                                            />
                                            <button 
                                                type="button" 
                                                onClick={handlePosterUpload}
                                                disabled={!posterFile}
                                                className={styles.uploadButton}
                                            >
                                                포스터 업로드
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* URL 입력 방식 */}
                            {posterUploadMethod === 'url' && (
                                <div>
                                    <input
                                        type="url"
                                        value={movieData.posterUrl}
                                        onChange={handlePosterUrlChange}
                                        placeholder="https://example.com/poster.jpg"
                                    />
                                    {movieData.posterUrl && (
                                        <div className={styles.previewContainer}>
                                            <img 
                                                src={getImageUrl(movieData.posterUrl)} 
                                                alt="포스터 미리보기" 
                                                className={styles.posterPreview}
                                                onError={(e) => {
                                                    e.target.style.display = 'none';
                                                    alert('이미지를 불러올 수 없습니다. URL을 확인해주세요.');
                                                }}
                                            />
                                        </div>
                                    )}
                                    <button 
                                        type="button" 
                                        onClick={handlePosterUpload}
                                        disabled={!movieData.posterUrl || !movieData.posterUrl.trim()}
                                        className={styles.uploadButton}
                                    >
                                        포스터 URL 설정
                                    </button>
                                </div>
                            )}

                            {posterUrl && <div className={styles.successMessage}>✓ 포스터 설정 완료!</div>}
                        </div>

                        {/* 스틸컷 업로드 */}
                        <div className={styles.formGroup}>
                            <label>스틸컷 이미지</label>
                            
                            {/* 업로드 방식 선택 */}
                            <div className={styles.uploadMethodSelector}>
                                <label>
                                    <input
                                        type="radio"
                                        name="stillcutMethod"
                                        value="file"
                                        checked={stillcutUploadMethod === 'file'}
                                        onChange={() => setStillcutUploadMethod('file')}
                                    />
                                    파일 업로드
                                </label>
                                <label>
                                    <input
                                        type="radio"
                                        name="stillcutMethod"
                                        value="url"
                                        checked={stillcutUploadMethod === 'url'}
                                        onChange={() => setStillcutUploadMethod('url')}
                                    />
                                    URL 입력
                                </label>
                            </div>

                            {/* 파일 업로드 방식 */}
                            {stillcutUploadMethod === 'file' && (
                                <div>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        multiple
                                        onChange={handleStillcutChange}
                                    />
                                    {stillcutFiles.length > 0 && (
                                        <div className={styles.previewContainer}>
                                            {Array.from(stillcutFiles).map((file, idx) => (
                                                <img 
                                                    key={idx} 
                                                    src={URL.createObjectURL(file)} 
                                                    alt="스틸컷 미리보기" 
                                                    className={styles.stillcutPreview} 
                                                />
                                            ))}
                                        </div>
                                    )}
                                    <button 
                                        type="button" 
                                        onClick={handleStillcutUpload}
                                        disabled={!stillcutFiles.length}
                                        className={styles.uploadButton}
                                    >
                                        스틸컷 업로드
                                    </button>
                                </div>
                            )}

                            {/* URL 입력 방식 */}
                            {stillcutUploadMethod === 'url' && (
                                <div>
                                    {stillcutUrlInputs.map((url, index) => (
                                        <div key={index} className={styles.urlInputGroup}>
                                            <input
                                                type="url"
                                                value={url}
                                                onChange={(e) => handleStillcutUrlChange(index, e.target.value)}
                                                placeholder="https://example.com/stillcut.jpg"
                                            />
                                            <button 
                                                type="button" 
                                                onClick={() => removeStillcutUrlInput(index)}
                                                className={styles.removeButton}
                                            >
                                                삭제
                                            </button>
                                        </div>
                                    ))}
                                    <button 
                                        type="button" 
                                        onClick={addStillcutUrlInput}
                                        className={styles.addButton}
                                    >
                                        URL 추가
                                    </button>
                                    <button 
                                        type="button" 
                                        onClick={handleStillcutUpload}
                                        disabled={!stillcutUrlInputs.some(url => url && url.trim() !== '')}
                                        className={styles.uploadButton}
                                    >
                                        스틸컷 URL 추가
                                    </button>
                                </div>
                            )}

                            {/* 업로드된 스틸컷 미리보기 */}
                            {stillcutUrls.length > 0 && (
                                <div className={styles.previewContainer}>
                                    <h4>업로드된 스틸컷:</h4>
                                    {stillcutUrls.map((url, idx) => (
                                        <img key={idx} src={getImageUrl(url)} alt="스틸컷" className={styles.stillcutPreview} />
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* 감독 이미지 업로드 */}
                        <div className={styles.formGroup}>
                            <label>감독 이미지</label>
                            
                            {/* 업로드 방식 선택 */}
                            <div className={styles.uploadMethodSelector}>
                                <label>
                                    <input
                                        type="radio"
                                        name="directorMethod"
                                        value="file"
                                        checked={directorImageUploadMethod === 'file'}
                                        onChange={() => setDirectorImageUploadMethod('file')}
                                    />
                                    파일 업로드
                                </label>
                                <label>
                                    <input
                                        type="radio"
                                        name="directorMethod"
                                        value="url"
                                        checked={directorImageUploadMethod === 'url'}
                                        onChange={() => setDirectorImageUploadMethod('url')}
                                    />
                                    URL 입력
                                </label>
                            </div>

                            {/* 파일 업로드 방식 */}
                            {directorImageUploadMethod === 'file' && (
                                <div>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={handleDirectorImageChange}
                                    />
                                    {directorImagePreview && (
                                        <div className={styles.previewContainer}>
                                            <img 
                                                src={directorImagePreview} 
                                                alt="감독 미리보기" 
                                                className={styles.directorPreview}
                                            />
                                            <button 
                                                type="button" 
                                                onClick={handleDirectorImageUpload}
                                                disabled={!directorImageFile}
                                                className={styles.uploadButton}
                                            >
                                                감독 이미지 업로드
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* URL 입력 방식 */}
                            {directorImageUploadMethod === 'url' && (
                                <div>
                                    <input
                                        type="url"
                                        value={directorImageUrlInput}
                                        onChange={handleDirectorImageUrlChange}
                                        placeholder="https://example.com/director.jpg"
                                    />
                                    {directorImageUrlInput && (
                                        <div className={styles.previewContainer}>
                                            <img 
                                                src={getImageUrl(directorImageUrlInput)} 
                                                alt="감독 미리보기" 
                                                className={styles.directorPreview}
                                                onError={(e) => {
                                                    e.target.style.display = 'none';
                                                    alert('이미지를 불러올 수 없습니다. URL을 확인해주세요.');
                                                }}
                                            />
                                        </div>
                                    )}
                                    <button 
                                        type="button" 
                                        onClick={handleDirectorImageUpload}
                                        disabled={!directorImageUrlInput || !directorImageUrlInput.trim()}
                                        className={styles.uploadButton}
                                    >
                                        감독 이미지 URL 설정
                                    </button>
                                </div>
                            )}

                            {directorImageUrl && <div className={styles.successMessage}>✓ 감독 이미지 설정 완료!</div>}
                        </div>

                        {/* 배우 이미지 업로드 */}
                        <div className={styles.formGroup}>
                            <label>배우 이미지 (여러 명)</label>
                            
                            {/* 업로드 방식 선택 */}
                            <div className={styles.uploadMethodSelector}>
                                <label>
                                    <input
                                        type="radio"
                                        name="actorMethod"
                                        value="file"
                                        checked={actorImageUploadMethod === 'file'}
                                        onChange={() => setActorImageUploadMethod('file')}
                                    />
                                    파일 업로드
                                </label>
                                <label>
                                    <input
                                        type="radio"
                                        name="actorMethod"
                                        value="url"
                                        checked={actorImageUploadMethod === 'url'}
                                        onChange={() => setActorImageUploadMethod('url')}
                                    />
                                    URL 입력
                                </label>
                            </div>

                            {/* 파일 업로드 방식 */}
                            {actorImageUploadMethod === 'file' && (
                                <div>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        multiple
                                        onChange={handleActorImageChange}
                                    />
                                    {actorImagePreviews.length > 0 && (
                                        <div className={styles.previewContainer}>
                                            {actorImagePreviews.map((preview, idx) => (
                                                <img 
                                                    key={idx} 
                                                    src={preview} 
                                                    alt={`배우${idx+1} 미리보기`} 
                                                    className={styles.actorPreview}
                                                />
                                            ))}
                                            <button 
                                                type="button" 
                                                onClick={handleActorImageUpload}
                                                disabled={!actorImageFiles.length}
                                                className={styles.uploadButton}
                                            >
                                                배우 이미지 업로드
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* URL 입력 방식 */}
                            {actorImageUploadMethod === 'url' && (
                                <div>
                                    {actorImageUrlInputs.map((url, index) => (
                                        <div key={index} className={styles.urlInputGroup}>
                                            <input
                                                type="url"
                                                value={url}
                                                onChange={(e) => handleActorImageUrlChange(index, e.target.value)}
                                                placeholder="https://example.com/actor.jpg"
                                            />
                                            <button 
                                                type="button" 
                                                onClick={() => removeActorImageUrlInput(index)}
                                                className={styles.removeButton}
                                            >
                                                삭제
                                            </button>
                                        </div>
                                    ))}
                                    <button 
                                        type="button" 
                                        onClick={addActorImageUrlInput}
                                        className={styles.addButton}
                                    >
                                        URL 추가
                                    </button>
                                    <button 
                                        type="button" 
                                        onClick={handleActorImageUpload}
                                        disabled={!actorImageUrlInputs.some(url => url && url.trim() !== '')}
                                        className={styles.uploadButton}
                                    >
                                        배우 이미지 URL 추가
                                    </button>
                                </div>
                            )}

                            {/* 업로드된 배우 이미지 미리보기 */}
                            {actorImageUrls.length > 0 && (
                                <div className={styles.previewContainer}>
                                    <h4>업로드된 배우 이미지:</h4>
                                    {actorImageUrls.map((url, idx) => (
                                        <img key={idx} src={getImageUrl(url)} alt="배우" className={styles.actorPreview} />
                                    ))}
                                </div>
                            )}
                        </div>

                        <div className={styles.buttonGroup}>
                            <button
                                type="button"
                                onClick={() => {
                                    setRegistrationStep(1);
                                    setSavedMovieCd(null);
                                }}
                                className={styles.cancelButton}
                            >
                                이전 단계
                            </button>
                            <button
                                type="submit"
                                className={styles.submitButton}
                                disabled={loading}
                            >
                                {loading ? '완료 중...' : '영화 등록 완료'}
                            </button>
                        </div>
                    </>
                )}
            </form>
        </div>
    );
};

export default MovieRegisterPage; 