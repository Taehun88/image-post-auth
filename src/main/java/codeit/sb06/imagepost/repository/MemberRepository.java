package codeit.sb06.imagepost.repository;

import codeit.sb06.imagepost.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 로그인 ID로 회원 조회
    Optional<Member> findByUsername(String username);
}