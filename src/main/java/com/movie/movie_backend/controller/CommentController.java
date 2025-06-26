package com.movie.controller;

import com.movie.entity.Comment;
import com.movie.entity.CommentLike;
import com.movie.service.REVCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final REVCommentService commentService;

    /**
     * 댓글 작성
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createComment(@RequestBody Map<String, Object> request) {
        try {
            Long reviewId = Long.valueOf(request.get("reviewId").toString());
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = (String) request.get("content");
            Long parentId = request.get("parentId") != null ? 
                Long.valueOf(request.get("parentId").toString()) : null;

            Comment comment = commentService.createComment(reviewId, userId, content, parentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", comment.isReply() ? "대댓글이 작성되었습니다." : "댓글이 작성되었습니다.",
                "commentId", comment.getId(),
                "commentType", comment.isReply() ? "REPLY" : "COMMENT"
            ));
        } catch (Exception e) {
            log.error("댓글 작성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 작성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = (String) request.get("content");

            Comment comment = commentService.updateComment(commentId, userId, content);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "댓글이 수정되었습니다.",
                "commentId", comment.getId()
            ));
        } catch (Exception e) {
            log.error("댓글 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 수정에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            
            commentService.deleteComment(commentId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "댓글이 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("댓글 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 삭제에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 리뷰의 최상위 댓글 조회
     */
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Map<String, Object>> getTopLevelComments(@PathVariable Long reviewId) {
        try {
            List<Comment> comments = commentService.getTopLevelCommentsByReviewId(reviewId);
            Long commentCount = commentService.getCommentCountByReviewId(reviewId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", comments,
                "count", comments.size(),
                "totalCount", commentCount
            ));
        } catch (Exception e) {
            log.error("댓글 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 리뷰의 모든 댓글 조회 (트리 구조)
     */
    @GetMapping("/review/{reviewId}/all")
    public ResponseEntity<Map<String, Object>> getAllComments(@PathVariable Long reviewId) {
        try {
            List<Comment> comments = commentService.getAllCommentsByReviewId(reviewId);
            Long commentCount = commentService.getCommentCountByReviewId(reviewId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", comments,
                "count", comments.size(),
                "totalCount", commentCount
            ));
        } catch (Exception e) {
            log.error("전체 댓글 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "전체 댓글 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 특정 댓글의 대댓글 조회
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Map<String, Object>> getReplies(@PathVariable Long commentId) {
        try {
            List<Comment> replies = commentService.getRepliesByParentId(commentId);
            Long replyCount = commentService.getReplyCountByParentId(commentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", replies,
                "count", replies.size(),
                "totalCount", replyCount
            ));
        } catch (Exception e) {
            log.error("대댓글 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "대댓글 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 사용자의 댓글 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserComments(@PathVariable Long userId) {
        try {
            List<Comment> comments = commentService.getCommentsByUserId(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", comments,
                "count", comments.size()
            ));
        } catch (Exception e) {
            log.error("사용자 댓글 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "사용자 댓글 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 댓글 좋아요 추가
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Map<String, Object>> addCommentLike(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            
            CommentLike commentLike = commentService.addCommentLike(commentId, userId);
            Long likeCount = commentService.getCommentLikeCount(commentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "댓글에 좋아요를 눌렀습니다.",
                "likeId", commentLike.getId(),
                "likeCount", likeCount
            ));
        } catch (Exception e) {
            log.error("댓글 좋아요 추가 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 좋아요 추가에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 댓글 좋아요 취소
     */
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Map<String, Object>> removeCommentLike(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            
            commentService.removeCommentLike(commentId, userId);
            Long likeCount = commentService.getCommentLikeCount(commentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "댓글 좋아요를 취소했습니다.",
                "likeCount", likeCount
            ));
        } catch (Exception e) {
            log.error("댓글 좋아요 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 좋아요 취소에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 댓글 좋아요 개수 조회
     */
    @GetMapping("/{commentId}/like-count")
    public ResponseEntity<Map<String, Object>> getCommentLikeCount(@PathVariable Long commentId) {
        try {
            Long likeCount = commentService.getCommentLikeCount(commentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "commentId", commentId,
                "likeCount", likeCount
            ));
        } catch (Exception e) {
            log.error("댓글 좋아요 개수 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 좋아요 개수 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 사용자가 댓글에 좋아요를 눌렀는지 확인
     */
    @GetMapping("/{commentId}/like-status")
    public ResponseEntity<Map<String, Object>> getCommentLikeStatus(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            boolean hasLiked = commentService.hasUserLikedComment(commentId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "commentId", commentId,
                "userId", userId,
                "hasLiked", hasLiked
            ));
        } catch (Exception e) {
            log.error("댓글 좋아요 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "댓글 좋아요 상태 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }
} 