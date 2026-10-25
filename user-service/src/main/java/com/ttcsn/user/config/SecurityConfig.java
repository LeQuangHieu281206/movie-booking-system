package com.ttcsn.user.config;

import com.ttcsn.user.security.JwtAuthenticationFilter;
import com.ttcsn.user.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security 6 cho user-service: stateless JWT.
 * Chưa đăng nhập -> 401 qua {@link HttpStatusEntryPoint} (không body JSON tùy biến).
 *
 * @author quanghieu
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // legacy CRUD — sẽ thay bằng /api/users sau khi frontend chuyển sang luồng JWT
                        .requestMatchers(HttpMethod.GET, "/api/getUsers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/getUserById").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/createUser").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/updateUser").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/deleteUser").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Tạo filter trực tiếp (new), KHÔNG khai @Bean — tránh Spring Boot
                // tự đăng ký filter này thêm một lần nữa ở tầng servlet container.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
