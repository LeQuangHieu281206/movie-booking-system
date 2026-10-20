package com.ttcsn.common.dto;

/** View cho Feign: dữ liệu Phim thu gọn (movie-service -> booking-service/others). */
public record MovieView(
        Long movieId,
        String title,
        String language,
        String ageRestriction,
        String status,
        String director
) {
    // Jackson sẽ map phàn nàn các field dư (genres, createdAt...) tự bỏ qua.
}
