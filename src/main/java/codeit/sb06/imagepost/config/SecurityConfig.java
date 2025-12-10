package codeit.sb06.imagepost.config;

import codeit.sb06.imagepost.entity.Member;
import codeit.sb06.imagepost.entity.Role;
import codeit.sb06.imagepost.repository.MemberRepository;
import codeit.sb06.imagepost.security.RestAuthenticationFailureHandler;
import codeit.sb06.imagepost.security.RestAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

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
                // [추가] 세션 관리 정책 설정
                .sessionManagement(session -> session
                        .maximumSessions(1)                 // 최대 허용 세션 1개
                        .maxSessionsPreventsLogin(false)    // false: 기존 세션 만료 (밀어내기), true: 신규 로그인 차단
                        .expiredUrl("/app/login?expired")   // 세션 만료 시 이동할 URL (API 환경에서는 핸들러 처리가 더 적합할 수 있음)
                        .sessionRegistry(sessionRegistry())
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
