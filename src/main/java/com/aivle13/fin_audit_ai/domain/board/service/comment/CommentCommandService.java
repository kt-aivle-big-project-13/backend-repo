package com.aivle13.fin_audit_ai.domain.board.service.comment;

import com.aivle13.fin_audit_ai.domain.board.dto.request.comment.CommentCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.comment.CommentUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.comment.CommentResponse;
import com.aivle13.fin_audit_ai.domain.board.entity.CommentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.board.CommentNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.board.PostNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 댓글 작성/수정/삭제는 SecurityConfig에서 ROLE_ADMIN으로만 접근하도록 이미 제한되어 있다.
@Service
@RequiredArgsConstructor
@Transactional
public class CommentCommandService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentResponse create(Long userId, Long postId, CommentCreateRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        UserEntity author = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        CommentEntity comment = CommentEntity.create(post, author, request.content());
        commentRepository.save(comment);

        return CommentResponse.from(comment);
    }

    public CommentResponse update(Long postId, Long commentId, CommentUpdateRequest request) {
        CommentEntity comment = commentRepository.findByIdAndPost_Id(commentId, postId)
                .orElseThrow(CommentNotFoundException::new);

        comment.updateContent(request.content());

        return CommentResponse.from(comment);
    }

    public void delete(Long postId, Long commentId) {
        CommentEntity comment = commentRepository.findByIdAndPost_Id(commentId, postId)
                .orElseThrow(CommentNotFoundException::new);

        commentRepository.delete(comment);
    }
}