package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.Comment;
import com.movie.movie_backend.entity.CommentLike;
import com.movie.movie_backend.entity.Review;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.REVCommentRepository;
import com.movie.movie_backend.repository.REVCommentLikeRepository;
import com.movie.movie_backend.repository.REVReviewRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class REVCommentService {

    private final REVCommentRepository commentRepository;
    private final REVCommentLikeRepository commentLikeRepository;
    private final REVReviewRepository reviewRepository;
    private final USRUserRepository userRepository;

    /**
     * 댓글 작성
     */
    @Transactional
    public Comment createComment(Long reviewId, Long userId, String content, Long parentId) {
        log.info("댓글 작성: 리뷰ID={}, 사용자={}, 부모댓글ID={}", reviewId, userId, parentId);

        // 사용자와 리뷰 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));

        // 부모 댓글 조회 (대댓글인 경우)
        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다: " + parentId));
        }

        // 댓글 생성
        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .review(review)
                .parent(parent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Comment.CommentStatus.ACTIVE)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("댓글 작성 완료: ID={}, 타입={}", savedComment.getId(), 
                savedComment.isReply() ? "대댓글" : "댓글");

        return savedComment;
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public Comment updateComment(Long commentId, Long userId, String content) {
        log.info("댓글 수정: 댓글ID={}, 사용자={}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다: " + commentId));

        // 작성자 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("댓글을 수정할 권한이 없습니다.");
        }

        // 댓글 수정
        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        log.info("댓글 수정 완료: ID={}", updatedComment.getId());

        return updatedComment;
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info("댓글 삭제: 댓글ID={}, 사용자={}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다: " + commentId));

        // 작성자 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("댓글을 삭제할 권한이 없습니다.");
        }

        // 소프트 삭제
        comment.setStatus(Comment.CommentStatus.DELETED);
        commentRepository.save(comment);
        log.info("댓글 삭제 완료: ID={}", commentId);
    }

    /**
     * 리뷰의 최상위 댓글 조회
     */
    public List<Comment> getTopLevelCommentsByReviewId(Long reviewId) {
        return commentRepository.findByReviewIdAndParentIsNullOrderByCreatedAtDesc(reviewId);
    }

    /**
     * 리뷰의 모든 댓글 조회
     */
    public List<Comment> getAllCommentsByReviewId(Long reviewId) {
        return commentRepository.findByReviewIdOrderByCreatedAtDesc(reviewId);
    }

    /**
     * 특정 댓글의 대댓글 조회
     */
    public List<Comment> getRepliesByParentId(Long parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    /**
     * 사용자의 댓글 조회
     */
    public List<Comment> getCommentsByUserId(Long userId) {
        return commentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 댓글 개수 조회
     */
    public Long getCommentCountByReviewId(Long reviewId) {
        return commentRepository.getCommentCountByReviewId(reviewId);
    }

    /**
     * 대댓글 개수 조회
     */
    public Long getReplyCountByParentId(Long parentId) {
        return commentRepository.getReplyCountByParentId(parentId);
    }

    /**
     * 댓글 좋아요 추가
     */
    @Transactional
    public CommentLike addCommentLike(Long commentId, Long userId) {
        log.info("댓글 좋아요 추가: 댓글ID={}, 사용자={}", commentId, userId);

        // 이미 좋아요를 눌렀는지 확인
        Optional<CommentLike> existingLike = commentLikeRepository.findByUserIdAndCommentId(userId, commentId);
        if (existingLike.isPresent()) {
            throw new RuntimeException("이미 좋아요를 눌렀습니다.");
        }

        // 사용자와 댓글 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다: " + commentId));

        // 좋아요 생성
        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        CommentLike savedLike = commentLikeRepository.save(commentLike);
        log.info("댓글 좋아요 추가 완료: ID={}", savedLike.getId());

        return savedLike;
    }

    /**
     * 댓글 좋아요 취소
     */
    @Transactional
    public void removeCommentLike(Long commentId, Long userId) {
        log.info("댓글 좋아요 취소: 댓글ID={}, 사용자={}", commentId, userId);

        CommentLike commentLike = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new RuntimeException("좋아요를 찾을 수 없습니다."));

        commentLikeRepository.delete(commentLike);
        log.info("댓글 좋아요 취소 완료");
    }

    /**
     * 댓글 좋아요 개수 조회
     */
    public Long getCommentLikeCount(Long commentId) {
        return commentLikeRepository.getLikeCountByCommentId(commentId);
    }

    /**
     * 사용자가 댓글에 좋아요를 눌렀는지 확인
     */
    public boolean hasUserLikedComment(Long commentId, Long userId) {
        return commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
    }
} 
