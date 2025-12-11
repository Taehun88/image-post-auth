package codeit.sb06.imagepost.service;

import codeit.sb06.imagepost.dto.FileMetaData;
import codeit.sb06.imagepost.dto.request.PostCreateRequest;
import codeit.sb06.imagepost.dto.request.PostUpdateRequest;
import codeit.sb06.imagepost.dto.response.PostImageResponse;
import codeit.sb06.imagepost.dto.response.PostResponse;
import codeit.sb06.imagepost.entity.Member;
import codeit.sb06.imagepost.entity.Post;
import codeit.sb06.imagepost.entity.PostImage;
import codeit.sb06.imagepost.exception.ErrorCode;
import codeit.sb06.imagepost.exception.FileUploadException;
import codeit.sb06.imagepost.exception.PostNotFoundException;
import codeit.sb06.imagepost.repository.MemberRepository;
import codeit.sb06.imagepost.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private static final int MAX_IMAGE_COUNT = 5;

    @Transactional
    public PostResponse savePost(PostCreateRequest request, List<MultipartFile> images) {
        validateImageCount(images);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Member author = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        List<FileMetaData> storedFiles = fileStorageService.storeFiles(images);

        Post post = Post.builder()
                .author(author)
                .title(request.title())
                .content(request.content())
                .tags(request.tags())
                .build();

        List<PostImage> postImages = storedFiles.stream()
                .map(meta -> PostImage.builder()
                        .storageUrl(meta.storageUrl())
                        .originalFileName(meta.originalFileName())
                        .build())
                .collect(Collectors.toList());

        post.setImages(postImages);

        Post savedPost = postRepository.save(post);
        return convertToResponseWithRetrievalUrls(savedPost);
    }

    @Transactional
    @PreAuthorize("hasPermission(#id, 'Post', 'UPDATE')")
    public PostResponse updatePost(Long id, PostUpdateRequest request, List<MultipartFile> images) {
        validateImageCount(images);

        Post post = findPostById(id);

        List<String> oldStorageUrls = post.getImages().stream()
                .map(PostImage::getStorageUrl)
                .collect(Collectors.toList());
        fileStorageService.deleteFiles(oldStorageUrls);

        List<FileMetaData> newStoredFiles = fileStorageService.storeFiles(images);

        List<PostImage> newPostImages = newStoredFiles.stream()
                .map(meta -> PostImage.builder()
                        .storageUrl(meta.storageUrl())
                        .originalFileName(meta.originalFileName())
                        .build())
                .collect(Collectors.toList());

        post.update(request.title(), request.content(), request.tags());

        post.setImages(newPostImages);

        return convertToResponseWithRetrievalUrls(post);
    }

    @Transactional
    @PreAuthorize("hasPermission(#id, 'Post', 'DELETE')")
    public void deletePost(Long id) { // [변경] password 파라미터 제거
        Post post = findPostById(id);

        List<String> storageUrls = post.getImages().stream()
                .map(PostImage::getStorageUrl)
                .collect(Collectors.toList());
        fileStorageService.deleteFiles(storageUrls);

        postRepository.delete(post);
    }

    public PostResponse getPostById(Long id) {
        Post post = findPostById(id);
        return convertToResponseWithRetrievalUrls(post);
    }

    public List<PostResponse> findAllPosts() {
        return postRepository.findAll().stream()
                .map(this::convertToResponseWithRetrievalUrls)
                .collect(Collectors.toList());
    }

    private Post findPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }

    private void validateImageCount(List<MultipartFile> images) {
        if (images != null && images.size() > MAX_IMAGE_COUNT) {
            throw new FileUploadException(ErrorCode.INVALID_FILE_COUNT.getMessage());
        }
    }

    private PostResponse convertToResponseWithRetrievalUrls(Post post) {
        List<PostImageResponse> imageResponses;

        if (post.getImages() != null) {
            imageResponses = post.getImages().stream()
                    .map(image -> PostImageResponse.builder()
                            .id(image.getId())
                            .imageUrl(fileStorageService.getRetrievalUrl(image.getStorageUrl()))
                            .build())
                    .collect(Collectors.toList());
        } else {
            imageResponses = Collections.emptyList();
        }

        return PostResponse.builder()
                .id(post.getId())
                .author(post.getAuthor().getUsername())
                .title(post.getTitle())
                .content(post.getContent())
                .tags(post.getTags())
                .images(imageResponses)
                .createdAt(post.getCreatedAt())
                .build();
    }
}