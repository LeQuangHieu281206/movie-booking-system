import React, { useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { cityList, movieOptions, sameId, telHref } from "../lib/venues.js";
import { Button, Empty, ErrorBox, Icon, Loading } from "../components/UI.jsx";
import ScheduleBoard from "../components/ScheduleBoard.jsx";

export function Cinemas({ api, onSchedule }) {
  const result = useLoad((options) => api.cinemas(options), [api]);
  const [chosenCity, setCity] = useState("");
  const all = result.data || [];
  const cities = cityList(all);
  // Nếu thành phố đã chọn không còn trong dữ liệu mới tải thì quay về "Tất cả".
  const city = cities.includes(chosenCity) ? chosenCity : "";
  const cinemas = all.filter((cinema) => !city || cinema.city === city);
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
            {cities.map((value) => (
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
          {all.length
            ? "Hãy chọn thành phố khác."
            : "Backend chưa trả về rạp nào. Hãy kiểm tra dữ liệu rạp trong venue-service."}
        </Empty>
      ) : (
        <div className="cinema-grid">
          {cinemas.map((cinema) => {
            const phone = telHref(cinema.phoneNumber);
            return (
              <article className="cinema-card" key={cinema.cinemaId}>
                {/* Màu thẻ theo vị trí trong danh sách gốc, không đổi khi lọc thành phố. */}
                <div className={`cinema-art tone-${all.indexOf(cinema) % 3}`}>
                  <span>
                    CINE<span>ROOM</span>
                  </span>
                  <Icon name="seat" size={64} />
                  <b>{cinema.city || "Việt Nam"}</b>
                </div>
                <div className="cinema-info">
                  <h2>{cinema.name}</h2>
                  <p>
                    <Icon name="pin" size={17} />
                    {cinema.address || "Địa chỉ đang được cập nhật"}
                  </p>
                  <p>
                    {cinema.phoneNumber ? (
                      phone ? (
                        <>
                          Liên hệ: <a href={phone}>{cinema.phoneNumber}</a>
                        </>
                      ) : (
                        `Liên hệ: ${cinema.phoneNumber}`
                      )
                    ) : (
                      "Lịch chiếu được cập nhật theo rạp"
                    )}
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
            );
          })}
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
  const [chosenMovieId, setMovieId] = useState(String(initialMovieId || ""));
  // /getShowtimes không lọc sẽ trả toàn bộ lịch của rạp (rất lớn), nên luôn
  // chọn sẵn một phim đang chiếu thay vì gọi không tham số.
  const movieId =
    chosenMovieId ||
    String(movies.find((movie) => movie.status === "SHOWING")?.movieId ?? "");
  const [cinemaId, setCinemaId] = useState(String(initialCinemaId || ""));
  // Chỉ tải danh sách rạp khi đang lọc theo rạp, để hiện tên rạp thay vì "#id".
  const filteringCinema = Boolean(cinemaId);
  const cinemas = useLoad(
    (options) => (filteringCinema ? api.cinemas(options) : []),
    [api, filteringCinema],
  );
  const cinemaName = (cinemas.data || []).find((cinema) =>
    sameId(cinema.cinemaId, cinemaId),
  )?.name;
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
            {movieOptions(movies, movieId).map((movie) => (
              <option key={movie.id} value={movie.id}>
                {movie.title}
              </option>
            ))}
          </select>
        </label>
      </div>
      {cinemaId && (
        <p className="hint cinema-filter">
          Đang xem lịch của <strong>{cinemaName || `rạp #${cinemaId}`}</strong>.{" "}
          <button
            type="button"
            className="link-button"
            onClick={() => setCinemaId("")}
          >
            Xem tất cả rạp
          </button>
        </p>
      )}
      {movieId ? (
        <ScheduleBoard
          key={movieId}
          api={api}
          movieId={movieId}
          cinemaId={cinemaId}
          cinemaName={cinemaName}
          movies={movies}
          onSelect={onSelect}
          onClearCinema={() => setCinemaId("")}
        />
      ) : (
        <Loading label="Đang tải danh sách phim…" />
      )}
    </section>
  );
}
