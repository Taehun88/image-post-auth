package codeit.sb06.imagepost.config;

import codeit.sb06.imagepost.entity.Member;
import codeit.sb06.imagepost.entity.Role;
import codeit.sb06.imagepost.repository.MemberRepository;
import codeit.sb06.imagepost.security.ApiInvalidSessionStrategy;
import codeit.sb06.imagepost.security.ApiSessionExpiredStrategy;
import codeit.sb06.imagepost.security.RestAuthenticationFailureHandler;
import codeit.sb06.imagepost.security.RestAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DataSource dataSource;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler(new RestAuthenticationSuccessHandler())
                        .failureHandler(new RestAuthenticationFailureHandler())
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        // 1. 핵심 설정
                        .key("my-secure-key-1234")           // 쿠키 암호화 키 (실무에선 환경변수 사용 권장)
                        .tokenValiditySeconds(60 * 60 * 24 * 30) // 30일간 유지 (기본 2주)
                        .rememberMeParameter("remember-me")  // 프론트엔드에서 보낼 파라미터명

                        // 2. 영구 토큰 방식 설정 (DB 연동)
                        .userDetailsService(userDetailsService) // 필수: 재인증 시 유저 정보 로드
                        .tokenRepository(tokenRepository())     // 필수: DB 레포지토리 연결
                )
                // 세션 관리 정책 고도화
                .sessionManagement(session -> session
                        // 1. 세션 생성 정책: 필요 시 생성 (기본값이나 명시적으로 설정)
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // 2. [신규] 유효하지 않은 세션(만료 등) 접근 시 처리 전략
                        .invalidSessionStrategy(new ApiInvalidSessionStrategy())
                        // [Step 3-1] 세션 고정 보호 (신규 추가)
                        // 로그인 시 기존 세션 ID를 버리고 새로운 ID 발급
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        // [Step 3-2] 동시 세션 제어 고도화 (수정)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false)
                                .sessionRegistry(sessionRegistry())
                                // 기존: .expiredUrl("/app/login?expired")  <-- 삭제
                                // 변경: API 전용 핸들러 등록
                                .expiredSessionStrategy(new ApiSessionExpiredStrategy())
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                );

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 단방향 암호화 객체 등록
    }

    @Bean
    public CommandLineRunner initData(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // admin 계정이 없으면 생성
            if (memberRepository.findByUsername("admin").isEmpty()) {
                Member admin = Member.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("1234")) // 비밀번호 암호화 필수
                        .role(Role.ADMIN)
                        .build();
                memberRepository.save(admin);
            }

            // user 계정이 없으면 생성
            if (memberRepository.findByUsername("user").isEmpty()) {
                Member user = Member.builder()
                        .username("user")
                        .password(passwordEncoder.encode("1234"))
                        .role(Role.USER)
                        .build();
                memberRepository.save(user);
            }
        };
    }
}
