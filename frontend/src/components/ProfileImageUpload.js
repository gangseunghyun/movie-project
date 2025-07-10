import React, { useState, useRef } from 'react';
import axios from 'axios';
import './ProfileImageUpload.css';

const ProfileImageUpload = ({ currentImageUrl, onImageUpdate, onSuccess }) => {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');
  const [previewUrl, setPreviewUrl] = useState(null);
  const fileInputRef = useRef(null);

  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // 파일 유효성 검사
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
    
    // 미리보기 생성
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
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (response.data.success) {
        onImageUpdate(response.data.imageUrl);
        setPreviewUrl(null);
        fileInputRef.current.value = '';
        alert('프로필 이미지가 업로드되었습니다.');
        if (onSuccess) onSuccess(); // 업로드 성공 시 모달 닫기
      } else {
        setError(response.data.message || '업로드에 실패했습니다.');
      }
    } catch (err) {
      console.error('프로필 이미지 업로드 오류:', err);
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

    if (!window.confirm('프로필 이미지를 삭제하시겠습니까?')) {
      return;
    }

    setIsUploading(true);
    setError('');

    try {
      const response = await axios.delete('http://localhost:80/api/profile/delete-image', {
        withCredentials: true,
      });

      if (response.data.success) {
        onImageUpdate(null);
        alert('프로필 이미지가 삭제되었습니다.');
        if (onSuccess) onSuccess(); // 삭제 성공 시 모달 닫기
      } else {
        setError(response.data.message || '삭제에 실패했습니다.');
      }
    } catch (err) {
      console.error('프로필 이미지 삭제 오류:', err);
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
    <div className="profile-image-upload">
      <h3>프로필 이미지</h3>
      
      <div className="image-preview">
        <img 
          src={previewUrl || getImageUrl(currentImageUrl) || '/placeholder-actor.png'} 
          alt="프로필 이미지" 
          className="profile-image"
        />
      </div>

      <div className="upload-controls">
        <div className="file-input-wrapper">
          <input
            type="file"
            ref={fileInputRef}
            accept="image/*"
            onChange={handleFileSelect}
            className="file-input"
            style={{ display: 'block' }} // 항상 보이게
          />
        </div>
        
        <div className="button-group">
          <button 
            onClick={handleUpload}
            disabled={isUploading || !previewUrl}
            className="upload-btn"
          >
            {isUploading ? '업로드 중...' : '업로드'}
          </button>
          
          {currentImageUrl && (
            <button 
              onClick={handleDelete}
              disabled={isUploading}
              className="delete-btn"
            >
              {isUploading ? '삭제 중...' : '삭제'}
            </button>
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      
      <div className="upload-info">
        <p>• 지원 형식: JPG, JPEG, PNG, GIF, WebP</p>
        <p>• 최대 파일 크기: 5MB</p>
      </div>
    </div>
  );
};

export default ProfileImageUpload; 