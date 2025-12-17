package codeit.sb06.imagepost.security.evaluator;

import codeit.sb06.imagepost.entity.Post;
import codeit.sb06.imagepost.exception.PostNotFoundException;
import codeit.sb06.imagepost.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component("Post")
@RequiredArgsConstructor
public class PostPermissionEvaluator implements DomainPermissionEvaluator {

    private final PostRepository postRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String permission) {
        // 1. 게시글 조회
        Post post = postRepository.findById((Long) targetId)
                .orElseThrow(PostNotFoundException::new);

        // 2. 권한 확인 (ADMIN은 무조건 프리패스)
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        // 3. 작성자 본인 확인
        // Post.getAuthor().getUsername()과 현재 로그인한 authentication.getName() 비교
        return post.getAuthor().getUsername().equals(authentication.getName());
    }
}