package codeit.sb06.imagepost.config;

import codeit.sb06.imagepost.entity.Role;
import codeit.sb06.imagepost.repository.MemberRepository;
import codeit.sb06.imagepost.security.RestAuthenticationFailureHandler;
import codeit.sb06.imagepost.security.RestAuthenticationSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(form -> form
                .loginProcessingUrl("/api/login")
                .successHandler(new RestAuthenticationSuccessHandler(objectMapper))
                .failureHandler(new RestAuthenticationFailureHandler(objectMapper))
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/app/login")
                .sessionRegistry(sessionRegistry()))

            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .logoutSuccessHandler((req, res, auth) -> {
                    res.setStatus(200);
                    res.setContentType("application/json");
                    objectMapper.writeValue(res.getWriter(), java.util.Map.of("message", "Logout successful"));
                })
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    @Bean
    public CommandLineRunner initData(MemberRepository memberRepository, PasswordEncoder passwordEncoder){
        if(memberRepository.findByUsername("member1").isEmpty()){
            return args -> {
                memberRepository.save(
                    codeit.sb06.imagepost.entity.Member.builder()
                        .username("member1")
                        .password(passwordEncoder.encode("password1"))
                        .role(Role.USER)
                        .build()
                );
                memberRepository.save(
                    codeit.sb06.imagepost.entity.Member.builder()
                        .username("member2")
                        .password(passwordEncoder.encode("password2"))
                        .role(Role.USER)
                        .build()
                );
            };
        }
        return args -> {

        };
    }

    @Bean
    public CommandLineRunner initAdmin(MemberRepository memberRepository, PasswordEncoder passwordEncoder){
        if(memberRepository.findByUsername("admin").isEmpty()){
            return args -> {
                memberRepository.save(
                    codeit.sb06.imagepost.entity.Member.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("adminpass"))
                        .role(Role.ADMIN)
                        .build()
                );
            };
        }
        return args -> {

        };
    }
}


