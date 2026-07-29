package com.aivle13.fin_audit_ai.domain.board.service.post;

import com.aivle13.fin_audit_ai.domain.board.dto.request.post.PostCreateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.request.post.PostUpdateRequest;
import com.aivle13.fin_audit_ai.domain.board.dto.response.post.PostDetailResponse;
import com.aivle13.fin_audit_ai.domain.board.entity.PostAttachmentEntity;
import com.aivle13.fin_audit_ai.domain.board.entity.PostEntity;
import com.aivle13.fin_audit_ai.domain.board.repository.CommentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostAttachmentRepository;
import com.aivle13.fin_audit_ai.domain.board.repository.PostRepository;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.board.PostNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import com.aivle13.fin_audit_ai.global.s3.dto.StoredFile;
import com.aivle13.fin_audit_ai.global.s3.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private static final int MAX_ATTACHMENTS = 5;
    private static final String ATTACHMENT_PREFIX = "board-posts";

    private final PostRepository postRepository;
    private final PostAttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public PostDetailResponse create(Long userId, PostCreateRequest request) {
        UserEntity author = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        PostEntity post = PostEntity.create(author, request.title(), request.content());
        postRepository.save(post);

        storeAttachments(post, request.files());

        List<PostAttachmentEntity> attachments = attachmentRepository.findByPost_Id(post.getId());
        return PostDetailResponse.of(post, attachments);
    }

    public PostDetailResponse update(Long userId, Long postId, PostUpdateRequest request) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        ensureEditable(userId, post);

        post.update(request.title(), request.content());

        List<Long> deleteIds = request.deleteAttachmentIds();
        if (deleteIds != null && !deleteIds.isEmpty()) {
            List<PostAttachmentEntity> toDelete = attachmentRepository.findByPost_IdAndIdIn(postId, deleteIds);
            List<String> fileKeys = toDelete.stream().map(PostAttachmentEntity::getFileKey).toList();
            attachmentRepository.deleteAll(toDelete);
            fileStorageService.deleteAfterCommit(fileKeys);
        }

        storeAttachments(post, request.files());

        List<PostAttachmentEntity> attachments = attachmentRepository.findByPost_Id(postId);
        return PostDetailResponse.of(post, attachments);
    }

    public void delete(Long userId, Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        ensureEditable(userId, post);

        List<PostAttachmentEntity> attachments = attachmentRepository.findByPost_Id(postId);
        List<String> fileKeys = attachments.stream().map(PostAttachmentEntity::getFileKey).toList();

        commentRepository.deleteAll(commentRepository.findByPost_Id(postId));
        attachmentRepository.deleteAll(attachments);
        postRepository.delete(post);

        fileStorageService.deleteAfterCommit(fileKeys);
    }

    // 관리자만 접근 가능하도록 SecurityConfig에서 경로를 제한하지만, 서비스 단에서도 방어적으로 재확인한다.
    public PostDetailResponse updatePinned(Long userId, Long postId, boolean pinned) {
        UserEntity requester = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (requester.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        post.updatePinned(pinned);

        List<PostAttachmentEntity> attachments = attachmentRepository.findByPost_Id(postId);
        return PostDetailResponse.of(post, attachments);
    }

    // 작성자 본인 또는 관리자만 게시글을 수정/삭제할 수 있다.
    private void ensureEditable(Long userId, PostEntity post) {
        if (post.isAuthor(userId)) {
            return;
        }

        UserEntity requester = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (requester.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void storeAttachments(PostEntity post, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return;
        }

        // 같은 게시글에 대한 동시 첨부 요청이 카운트 검증을 동시에 통과해 최대 개수를
        // 넘기지 않도록, 카운트 확인 전에 게시글 행을 잠근다.
        postRepository.findByIdForUpdate(post.getId()).orElseThrow(PostNotFoundException::new);

        long existingCount = attachmentRepository.countByPost_Id(post.getId());
        if (existingCount + validFiles.size() > MAX_ATTACHMENTS) {
            throw new BusinessException(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
        }

        List<String> storedKeys = new ArrayList<>();
        fileStorageService.deleteOnRollback(storedKeys);

        for (MultipartFile file : validFiles) {
            StoredFile stored = fileStorageService.store(file, ATTACHMENT_PREFIX);
            storedKeys.add(stored.s3Key());
            attachmentRepository.save(PostAttachmentEntity.create(post, stored));
        }
    }
}