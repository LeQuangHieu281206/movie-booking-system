package com.ttcsn.user.security;

import com.ttcsn.user.entity.User;
import com.ttcsn.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Dành cho luồng login (Bước 6 - AuthController): tìm user theo email, trả UserDetails
 * với mật khẩu đã băm (password_hash) và authority "ROLE_" + role để DaoAuthenticationProvider
 * so khớp BCrypt. KHÔNG dùng trong JwtAuthenticationFilter (filter stateless, đọc claims).
 *
 * @author quanghieu
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email hoac mat khau khong dung"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
