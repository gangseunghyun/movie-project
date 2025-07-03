package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.CommentDto;
import com.movie.movie_backend.entity.Comment;
import com.movie.movie_backend.entity.CommentLike;
import com.movie.movie_backend.service.REVCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

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
    public ResponseEntity<Map<String, Object>> createComment(
            @RequestBody Map<String, Object> request) {
        try {
            Long reviewId = Long.valueOf(request.get("reviewId").toString());
            Long userId = Long.valueOf(request.get("userId").toString());
            String content = (String) request.get("content");
            Long parentId = request.get("parentId") != null ? 
                Long.valueOf(request.get("parentId").toString()) : null;

            Comment comment = commentService.createComment(reviewId, userId, content, parentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", comment.isReply() ? "대댓글이 작성되었습니다." : "댓글이 작성되었습니다.");
            response.put("commentId", comment.getId());
            response.put("commentType", comment.isReply() ? "REPLY" : "COMMENT");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 작성 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "댓글 작성에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "댓글이 수정되었습니다.");
            response.put("commentId", comment.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 수정 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "댓글 수정에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            commentService.deleteComment(commentId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "댓글이 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 삭제 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "댓글 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 리뷰의 최상위 댓글 조회
     */
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Map<String, Object>> getTopLevelComments(
            @PathVariable Long reviewId,
            @RequestParam(required = false) Long userId) {
        try {
            List<CommentDto> comments;
            if (userId != null) {
                comments = commentService.getTopLevelCommentsByReviewIdWithUserLikeStatus(reviewId, userId);
            } else {
                comments = commentService.getTopLevelCommentsByReviewId(reviewId);
            }
            Long commentCount = commentService.getCommentCountByReviewId(reviewId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", comments);
            response.put("count", comments.size());
            response.put("totalCount", commentCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "댓글 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 리뷰의 모든 댓글 조회 (트리 구조)
     */
    @GetMapping("/review/{reviewId}/all")
    public ResponseEntity<Map<String, Object>> getAllComments(@PathVariable Long reviewId) {
        try {
            List<CommentDto> comments = commentService.getAllCommentsByReviewId(reviewId);
            Long commentCount = commentService.getCommentCountByReviewId(reviewId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", comments);
            response.put("count", comments.size());
            response.put("totalCount", commentCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("전체 댓글 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "전체 댓글 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 특정 댓글의 대댓글 조회
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Map<String, Object>> getReplies(@PathVariable Long commentId) {
        try {
            List<CommentDto> replies = commentService.getRepliesByParentId(commentId);
            Long replyCount = commentService.getReplyCountByParentId(commentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", replies);
            response.put("count", replies.size());
            response.put("totalCount", replyCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대댓글 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "대댓글 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 사용자의 댓글 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserComments(@PathVariable Long userId) {
        try {
            List<CommentDto> comments = commentService.getCommentsByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", comments);
            response.put("count", comments.size());
            response.put("message", "사용자 댓글 목록을 조회했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 댓글 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "사용자 댓글 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "댓글에 좋아요를 눌렀습니다.");
            response.put("likeId", commentLike.getId());
            response.put("likeCount", likeCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 좋아요 추가 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "댓글 좋아요 추가에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 댓글 좋아요 취소
     */
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Map<String, Object>> removeCommentLike(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        try {
            commentService.removeCommentLike(commentId, userId);
            Long likeCount = commentService.getCommentLikeCount(commentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "댓글 좋아요를 취소했습니다.");
            response.put("likeCount", likeCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 좋아요 취소 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "댓글 좋아요 취소에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 댓글 좋아요 개수 조회
     */
    @GetMapping("/{commentId}/like-count")
    public ResponseEntity<Map<String, Object>> getCommentLikeCount(@PathVariable Long commentId) {
        try {
            Long likeCount = commentService.getCommentLikeCount(commentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("commentId", commentId);
            response.put("likeCount", likeCount);
            response.put("message", "댓글 좋아요 개수를 조회했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 좋아요 개수 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "댓글 좋아요 개수 조회에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("commentId", commentId);
            response.put("userId", userId);
            response.put("hasLiked", hasLiked);
            response.put("message", "사용자 댓글 좋아요 상태를 확인했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 댓글 좋아요 상태 확인 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "사용자 댓글 좋아요 상태 확인에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
