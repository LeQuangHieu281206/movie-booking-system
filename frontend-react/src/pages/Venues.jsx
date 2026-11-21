import React, { useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { cityList, sameId, telHref } from "../lib/venues.js";
import { Button, Empty, ErrorBox, Icon } from "../components/UI.jsx";
import {
  CinemaGridSkeleton,
  ScheduleSkeleton,
} from "../components/Skeleton.jsx";
import ScheduleBoard from "../components/ScheduleBoard.jsx";
import Select from "../components/Select.jsx";

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
      <div className="section-heading">
        <div>
          <h1>Hệ thống rạp</h1>
        </div>
        <Select
          label="Lọc thành phố"
          value={city}
          onChange={setCity}
          options={[
            { value: "", label: "Tất cả thành phố" },
            ...cities.map((value) => ({ value, label: value })),
          ]}
        />
      </div>
      {result.loading ? (
        <CinemaGridSkeleton />
      ) : result.error ? (
        <ErrorBox message={result.error} onRetry={result.reload} />
      ) : !cinemas.length ? (
        <Empty title="Chưa có rạp phù hợp">
          {all.length
            ? "Hãy chọn thành phố khác."
            : "Danh sách rạp đang được cập nhật. Vui lòng quay lại sau."}
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
      <div className="section-heading">
        <div>
          <h1>Lịch chiếu</h1>
        </div>
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
          moviePicker
          onMovieChange={setMovieId}
          onSelect={onSelect}
          onClearCinema={() => setCinemaId("")}
        />
      ) : (
        <ScheduleSkeleton />
      )}
    </section>
  );
}
