package org.lostwind.netlab.configuration;

import jakarta.servlet.http.HttpSession;
import org.lostwind.netlab.filter.VerificationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    @SuppressWarnings("removal")
    public PersistentTokenRepository tokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setCreateTableOnStartup(false);
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PersistentTokenRepository repo) {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/login",
                            "/doLogin",
                            "/component/**",
                            "/admin/css/**",
                            "/admin/images/**",
                            "/view/error/**",
                            "/auth/verification-code/send",
                            "/error").permitAll();
                    auth.requestMatchers("/admin/**").hasAnyRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .formLogin(conf -> {
                    conf.loginPage("/login");
                    conf.loginProcessingUrl("/doLogin");
                    conf.successHandler((request, response, authentication) -> {
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            session.removeAttribute("LOGIN_CODE");
                            session.removeAttribute("LOGIN_CODE_EMAIL");
                            session.removeAttribute("LOGIN_CODE_EXPIRES_AT");
                        }
                        response.sendRedirect(request.getContextPath() + "/index");
                    });
                    conf.defaultSuccessUrl("/index", true);
                    conf.failureUrl("/login?error");
                    conf.permitAll();
                })
                .rememberMe(conf -> {
                    conf.rememberMeParameter("remember-me");
                    conf.tokenRepository(repo);
                    conf.tokenValiditySeconds(3600 * 12);
                })
                .csrf(AbstractHttpConfigurer::disable)
                .logout(conf -> {
                    conf.logoutUrl("/doLogout");
                    conf.logoutSuccessUrl("/login");
                    conf.permitAll();
                })
                .addFilterBefore(
                    new VerificationFilter(),
                    UsernamePasswordAuthenticationFilter.class
                )
                .headers(header -> {
                        header.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                    }
                )
                .build();
    }
}
