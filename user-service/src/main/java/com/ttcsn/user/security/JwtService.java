package com.ttcsn.user.security;

import com.ttcsn.user.entity.User;
import io.jsonwebtoken.Claims;

/**
 * Sinh, ký, parse và validate JWT (HS256) dùng chung cho mọi request.
 *
 * Payload ổn định — các service khác (movie, booking, payment) nếu tự verify
 * bằng cùng JWT_SECRET thì đọc đúng các claim này:
 *   sub   : userId (chuỗi số)
 *   email : email của user
 *   role  : giá trị của UserRole (CUSTOMER, ADMIN)
 *   type  : "access" (CLAIM_TYPE_ACCESS) hoặc "refresh" (CLAIM_TYPE_REFRESH)
 *   jti   : chỉ có trên refresh token (UUID) — dùng cho revoke nếu sau này chuyển stateful
 *   iat, exp, iss (= jwt.issuer)
 *
 * Thuật toán ký cố định HS256 (signWith(key, Jwts.SIG.HS256)), độc lập với độ dài secret.
 *
 * 2 loại token:
 *   - access  : TTL = jwt.access-expiration-ms  (mặc định 2 giờ)
 *   - refresh : TTL = jwt.refresh-expiration-ms (mặc định 7 ngày), stateless, không lưu DB
 *
 * Mọi hàm ném {@link com.ttcsn.user.exception.InvalidTokenException} (→ 401)
 * khi token sai chữ ký, hết hạn, sai cấu trúc hoặc sai type — không tiết lộ nguyên nhân.
 */
public interface JwtService {

    /** Giá trị claim type cho access token. */
    String CLAIM_TYPE_ACCESS = "access";
    /** Giá trị claim type cho refresh token. */
    String CLAIM_TYPE_REFRESH = "refresh";

    String CLAIM_EMAIL = "email";
    String CLAIM_ROLE = "role";
    String CLAIM_TYPE = "type";

    /** Tạo access token cho user (type = "access", TTL ngắn). */
    String generateAccessToken(User user);

    /** Tạo refresh token cho user (type = "refresh", TTL dài). */
    String generateRefreshToken(User user);

    /**
     * Verify chữ ký (cùng secret), issuer và hạn dùng; trả Claims để đọc tiếp.
     * Token trống/sai/hết hạn → InvalidTokenException với thông báo chung chung.
     */
    Claims parseAndVerify(String token);

    /**
     * Kiểm tra claim type đúng loại token mong đợi (access hoặc refresh).
     * Trả lại chính các Claims nếu hợp lệ; sai type → InvalidTokenException.
     * Bảo đảm token refresh không dùng được cho API thường và ngược lại.
     */
    Claims requireTokenType(Claims claims, String expectedType);

    /** TTL của access token (ms) — dùng để tính expiresIn (giây) cho AuthResponse. */
    long accessExpirationMs();

    /** userId từ claim sub; sub không phải số hợp lệ → InvalidTokenException. */
    Long extractUserId(Claims claims);

    /** email từ claim email. */
    String extractEmail(Claims claims);

    /** role từ claim role; JwtAuthenticationFilter sẽ validate là giá trị UserRole hợp lệ trước khi set authority "ROLE_" + role. */
    String extractRole(Claims claims);
}
