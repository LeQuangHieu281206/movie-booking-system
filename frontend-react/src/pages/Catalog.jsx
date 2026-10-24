import React, { useState } from "react";
import { filterMovies } from "../lib/domain.js";
import {
  Button,
  Empty,
  ErrorBox,
  Icon,
  Loading,
  Modal,
  Poster,
} from "../components/UI.jsx";
import ScheduleBoard from "../components/ScheduleBoard.jsx";

export function MovieGrid({ movies, onMovie }) {
  return (
    <div className="movie-grid">
      {movies.map((movie) => (
        <article className="movie-card" key={movie.movieId}>
          <button
            className="poster-link"
            onClick={() => onMovie(movie)}
            aria-label={`Xem ${movie.title}`}
          >
            <Poster movie={movie} />
            <span className="poster-action">
              <Icon name="ticket" />
              Khám phá phim
            </span>
          </button>
          <div className="movie-meta">
            <span>
              {movie.genres
                ?.map((genre) => genre.name)
                .slice(0, 2)
                .join(" / ") || "Điện ảnh"}
            </span>
            <span>{movie.durationMinutes} phút</span>
          </div>
          <button className="title-button" onClick={() => onMovie(movie)}>
            {movie.title}
          </button>
          <div className="movie-bottom">
            <span
              className={`status-dot ${movie.status === "UPCOMING" ? "upcoming" : ""}`}
            >
              {movie.status === "UPCOMING"
                ? "Sắp chiếu"
                : movie.status === "ENDED"
                  ? "Đã kết thúc"
                  : "Đang chiếu"}
            </span>
            <button className="text-button" onClick={() => onMovie(movie)}>
              {movie.status === "SHOWING" ? "Đặt vé" : "Chi tiết"}
              <Icon name="arrow" size={16} />
            </button>
          </div>
        </article>
      ))}
    </div>
  );
}

export function Catalog({
  data,
  loading,
  error,
  reload,
  onMovie,
  home = false,
}) {
  const [status, setStatus] = useState("SHOWING"),
    [query, setQuery] = useState(""),
    [genre, setGenre] = useState("");
  const movies = filterMovies(data?.movies || [], { query, genre, status });
  return (
    <section
      className={`catalog section ${home ? "" : "page-section"}`}
      id="phim"
    >
      <div className="section-heading">
        <div>
          <span className="eyebrow">CÂU CHUYỆN TIẾP THEO CỦA BẠN</span>
          <h2>{home ? "Hôm nay xem gì?" : "Khám phá điện ảnh"}</h2>
        </div>
        <div className="segmented">
          <button
            onClick={() => setStatus("SHOWING")}
            className={status === "SHOWING" ? "active" : ""}
          >
            Đang chiếu
          </button>
          <button
            onClick={() => setStatus("UPCOMING")}
            className={status === "UPCOMING" ? "active" : ""}
          >
            Sắp chiếu
          </button>
        </div>
      </div>
      <div className="catalog-filters">
        <label className="search-field">
          <Icon name="search" size={18} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tìm bộ phim bạn yêu thích…"
            aria-label="Tìm phim"
          />
        </label>
        <label>
          <span className="sr-only">Thể loại</span>
          <select
            value={genre}
            onChange={(event) => setGenre(event.target.value)}
          >
            <option value="">Tất cả thể loại</option>
            {data?.genres.map((genre) => (
              <option key={genre.genreId} value={genre.genreId}>
                {genre.name}
              </option>
            ))}
          </select>
        </label>
        <span className="result-count">{movies.length} bộ phim</span>
      </div>
      {loading ? (
        <Loading />
      ) : error ? (
        <ErrorBox message={error} onRetry={reload} />
      ) : movies.length ? (
        <MovieGrid movies={movies} onMovie={onMovie} />
      ) : (
        <Empty title="Chưa tìm thấy phim">
          Thử đổi từ khóa hoặc bộ lọc. Nếu đang dùng backend, hãy kiểm tra dữ
          liệu phim của nhóm.
        </Empty>
      )}
    </section>
  );
}

export function Home({ catalog, onMovie, onNavigate, onSchedule }) {
  const hero = catalog.data?.movies.find((movie) => movie.status === "SHOWING");
  const [quickId, setQuickId] = useState("");
  return (
    <>
      <section className="hero">
        <div className="hero-orbit" />
        {hero && (
          <div className="hero-art">
            <Poster movie={hero} />
          </div>
        )}
        <div className="hero-shade" />
        <div className="container hero-content">
          <span className="eyebrow">
            <span className="gold-line" />
            MÀN ẢNH LỚN. CẢM XÚC TRỌN VẸN.
          </span>
          <p className="hero-kicker">Một chiếc vé nhỏ.</p>
          <h1>{hero ? hero.title : "Một thế giới cảm xúc."}</h1>
          <div className="hero-meta">
            <span className="outline-tag">
              {hero?.ageRestriction || "CINEROOM"}
            </span>
            <span>
              {hero?.genres?.map((genre) => genre.name).join(" · ") ||
                "Điểm hẹn của người yêu điện ảnh"}
            </span>
            {hero && (
              <span>
                <Icon name="clock" size={14} />
                {hero.durationMinutes} phút
              </span>
            )}
          </div>
          <p className="hero-description">
            {hero?.description ||
              "Tìm bộ phim yêu thích, chọn một chỗ ngồi thật vừa ý và tận hưởng câu chuyện trên màn ảnh lớn."}
          </p>
          <div className="button-row">
            <Button
              primary
              onClick={() => (hero ? onMovie(hero) : onNavigate("movies"))}
            >
              <Icon name="ticket" size={18} />
              Khám phá & đặt vé
              <Icon name="arrow" size={18} />
            </Button>
            {hero && (
              <Button onClick={() => onMovie(hero)}>Thông tin phim</Button>
            )}
          </div>
          <div className="hero-footnote">
            <span className="hero-number">
              01<span> / CINEROOM</span>
            </span>
            <span>Hẹn bạn ở rạp.</span>
          </div>
        </div>
      </section>
      <div className="container">
        <form
          className="quick-book"
          onSubmit={(event) => {
            event.preventDefault();
            onSchedule(quickId);
          }}
        >
          <div className="quick-label">
            <Icon name="ticket" size={26} />
            <div>
              <strong>Chọn phim. Chọn ghế.</strong>
              <span>Cuộc hẹn điện ảnh bắt đầu từ đây</span>
            </div>
          </div>
          <label>
            <span>BẠN MUỐN XEM PHIM GÌ?</span>
            <select
              value={quickId}
              onChange={(event) => setQuickId(event.target.value)}
            >
              <option value="">Chọn bộ phim của bạn</option>
              {catalog.data?.movies
                .filter((movie) => movie.status === "SHOWING")
                .map((movie) => (
                  <option key={movie.movieId} value={movie.movieId}>
                    {movie.title}
                  </option>
                ))}
            </select>
          </label>
          <Button primary type="submit">
            Tìm suất chiếu
            <Icon name="arrow" size={17} />
          </Button>
        </form>
        <Catalog {...catalog} home onMovie={onMovie} />
        <section className="experience">
          <div>
            <span className="eyebrow">DÀNH THỜI GIAN CHO ĐIỀU BẠN THÍCH</span>
            <h2>
              Rời màn hình nhỏ.
              <br />
              Bước vào câu chuyện lớn.
            </h2>
            <p>Chọn rạp gần bạn và tìm một suất chiếu phù hợp.</p>
            <Button onClick={() => onNavigate("cinemas")}>
              Khám phá hệ thống rạp
              <Icon name="arrow" size={16} />
            </Button>
          </div>
          <div className="experience-art">
            <Icon name="seat" size={64} />
            <div>
              <strong>Chỗ ngồi dành riêng cho bạn</strong>
              <p>Tự chọn vị trí trên sơ đồ phòng chiếu</p>
            </div>
            <span className="decorative-ticket">
              CINEROOM
              <br />
              <small>ENJOY THE MOMENT</small>
            </span>
          </div>
        </section>
      </div>
    </>
  );
}

export function MovieDetail({ movie, api, onClose, onSelect }) {
  const trailer = /^https:\/\/(www\.)?(youtube\.com|youtu\.be)\//.test(
    movie.trailerUrl || "",
  )
    ? movie.trailerUrl
    : "";
  return (
    <Modal wide title={movie.title} onClose={onClose}>
      <div className="movie-detail">
        <Poster movie={movie} />
        <div>
          <div className="detail-tags">
            <span className="outline-tag">{movie.ageRestriction || "P"}</span>
            <span>{movie.durationMinutes} phút</span>
            <span>{movie.language}</span>
          </div>
          <p>{movie.description || "Nội dung phim đang được cập nhật."}</p>
          <dl>
            <dt>Thể loại</dt>
            <dd>
              {movie.genres?.map((genre) => genre.name).join(", ") ||
                "Đang cập nhật"}
            </dd>
            <dt>Đạo diễn</dt>
            <dd>{movie.director || "Đang cập nhật"}</dd>
            <dt>Diễn viên</dt>
            <dd>{movie.actors || "Đang cập nhật"}</dd>
            <dt>Khởi chiếu</dt>
            <dd>{movie.releaseDate || "Đang cập nhật"}</dd>
          </dl>
          {trailer && (
            <a
              className="button small"
              href={trailer}
              target="_blank"
              rel="noreferrer"
            >
              <Icon name="play" size={16} />
              Xem trailer
            </a>
          )}
        </div>
      </div>
      <div className="subheading">
        <h3>Lịch chiếu & đặt vé</h3>
        <span>Giờ Việt Nam (GMT+7)</span>
      </div>
      {movie.status === "UPCOMING" ? (
        <Empty title="Phim sắp ra mắt">Lịch chiếu sẽ được cập nhật sau.</Empty>
      ) : (
        <ScheduleBoard api={api} movieId={movie.movieId} onSelect={onSelect} />
      )}
    </Modal>
  );
}
