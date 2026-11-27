package com.ttcsn.movie.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Danh sách movieId theo đúng thứ tự hạng 1..5; danh sách rỗng = bỏ toàn bộ top. */
public record HotMoviesRequest(
        @NotNull(message = "movieIds không được để trống")
        @Size(max = 5, message = "Chỉ được chọn tối đa 5 phim hot")
        List<@NotNull Long> movieIds
) {
}
