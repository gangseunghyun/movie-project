import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './MovieDetailHeader.module.css';
import { useUser } from '../../contexts/UserContext';


// 아이콘 경로
import plusIcon from '../../assets/plus_icon.png';
import commentIcon from '../../assets/comment_icon.png';
import shareIcon from '../../assets/share_icon.png';
import moreIcon from '../../assets/more_icon.png';
import plusIcon_hover from '../../assets/plus_icon_hover.png';
import commentIcon_hover from '../../assets/comment_icon_hover.png';
import shareIcon_hover from '../../assets/share_icon_hover.png';
import likeIconTrue from '../../assets/like_icon_true.png';
import posterImg from '../../assets/banner1.jpg'; // 임시 포스터 이미지
import starFull from '../../assets/star_full.svg';
import starHalf from '../../assets/star_half.svg';
import starEmpty from '../../assets/star_empty.svg';
import ReviewModal from '../Modal/ReviewModal';
import { Bar, Line } from 'react-chartjs-2';
import {
    Chart as ChartJS,
    LineElement,
    BarElement,
    PointElement,
    CategoryScale,
    LinearScale,
    Tooltip,
    Legend,
} from 'chart.js';

ChartJS.register(LineElement, BarElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend);


const MovieDetailHeader = ({ movieDetail, onCommentSaved, onRefreshMovieDetail }) => {
    const { user } = useUser();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showMore, setShowMore] = useState(false);
    const [userRating, setUserRating] = useState(0);
    const [hoverRating, setHoverRating] = useState(0);
    const [plusHover, setPlusHover] = useState(false);
    const [commentHover, setCommentHover] = useState(false);
    const [shareHover, setShareHover] = useState(false);
    const [isOverflow, setIsOverflow] = useState(false);
    const [commentModalOpen, setCommentModalOpen] = useState(false);
    const [ratingLoading, setRatingLoading] = useState(false);
    const [hasLoadedRating, setHasLoadedRating] = useState(false);
    const [ratingDist, setRatingDist] = useState(null);
    const [isLiked, setIsLiked] = useState(false);
    const [likeLoading, setLikeLoading] = useState(false);
    const summaryRef = useRef(null);

    useEffect(() => {
        if (summaryRef.current) {
            // 실제로 3줄을 넘는지 체크
            const el = summaryRef.current;
            setIsOverflow(el.scrollHeight > el.clientHeight + 1); // 약간의 오차 허용
        }
    }, [movieDetail?.description]);

    // 찜 상태 설정 (movieDetail에서 받아온 likedByMe 사용)
    useEffect(() => {

        if (movieDetail?.likedByMe !== undefined) {
            setIsLiked(movieDetail.likedByMe);

        }
    }, [movieDetail?.likedByMe]);

    // movieDetail이 변경될 때도 찜 상태 업데이트
    useEffect(() => {
        if (movieDetail?.likedByMe !== undefined) {
            setIsLiked(movieDetail.likedByMe);

        }
    }, [movieDetail]);

    // 사용자의 기존 별점 조회
    useEffect(() => {
        const fetchUserRating = async () => {
            if (!movieDetail?.movieCd || hasLoadedRating) return;

            try {
                const response = await fetch(`http://localhost:80/api/ratings/${movieDetail.movieCd}`, {
                    credentials: 'include',
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.success && data.data) {
                        setUserRating(data.data.score);
                    } else {
                        // 별점이 없는 경우 0으로 설정
                        setUserRating(0);
                    }
                } else {
                    // 응답이 실패한 경우 0으로 설정
                    setUserRating(0);
                }
            } catch (error) {
                console.error('사용자 별점 조회 실패:', error);
                setUserRating(0);
            } finally {
                setHasLoadedRating(true);
            }
        };

        fetchUserRating();
    }, [movieDetail?.movieCd, hasLoadedRating]);

    // 영화가 변경될 때 별점 조회 상태 초기화
    useEffect(() => {
        setHasLoadedRating(false);
        setUserRating(0);
        setIsLiked(false);
    }, [movieDetail?.movieCd]);

    // 별점 분포도 불러오기
    useEffect(() => {
        if (!movieDetail?.movieCd) return;
        fetch(`http://localhost:80/api/ratings/movie/${movieDetail.movieCd}/distribution`, {
            credentials: 'include'
        })
            .then(res => res.json())
            .then(data => {
                if (data.success && data.data?.distribution) {
                    //console.log(data.data.distribution);
                    setRatingDist(data.data.distribution);
                } else {
                    setRatingDist(null);
                }
            })
            .catch(() => setRatingDist(null));
    }, [movieDetail?.movieCd]);

    // ratingSteps, chartLabels, chartData를 0.5~5.0 오름차순으로 변경
    const ratingSteps = ["0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0"];

    // 최다득표 점수 계산
    const maxCount = ratingDist ? Math.max(...ratingSteps.map(star => ratingDist[star] || 0)) : 0;
    const maxStar = ratingDist ? ratingSteps.find(star => (ratingDist[star] || 0) === maxCount) : null;

    // chart.js용 데이터 및 옵션
    const chartLabels = ratingSteps.map(star => star.replace('.0', ''));
    const chartData = ratingSteps.map(star => ratingDist ? (ratingDist[star] || 0) : 0);

    const data = {
        labels: chartLabels,
        datasets: [
            {
                label: '별점 분포',
                data: chartData,
                fill: false,
                borderColor: '#ff3366',
                backgroundColor: '#ff3366',
                tension: 0.3,
                pointRadius: 4,
                pointBackgroundColor: '#ff3366',
            },
        ],
    };

    const options = {
        responsive: true,
        plugins: {
            legend: { display: false },
            tooltip: { enabled: true },
        },
        scales: {
            x: {
                title: { display: true, text: '별점 그래프' },
                grid: { display: false },
            },
            y: {
                title: { display: true, text: '인원수' },
                beginAtZero: true,
                ticks: { stepSize: 1, precision: 0 },
            },
        },
    };

    // Bar 차트 데이터 및 옵션
    const barData = {
        labels: chartLabels,
        datasets: [
            {
                label: '별점 분포 (막대)',
                data: chartData,
                backgroundColor: '#ffc107',
                borderColor: undefined, // 테두리 제거
                borderWidth: 0,
                borderRadius: 4,
                barPercentage: 0.7,
                categoryPercentage: 0.7,
            },
        ],
    };

    // x축 제목 동적 생성
    const averageRating = movieDetail?.averageRating && !isNaN(Number(movieDetail.averageRating)) ? Number(movieDetail.averageRating).toFixed(1) : '-';
    // 평가자 수: ratingDist 값들의 합
    const totalRaters = ratingDist ? Object.values(ratingDist).reduce((sum, v) => sum + (v || 0), 0) : 0;
    const xAxisTitle = `평균 ★${averageRating}  (${totalRaters}명)`;

    const barOptions = {
        responsive: true,
        plugins: {
            legend: { display: false },
            tooltip: {
                enabled: true,
                bodyColor: '#cecece', // 툴팁 본문 색상
                titleColor: '#cecece', // 툴팁 제목 색상
            },
        },
        scales: {
            x: {
                title: {
                    display: true,
                    text: xAxisTitle,
                    color: '#cecece',
                    font: { size: 14 }, // 글자 크기 키움
                },
                ticks: { color: '#cecece' }, // x축 라벨 색상
                grid: { display: false },
            },
            y: {
                title: { display: false },
                beginAtZero: true,
                ticks: { display: false },
                grid: { display: false },
            },
        },
    };

    const handleCommentClick = () => {
        if (!user) {
            alert('로그인 후 입력해주세요.');
            return;
        }
        if (userRating === 0) {
            alert('별점을 먼저 입력해주세요.');
            return;
        }
        setCommentModalOpen(true);
    };
    const handleCommentSave = (comment) => {
        // ReviewModal에서 이미 알림을 보여주므로 여기서는 제거
        setCommentModalOpen(false);
        if (onCommentSaved) onCommentSaved();
    };

    // 예매 버튼 클릭 핸들러
    const handleBookingClick = () => {
        navigate(`/booking/${movieDetail.movieCd}`, { state: { movieDetail } });
    };

    // 찜하기/취소 핸들러
    const handleLikeToggle = async () => {
        if (!user) {
            alert('로그인 후 찜하기를 이용할 수 있습니다.');
            return;
        }

        if (!movieDetail?.movieCd) {
            alert('영화 정보가 없습니다.');
            return;
        }

        const prevIsLiked = isLiked;

        // Optimistic update
        setIsLiked(!isLiked);
        setLikeLoading(true);

        try {
            const method = prevIsLiked ? 'DELETE' : 'POST';
            const response = await fetch(`http://localhost:80/api/movies/${movieDetail.movieCd}/like`, {
                method,
                credentials: 'include',
            });

            const data = await response.json();

            if (response.ok && data.success) {
                // 성공 시 상태 유지
               // alert(data.message || (prevIsLiked ? '찜이 취소되었습니다.' : '찜이 추가되었습니다.'));
            } else {
                // 실패 시 원래 상태로 되돌리기
                setIsLiked(prevIsLiked);
                alert(data.message || '찜 처리에 실패했습니다.');
            }
        } catch (error) {
            console.error('찜 처리 실패:', error);
            // 네트워크 오류 시에도 원래 상태로 되돌리기
            setIsLiked(prevIsLiked);
            alert('찜 처리 중 오류가 발생했습니다.');
        } finally {
            setLikeLoading(false);
        }
    };

    // 별점 저장 API 호출 함수
    const saveRating = async (score) => {
        if (!movieDetail?.movieCd) {
            alert('영화 정보가 없습니다.');
            return;
        }

        setRatingLoading(true);
        try {
            const response = await fetch('http://localhost:80/api/ratings', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    movieCd: movieDetail.movieCd,
                    score: score
                })
            });

            const data = await response.json();

            if (data.success) {
                // 별점 저장 성공 시 사용자 별점을 다시 조회하여 정확한 값 설정
                try {
                    const ratingResponse = await fetch(`http://localhost:80/api/ratings/${movieDetail.movieCd}`, {
                        credentials: 'include',
                    });

                    if (ratingResponse.ok) {
                        const ratingData = await ratingResponse.json();
                        if (ratingData.success && ratingData.data) {
                            setUserRating(ratingData.data.score);
                        } else {
                            setUserRating(score);
                        }
                    } else {
                        setUserRating(score);
                    }
                } catch (error) {
                    console.error('별점 재조회 실패:', error);
                    setUserRating(score);
                }

               // alert(data.message || '별점이 저장되었습니다.');
                // 부모 컴포넌트에 별점 저장 완료 알림
                if (onCommentSaved) onCommentSaved();
                // 영화 상세 정보 새로고침 (평균 별점 업데이트를 위해)
                if (onRefreshMovieDetail) onRefreshMovieDetail();
            } else {
                // 저장 실패 시 userRating을 원래대로 되돌리기
                setUserRating(0);
                alert(data.message || '별점 저장에 실패했습니다.');
            }
        } catch (error) {
            console.error('별점 저장 실패:', error);
            // 네트워크 오류 시에도 userRating을 원래대로 되돌리기
            setUserRating(0);
            alert('별점 저장 중 오류가 발생했습니다.');
        } finally {
            setRatingLoading(false);
        }
    };

    // 별점 클릭 핸들러
    const handleStarClick = (e, value) => {
        if (!user) {
            alert('로그인 후 별점을 등록할 수 있습니다.');
            return;
        }

        const rect = e.target.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const score = x < rect.width / 2 ? value - 0.5 : value;

        // 즉시 UI에 반영 (저장 중에는 이 값이 유지됨)
        setUserRating(score);
        saveRating(score);
    };

    // 영화 수정 핸들러
    const handleEditMovie = () => {
        if (!user || user.role !== 'ADMIN') {
            alert('관리자만 수정할 수 있습니다.');
            return;
        }
        
        // 영화 수정 페이지로 이동 (현재 영화 정보 전달)
        navigate(`/admin/movie/edit/${movieDetail.movieCd}`, { 
            state: { 
                movieDetail: {
                    movieCd: movieDetail.movieCd,
                    movieNm: movieDetail.movieNm,
                    movieNmEn: movieDetail.movieNmEn || '',
                    showTm: movieDetail.showTm || '',
                    openDt: movieDetail.openDt || '',
                    genreNm: movieDetail.genreNm || '',
                    watchGradeNm: movieDetail.watchGradeNm || '',
                    companyNm: movieDetail.companyNm || '',
                    description: movieDetail.description || '',
                    posterUrl: movieDetail.posterUrl || ''
                },
                isEdit: true 
            } 
        });
    };

    // 영화 삭제 핸들러
    const handleDeleteMovie = () => {
        if (!user || user.role !== 'ADMIN') {
            alert('관리자만 삭제할 수 있습니다.');
            return;
        }

        const confirmDelete = window.confirm('정말로 이 영화를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.');
        
        if (confirmDelete) {
            console.log('영화 삭제 시작:', movieDetail.movieCd);
            
            // 영화 삭제 API 호출 (AdminController 사용)
            fetch(`http://localhost:80/api/admin/movies/${movieDetail.movieCd}`, {
                method: 'DELETE',
                credentials: 'include',
            })
            .then(response => {
                console.log('삭제 API 응답 상태:', response.status);
                return response.json();
            })
            .then(data => {
                console.log('삭제 API 응답 데이터:', data);
                
                if (data.success) {
                    alert('영화가 성공적으로 삭제되었습니다.');
                    navigate('/'); // 메인 페이지로 이동
                } else {
                    alert(data.message || '영화 삭제에 실패했습니다.');
                }
            })
            .catch(error => {
                console.error('영화 삭제 오류:', error);
                alert('영화 삭제 중 오류가 발생했습니다.');
            });
        }
    };

    return (
        <>
            <div className={styles.headerWrap}>
                <div className={styles.headerLeft}>
                    <h1 className={styles.title}>
                        {movieDetail.movieNm} <span className={styles.rating}>★ {movieDetail.averageRating && !isNaN(Number(movieDetail.averageRating)) ? Number(movieDetail.averageRating).toFixed(1) : '-'}</span>
                        {user && user.role === 'ADMIN' && (
                            <div className={styles.adminActions}>
                                <button className={styles.editBtn} onClick={() => handleEditMovie()}>
                                    수정
                                </button>
                                <button className={styles.deleteBtn} onClick={() => handleDeleteMovie()}>
                                    삭제
                                </button>
                            </div>
                        )}
                    </h1>
                    <div className={styles.info}>
                        <span>{movieDetail.openDt}</span> · <span>{movieDetail.genreNm}</span>
                    </div>
                    <div className={styles.meta}>
                        <span>{movieDetail.showTm}분</span> · <span>{movieDetail.watchGradeNm}</span>
                    </div>
                    <div className={styles.stats}>
                        <span>예매 순위 {movieDetail.rank || movieDetail.reservationRank}위</span><span>({movieDetail.reservationRate})</span> · <span>누적 관객 {movieDetail.audienceCount || movieDetail.totalAudience}</span>
                    </div>
                    <div className={styles.userRating}>
                        <span>평가하기</span>
                        {[...Array(5)].map((_, i) => {
                            const value = i + 1;
                            let starImg = starEmpty;

                            // 호버 중이면 호버 값, 아니면 실제 저장된 별점 사용
                            const currentRating = hoverRating || userRating;

                            if (currentRating >= value) {
                                starImg = starFull;
                            } else if (currentRating >= value - 0.5) {
                                starImg = starHalf;
                            }

                            return (
                                <img
                                    key={i}
                                    src={starImg}
                                    alt={`${value}점`}
                                    onClick={e => handleStarClick(e, value)}
                                    onMouseMove={e => {
                                        const rect = e.target.getBoundingClientRect();
                                        const x = e.clientX - rect.left;
                                        if (x < rect.width / 2) {
                                            setHoverRating(value - 0.5);
                                        } else {
                                            setHoverRating(value);
                                        }
                                    }}
                                    onMouseLeave={() => setHoverRating(0)}
                                    className={`${styles.starImg} ${ratingLoading ? styles.disabled : ''}`}
                                    role="button"
                                    aria-label={`${value}점 주기`}
                                    style={{ cursor: ratingLoading ? 'not-allowed' : 'pointer' }}
                                />
                            );
                        })}
                        {/* {ratingLoading && <span className={styles.loadingText}>저장 중...</span>}
                        {userRating > 0 && !ratingLoading && (
                            <span className={styles.ratingText}>내 별점: {userRating.toFixed(1)}점</span>
                        )} */}
                    </div>

                    <div className={styles.actions}>
                        <div className={styles.actionItem}>
                            <button
                                className={styles.iconBtn}
                                onMouseEnter={() => setPlusHover(true)}
                                onMouseLeave={() => setPlusHover(false)}
                                onClick={handleLikeToggle}
                                disabled={likeLoading}
                            >
                                <img src={isLiked ? likeIconTrue : (plusHover ? plusIcon_hover : plusIcon)} alt="찜하기" />
                            </button>
                            <div className={styles.actionLabel}>찜</div>
                        </div>
                        <div className={styles.actionItem}>
                            <button
                                className={styles.iconBtn}
                                onMouseEnter={() => setCommentHover(true)}
                                onMouseLeave={() => setCommentHover(false)}
                                onClick={handleCommentClick}
                            >
                                <img src={commentHover ? commentIcon_hover : commentIcon} alt="코멘트" />
                            </button>
                            <div className={styles.actionLabel}>코멘트</div>
                        </div>
                        <div className={styles.actionItem}>
                            <button
                                className={styles.iconBtn}
                                onMouseEnter={() => setShareHover(true)}
                                onMouseLeave={() => setShareHover(false)}
                            >
                                <img src={shareHover ? shareIcon_hover : shareIcon} alt="공유하기" />
                            </button>
                            <div className={styles.actionLabel}>공유</div>
                        </div>
                    </div>
                    <div className={styles.summaryWrap}>
                        {/* showMore가 false일 때만 미리보기(3줄) */}
                        {!showMore && (
                            <span
                                className={styles.summaryPreview}
                                ref={summaryRef}
                            >
                                {movieDetail.description}
                            </span>
                        )}
                        {/* 더보기 버튼: 3줄 초과 + showMore가 false일 때만 */}
                        {isOverflow && !showMore && (
                            <button className={styles.moreBtn} onClick={() => setShowMore(true)}>
                                더보기
                                <img src={moreIcon} alt="더보기 화살표" className={styles.moreIcon} />
                            </button>
                        )}
                        {/* showMore가 true일 때 전체 설명 */}
                        {showMore && (
                            <>
                                <span className={styles.summaryMore}>
                                    {movieDetail.description}
                                </span>
                                <button className={styles.closeBtn} onClick={() => setShowMore(false)}>
                                    닫기
                                    <img src={moreIcon} alt="닫기" className={styles.closeIcon} />
                                </button>
                            </>
                        )}
                    </div>
                </div>
                <div 
                    className={styles.headerMiddle}
                    style={{ 
                        top: showMore ? '125px' : '145px'
                    }}
                >
                    {ratingDist && (
                        <div className={styles.ratingDistWrap}>
                            <Bar data={barData} options={barOptions} />
                        </div>
                    )}
                </div>
                <div className={styles.headerRight}>
                    <img src={movieDetail.posterUrl || posterImg} alt="영화 포스터" className={styles.posterImg} />
                    <button
                        className={styles.bookingButton}
                        onClick={handleBookingClick}
                    >
                        예매하기
                    </button>
                </div>
            </div>
            <hr className={styles.detailDivider} />
            <ReviewModal
                open={commentModalOpen}
                onClose={() => setCommentModalOpen(false)}
                onSave={handleCommentSave}
                movieTitle={movieDetail.movieNm}
                userRating={userRating}
                movieCd={movieDetail.movieCd}
            />
        </>
    );
};

export default MovieDetailHeader; 