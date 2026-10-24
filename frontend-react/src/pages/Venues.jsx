import React, { useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { Button, Empty, ErrorBox, Icon, Loading } from "../components/UI.jsx";
import ScheduleBoard from "../components/ScheduleBoard.jsx";

export function Cinemas({ api, onSchedule }) {
  const result = useLoad((options) => api.cinemas(options), [api]);
  const [city, setCity] = useState("");
  const cinemas = (result.data || []).filter(
    (cinema) => !city || cinema.city === city,
  );
  return (
    <section className="container page-section">
      <span className="eyebrow">MỘT ĐIỂM HẸN. NHIỀU TRẢI NGHIỆM.</span>
      <div className="section-heading">
        <div>
          <h1>Hệ thống rạp</h1>
          <p>Tìm nơi bắt đầu cuộc hẹn điện ảnh của bạn.</p>
        </div>
        <label>
          <span className="sr-only">Lọc thành phố</span>
          <select
            value={city}
            onChange={(event) => setCity(event.target.value)}
          >
            <option value="">Tất cả thành phố</option>
            {[
              ...new Set(
                (result.data || [])
                  .map((cinema) => cinema.city)
                  .filter(Boolean),
              ),
            ].map((value) => (
              <option key={value}>{value}</option>
            ))}
          </select>
        </label>
      </div>
      {result.loading ? (
        <Loading />
      ) : result.error ? (
        <ErrorBox message={result.error} onRetry={result.reload} />
      ) : !cinemas.length ? (
        <Empty title="Chưa có rạp phù hợp">
          Hãy chọn thành phố khác hoặc kiểm tra dữ liệu rạp trên backend.
        </Empty>
      ) : (
        <div className="cinema-grid">
          {cinemas.map((cinema, index) => (
            <article className="cinema-card" key={cinema.cinemaId}>
              <div className={`cinema-art tone-${index % 3}`}>
                <span>
                  CINE<span>ROOM</span>
                </span>
                <Icon name="seat" size={64} />
                <b>{cinema.city}</b>
              </div>
              <div className="cinema-info">
                <h2>{cinema.name}</h2>
                <p>
                  <Icon name="pin" size={17} />
                  {cinema.address}
                </p>
                <p>
                  {cinema.phoneNumber
                    ? `Liên hệ: ${cinema.phoneNumber}`
                    : "Lịch chiếu được cập nhật theo rạp"}
                </p>
                <Button
                  onClick={() => onSchedule("", cinema.cinemaId)}
                  className="full"
                >
                  Xem lịch chiếu
                  <Icon name="arrow" size={16} />
                </Button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export function Showtimes({
  api,
  movies,
  initialMovieId,
  initialCinemaId,
  onSelect,
}) {
  const [movieId, setMovieId] = useState(String(initialMovieId || ""));
  return (
    <section className="container page-section">
      <span className="eyebrow">CHỌN MỘT THỜI ĐIỂM DÀNH CHO BẠN</span>
      <div className="section-heading">
        <div>
          <h1>Lịch chiếu</h1>
          <p>
            Chọn giờ chiếu để tiếp tục chọn ghế. Giờ hiển thị theo Việt Nam.
          </p>
        </div>
        <label>
          <span className="sr-only">Chọn phim</span>
          <select
            value={movieId}
            onChange={(event) => setMovieId(event.target.value)}
          >
            <option value="">Tất cả phim</option>
            {movies
              .filter((movie) => movie.status === "SHOWING")
              .map((movie) => (
                <option key={movie.movieId} value={movie.movieId}>
                  {movie.title}
                </option>
              ))}
          </select>
        </label>
      </div>
      {initialCinemaId && (
        <p className="hint">Đang xem lịch của rạp #{initialCinemaId}.</p>
      )}
      <ScheduleBoard
        key={`${movieId}-${initialCinemaId}`}
        api={api}
        movieId={movieId}
        cinemaId={initialCinemaId}
        movies={movies}
        onSelect={onSelect}
      />
    </section>
  );
}
