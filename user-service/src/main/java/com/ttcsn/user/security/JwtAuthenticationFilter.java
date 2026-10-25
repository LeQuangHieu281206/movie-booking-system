package com.ttcsn.user.security;

import com.ttcsn.user.entity.enums.UserRole;
import com.ttcsn.user.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bridge giữa JWT và Spring Security: đọc header Authorization kiểu "Bearer <token>",
 * token hợp lệ (type = access) thì set Authentication với principal là
 * {@link AuthenticatedUser} — không truy vấn DB. Token sai/hết hạn thì KHÔNG set gì
 * và cho đi tiếp; endpoint cần xác thực sẽ trả 401 qua EntryPoint của SecurityConfig.
 *
 * KHÔNG gắn @Component — SecurityConfig tạo trực tiếp bằng
 * {@code new JwtAuthenticationFilter(jwtService)} trong addFilterBefore(...)
 * để tránh bị Spring Boot tự đăng ký thêm một lần nữa ở tầng servlet container.
 *
 * @author quanghieu
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String GENERIC_TOKEN_ERROR = "Token khong hop le hoac da het han";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(BEARER_PREFIX.length()).trim();
                Claims claims = jwtService.parseAndVerify(token);
                jwtService.requireTokenType(claims, JwtService.CLAIM_TYPE_ACCESS);

                String roleName = jwtService.extractRole(claims);
                if (roleName == null) {
                    throw new InvalidTokenException(GENERIC_TOKEN_ERROR);
                }
                UserRole role = UserRole.valueOf(roleName);   // sai giá trị enum -> IllegalArgumentException

                var principal = new AuthenticatedUser(
                        jwtService.extractUserId(claims),
                        jwtService.extractEmail(claims),
                        role.name());
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException | IllegalArgumentException ex) {
                // Token sai, hết hạn, dùng refresh token cho API, sai role...: bỏ không set gì,
                // chain tiếp tục (EntryPoint của SecurityConfig sẽ 401 nếu endpoint cần đăng nhập).
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
