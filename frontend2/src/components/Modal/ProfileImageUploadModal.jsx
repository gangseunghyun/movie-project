import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';
import styles from './ProfileImageUploadModal.module.css';

const ProfileImageUploadModal = ({ currentImageUrl, onImageUpdate, onClose }) => {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');
  const [previewUrl, setPreviewUrl] = useState(null);
  const [selectedFileName, setSelectedFileName] = useState('');
  const fileInputRef = useRef(null);


  // 모달이 열릴 때마다 currentImageUrl을 previewUrl로 설정
  useEffect(() => {
    setPreviewUrl(currentImageUrl);
  }, [currentImageUrl]);
  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (!file) {
      setSelectedFileName('');
      return;
    }
    setSelectedFileName(file.name);
    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    if (!validTypes.includes(file.type)) {
      setError('지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 가능)');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('파일 크기는 5MB를 초과할 수 없습니다.');
      return;
    }
    setError('');
    const reader = new FileReader();
    reader.onload = (e) => {
      setPreviewUrl(e.target.result);
    };
    reader.readAsDataURL(file);
  };

  const handleUpload = async () => {
    if (!fileInputRef.current.files[0]) {
      setError('업로드할 파일을 선택해주세요.');
      return;
    }
    setIsUploading(true);
    setError('');
    const formData = new FormData();
    formData.append('image', fileInputRef.current.files[0]);
    try {
      const response = await axios.post('http://localhost:80/api/profile/upload-image', formData, {
        withCredentials: true,
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (response.data.success) {
        onImageUpdate(response.data.imageUrl);
        //setPreviewUrl(null);
        fileInputRef.current.value = '';
        //alert('프로필 이미지가 업로드되었습니다.');
        onClose && onClose();
      } else {
        setError(response.data.message || '업로드에 실패했습니다.');
      }
    } catch (err) {
      setError(err.response?.data?.message || '업로드 중 오류가 발생했습니다.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDelete = async () => {
    if (!currentImageUrl) {
      setError('삭제할 프로필 이미지가 없습니다.');
      return;
    }
    if (!window.confirm('프로필 이미지를 삭제하시겠습니까?')) return;
    setIsUploading(true);
    setError('');
    try {
      const response = await axios.delete('http://localhost:80/api/profile/delete-image', {
        withCredentials: true,
      });
      if (response.data.success) {
        onImageUpdate(null);
        //alert('프로필 이미지가 삭제되었습니다.');
        onClose && onClose();
      } else {
        setError(response.data.message || '삭제에 실패했습니다.');
      }
    } catch (err) {
      setError(err.response?.data?.message || '삭제 중 오류가 발생했습니다.');
    } finally {
      setIsUploading(false);
    }
  };

  const getImageUrl = (url) => {
    if (!url) return null;
    if (url.startsWith('http')) return url;
    return `http://localhost:80${url}`;
  };

  return (
    <div className={styles.profileImageUploadModalOverlay}>
      <div className={styles.profileImageUploadModalContent}>
        <div className={styles.profileImageUploadModalHeader}>
          <span>프로필 이미지 변경</span>
          <span className={styles.profileImageUploadModalClose} onClick={onClose}>&times;</span>
        </div>
        <div className={styles.imagePreview}>
          {previewUrl ? (
            <img
              src={previewUrl}
              alt="프로필 이미지 미리보기"
              className={styles.profileImage}
            />
          ) : currentImageUrl && selectedFileName ? (
            <img
              src={getImageUrl(currentImageUrl)}
              alt="프로필 이미지"
              className={styles.profileImage}
            />
          ) : (
            <img
              src={require('../../assets/user_icon.png')}
              alt="기본 프로필 이미지"
              className={styles.profileImage}
            />
          )}
        </div>
        <div className={styles.uploadControls}>
          <div className={styles.fileInputWrapper}>
            <input
              type="file"
              ref={fileInputRef}
              accept="image/*"
              onChange={handleFileSelect}
              className={styles.fileInput}
              id="profile-image-upload-input"
            />
            <label htmlFor="profile-image-upload-input" className={styles.customFileButton}>
              파일 선택
            </label>
            <span className={styles.selectedFileName}>
              {selectedFileName || '선택된 파일 없음'}
            </span>
          </div>
          <div className={styles.buttonGroup}>
            <button
              onClick={handleUpload}
              disabled={isUploading || !previewUrl}
              className={styles.uploadBtn}
            >
              {isUploading ? '업로드 중...' : '업로드'}
            </button>
            {currentImageUrl && (
              <button
                onClick={handleDelete}
                disabled={isUploading}
                className={styles.deleteBtn}
              >
                {isUploading ? '삭제 중...' : '삭제'}
              </button>
            )}
          </div>
        </div>
        {error && <div className={styles.errorMessage}>{error}</div>}
        <div className={styles.uploadInfo}>
          <p>• 지원 형식: JPG, JPEG, PNG, GIF, WebP</p>
          <p>• 최대 파일 크기: 5MB</p>
        </div>
      </div>
    </div>
  );
};

export default ProfileImageUploadModal; 