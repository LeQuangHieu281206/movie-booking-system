package com.ttcsn.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

/**
 * Bảng phân quyền của gateway. Rule đầu tiên khớp (method + path) sẽ thắng.
 *
 * <p>Public chỉ gồm luồng đăng nhập/đăng ký và các GET đọc dữ liệu công khai.
 * Path không khớp rule nào mặc định chỉ ADMIN được đi tiếp — endpoint mới không
 * tự động mở cho CUSTOMER. Path không chuẩn (encoded separator, dot segment,
 * matrix parameter, double slash...) bị từ chối trước khi so rule.
 */
final class GatewayAuthorizationRules {

    enum Access {
        PUBLIC,
        CUSTOMER_OR_ADMIN,
        ADMIN,
        INVALID
    }

    record Rule(HttpMethod method, PathPattern path, Access access) {
    }

    private static final PathPatternParser PARSER = new PathPatternParser();

    private static final List<Rule> RULES = List.of(
            // CORS preflight không mang Authorization.
            rule(HttpMethod.OPTIONS, "/**", Access.PUBLIC),

            // Đăng nhập / đăng ký.
            rule(null, "/api/auth/**", Access.PUBLIC),

            // GET công khai — movie-service.
            rule(HttpMethod.GET, "/api/getMovies", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/searchMovies", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getMovieById", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getGenres", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getGenreById", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getShowtimes", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getShowtimeById", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getShowtimeSeats", Access.PUBLIC),

            // GET công khai — venue-service.
            rule(HttpMethod.GET, "/api/getCinemas", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getCinemaById", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getCities", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getCinemasByCity", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getRooms", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getRoomById", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getSeatsByRoomId", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/getSeatById", Access.PUBLIC),

            // CRUD quản trị — movie-service.
            rule(HttpMethod.POST, "/api/createMovie", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/updateMovie", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteMovie", Access.ADMIN),
            rule(HttpMethod.POST, "/api/createGenre", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/updateGenre", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteGenre", Access.ADMIN),
            rule(HttpMethod.POST, "/api/createShowtime", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/updateShowtime", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteShowtime", Access.ADMIN),

            // CRUD quản trị — venue-service.
            rule(HttpMethod.POST, "/api/createCinema", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/updateCinema", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteCinema", Access.ADMIN),
            rule(HttpMethod.POST, "/api/createRoom", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/updateRoom", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteRoom", Access.ADMIN),
            rule(HttpMethod.POST, "/api/createSeat", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/updateSeat", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteSeat", Access.ADMIN),

            // CRUD user legacy — user-service vẫn tự kiểm tra ADMIN, gateway chặn sớm.
            rule(HttpMethod.GET, "/api/getUsers", Access.ADMIN),
            rule(HttpMethod.GET, "/api/getUserById", Access.ADMIN),
            rule(HttpMethod.POST, "/api/createUser", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/updateUser", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteUser", Access.ADMIN),

            // Hồ sơ của chính mình.
            rule(HttpMethod.GET, "/api/users/**", Access.CUSTOMER_OR_ADMIN),

            // Giữ/nhả ghế là thao tác của khách, không phải CRUD quản trị.
            rule(HttpMethod.POST, "/api/holdShowtimeSeat", Access.CUSTOMER_OR_ADMIN),
            rule(HttpMethod.POST, "/api/releaseShowtimeSeat", Access.CUSTOMER_OR_ADMIN),

            // Booking / payment — ownership và trạng thái được kiểm tra ở booking-service.
            rule(HttpMethod.GET, "/api/getBookings", Access.ADMIN),
            rule(HttpMethod.GET, "/api/getBookingById", Access.CUSTOMER_OR_ADMIN),
            rule(HttpMethod.GET, "/api/getBookingsByUserId", Access.CUSTOMER_OR_ADMIN),
            rule(HttpMethod.GET, "/api/getBookingsByShowtimeId", Access.ADMIN),
            rule(HttpMethod.POST, "/api/createBooking", Access.CUSTOMER_OR_ADMIN),
            rule(HttpMethod.PUT, "/api/updateBooking", Access.CUSTOMER_OR_ADMIN),
            rule(HttpMethod.DELETE, "/api/deleteBooking", Access.CUSTOMER_OR_ADMIN),
            rule(HttpMethod.POST, "/api/createPayment", Access.CUSTOMER_OR_ADMIN),
            rule(HttpMethod.GET, "/api/getPayments/**", Access.CUSTOMER_OR_ADMIN)
    );

    private GatewayAuthorizationRules() {
    }

    static Access requiredAccess(ServerHttpRequest request) {
        PathContainer path = canonicalPath(request.getURI().getRawPath());
        if (path == null) {
            return Access.INVALID;
        }

        HttpMethod method = request.getMethod();
        return RULES.stream()
                .filter(r -> r.method() == null || r.method().equals(method))
                .filter(r -> r.path().matches(path))
                .findFirst()
                .map(Rule::access)
                // Endpoint mới chưa khai báo: chỉ ADMIN, không tự mở cho CUSTOMER.
                .orElse(Access.ADMIN);
    }

    static boolean allows(Access access, String role) {
        return switch (access) {
            case PUBLIC -> true;
            case CUSTOMER_OR_ADMIN -> "CUSTOMER".equals(role) || "ADMIN".equals(role);
            case ADMIN -> "ADMIN".equals(role);
            case INVALID -> false;
        };
    }

    /**
     * Chỉ chấp nhận path ASCII chuẩn, bắt đầu bằng '/', không có encoded byte,
     * matrix parameter, backslash, double slash hoặc dot segment.
     *
     * <p>Một dấu '/' ở cuối được chuẩn hóa (bỏ đi) để rule khớp ổn định; các biến thể
     * còn lại bị từ chối (404) thay vì để downstream tự normalize theo cách khác.
     */
    private static PathContainer canonicalPath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty() || rawPath.charAt(0) != '/') {
            return null;
        }
        if (rawPath.contains("//")
                || rawPath.indexOf('%') >= 0
                || rawPath.indexOf(';') >= 0
                || rawPath.indexOf('\\') >= 0) {
            return null;
        }
        // Endpoint hiện tại đều ASCII; chặn luôn control char/khoảng trắng/Unicode.
        if (!rawPath.chars().allMatch(c -> c >= 0x21 && c <= 0x7E)) {
            return null;
        }

        String path = rawPath.length() > 1 && rawPath.endsWith("/")
                ? rawPath.substring(0, rawPath.length() - 1)
                : rawPath;

        for (String segment : path.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                return null;
            }
        }

        try {
            return PathContainer.parsePath(path);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Rule rule(HttpMethod method, String pattern, Access access) {
        return new Rule(method, PARSER.parse(pattern), access);
    }
}
