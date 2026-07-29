package com.aivle13.fin_audit_ai.domain.board.service.comment;

import com.aivle13.fin_audit_ai.domain.board.dto.response.comment.CommentResponse;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.global.exception.board.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<CommentResponse> list(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }
}