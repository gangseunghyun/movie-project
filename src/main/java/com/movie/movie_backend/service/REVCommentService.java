package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.CommentDto;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class REVCommentService {

    private final REVCommentRepository commentRepository;
    private final REVCommentLikeRepository commentLikeRepository;
    private final REVReviewRepository reviewRepository;
    private final USRUserRepository userRepository;
    private final ForbiddenWordService forbiddenWordService;

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

        // 욕설 감지
        boolean isBlocked = forbiddenWordService.containsForbiddenWords(content);

        // 댓글 생성
        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .review(review)
                .parent(parent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Comment.CommentStatus.ACTIVE)
                .isBlockedByCleanbot(isBlocked)
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

        // 욕설 감지
        boolean isBlocked = forbiddenWordService.containsForbiddenWords(content);

        // 댓글 수정
        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setBlockedByCleanbot(isBlocked);

        Comment updatedComment = commentRepository.save(comment);
        log.info("댓글 수정 완료: ID={}", updatedComment.getId());

        return updatedComment;
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     * 부모 댓글 삭제 시 대댓글도 함께 삭제
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

        // 재귀적으로 하위 댓글 모두 소프트 삭제
        softDeleteRecursively(comment);

        log.info("댓글 및 하위 댓글 삭제 완료: ID={}", commentId);
    }

    private void softDeleteRecursively(Comment comment) {
        comment.setStatus(Comment.CommentStatus.DELETED);
        commentRepository.save(comment);

        List<Comment> replies = commentRepository.findByParentIdAndStatusOrderByCreatedAtAsc(comment.getId(), Comment.CommentStatus.ACTIVE);
        for (Comment reply : replies) {
            softDeleteRecursively(reply);
        }
    }

    /**
     * 리뷰의 최상위 댓글 조회
     */
    public List<CommentDto> getTopLevelCommentsByReviewId(Long reviewId) {
        List<Comment> comments = commentRepository.findByReviewIdAndParentIsNullAndStatusOrderByCreatedAtDesc(reviewId, Comment.CommentStatus.ACTIVE);
        return comments.stream()
                .map(comment -> {
                    Long likeCount = getCommentLikeCount(comment.getId());
                    return CommentDto.fromEntityWithLikeInfo(comment, likeCount, false);
                })
                .collect(Collectors.toList());
    }

    /**
     * 리뷰의 최상위 댓글 조회 (사용자별 좋아요 상태 포함)
     */
    public List<CommentDto> getTopLevelCommentsByReviewIdWithUserLikeStatus(Long reviewId, Long userId) {
        List<Comment> comments = commentRepository.findByReviewIdAndParentIsNullAndStatusOrderByCreatedAtDesc(reviewId, Comment.CommentStatus.ACTIVE);
        return comments.stream()
                .map(comment -> {
                    Long likeCount = getCommentLikeCount(comment.getId());
                    boolean likedByMe = hasUserLikedComment(comment.getId(), userId);
                    return CommentDto.fromEntityWithLikeInfo(comment, likeCount, likedByMe);
                })
                .collect(Collectors.toList());
    }

    /**
     * 리뷰의 모든 댓글을 평탄화(flat)된 리스트로 반환 (트리 구조 X, replies X)
     */
    public List<CommentDto> getAllCommentsByReviewIdFlat(Long reviewId) {
        List<Comment> comments = commentRepository.findByReviewIdAndStatusOrderByCreatedAtDesc(reviewId, Comment.CommentStatus.ACTIVE);
        // 트리 구조 없이, 각 댓글의 parentId만 남기고 replies는 null로 반환
        return comments.stream()
                .map(CommentDto::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 리뷰의 모든 댓글 조회
     */
    public List<CommentDto> getAllCommentsByReviewId(Long reviewId) {
        List<Comment> comments = commentRepository.findByReviewIdAndStatusOrderByCreatedAtDesc(reviewId, Comment.CommentStatus.ACTIVE);
        // 1. 모든 댓글을 id -> dto로 매핑
        java.util.Map<Long, CommentDto> dtoMap = new java.util.HashMap<>();
        for (Comment c : comments) {
            dtoMap.put(c.getId(), CommentDto.fromEntity(c));
        }
        // 2. 각 dto의 replies를 채움
        for (Comment c : comments) {
            if (c.getParent() != null) {
                CommentDto parentDto = dtoMap.get(c.getParent().getId());
                if (parentDto.getReplies() == null) parentDto.setReplies(new java.util.ArrayList<>());
                parentDto.getReplies().add(dtoMap.get(c.getId()));
            }
        }
        // 3. 최상위 댓글만 리스트로 반환
        java.util.List<CommentDto> result = new java.util.ArrayList<>();
        for (Comment c : comments) {
            if (c.getParent() == null) {
                CommentDto dto = dtoMap.get(c.getId());
                if (dto.getReplies() == null) dto.setReplies(new java.util.ArrayList<>());
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * 리뷰의 모든 댓글 조회 (사용자별 좋아요 상태 포함)
     */
    public List<CommentDto> getAllCommentsByReviewIdWithUserLikeStatus(Long reviewId, Long userId) {
        List<Comment> comments = commentRepository.findByReviewIdAndStatusOrderByCreatedAtDesc(reviewId, Comment.CommentStatus.ACTIVE);
        // 1. 모든 댓글을 id -> dto로 매핑 (좋아요 정보 포함)
        java.util.Map<Long, CommentDto> dtoMap = new java.util.HashMap<>();
        for (Comment c : comments) {
            Long likeCount = getCommentLikeCount(c.getId());
            boolean likedByMe = hasUserLikedComment(c.getId(), userId);
            dtoMap.put(c.getId(), CommentDto.fromEntityWithLikeInfo(c, likeCount, likedByMe));
        }
        // 2. 각 dto의 replies를 채움
        for (Comment c : comments) {
            if (c.getParent() != null) {
                CommentDto parentDto = dtoMap.get(c.getParent().getId());
                if (parentDto.getReplies() == null) parentDto.setReplies(new java.util.ArrayList<>());
                parentDto.getReplies().add(dtoMap.get(c.getId()));
            }
        }
        // 3. 최상위 댓글만 리스트로 반환
        java.util.List<CommentDto> result = new java.util.ArrayList<>();
        for (Comment c : comments) {
            if (c.getParent() == null) {
                CommentDto dto = dtoMap.get(c.getId());
                if (dto.getReplies() == null) dto.setReplies(new java.util.ArrayList<>());
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * 특정 댓글의 대댓글 조회
     */
    public List<CommentDto> getRepliesByParentId(Long parentId) {
        List<Comment> comments = commentRepository.findByParentIdAndStatusOrderByCreatedAtAsc(parentId, Comment.CommentStatus.ACTIVE);
        return comments.stream()
                .map(CommentDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 댓글 조회
     */
    public List<CommentDto> getCommentsByUserId(Long userId) {
        List<Comment> comments = commentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Comment.CommentStatus.ACTIVE);
        return comments.stream()
                .map(CommentDto::fromEntity)
                .collect(Collectors.toList());
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
