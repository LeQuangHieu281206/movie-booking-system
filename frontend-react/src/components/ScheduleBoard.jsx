import React, { useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { dayKey, date, time, money, isBookable } from "../lib/domain.js";
import { Empty, ErrorBox, Loading, Icon } from "./UI.jsx";

export default function ScheduleBoard({
  api,
  movieId = "",
  cinemaId = "",
  movies = [],
  onSelect,
}) {
  const result = useLoad(
    (options) => api.showtimes(movieId, options),
    [api, movieId],
  );
  const [chosenDate, setChosenDate] = useState(""),
    [city, setCity] = useState("");
  if (result.loading) return <Loading label="Đang tìm suất chiếu…" />;
  if (result.error)
    return <ErrorBox message={result.error} onRetry={result.reload} />;
  const eligible = result.data
    .filter(isBookable)
    .filter((show) => !cinemaId || String(show.cinemaId) === String(cinemaId));
  const cities = [
    ...new Set(eligible.map((show) => show.cinemaCity).filter(Boolean)),
  ];
  const filtered = eligible.filter((show) => !city || show.cinemaCity === city);
  const days = [
    ...new Map(
      filtered
        .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
        .map((show) => [dayKey(show.startTime), show.startTime]),
    ).entries(),
  ];
  const currentDate = days.some(([key]) => key === chosenDate)
    ? chosenDate
    : days[0]?.[0];
  const shows = filtered.filter(
    (show) => dayKey(show.startTime) === currentDate,
  );
  const groups = [...new Set(shows.map((show) => show.cinemaId))];
  return (
    <div className="schedule-board">
      <div className="schedule-filter">
        <div className="date-tabs">
          {days.map(([key, value]) => (
            <button
              key={key}
              className={currentDate === key ? "active" : ""}
              onClick={() => setChosenDate(key)}
            >
              <span>
                {new Intl.DateTimeFormat("vi-VN", {
                  timeZone: "Asia/Ho_Chi_Minh",
                  weekday: "short",
                }).format(new Date(value))}
              </span>
              <strong>
                {key.slice(8, 10)}/{key.slice(5, 7)}
              </strong>
            </button>
          ))}
        </div>
        <label className="city-select">
          <span className="sr-only">Thành phố</span>
          <select
            value={city}
            onChange={(event) => {
              setCity(event.target.value);
              setChosenDate("");
            }}
          >
            <option value="">Tất cả thành phố</option>
            {cities.map((value) => (
              <option key={value}>{value}</option>
            ))}
          </select>
        </label>
      </div>
      {!shows.length ? (
        <Empty title="Chưa có suất chiếu phù hợp">
          Hãy chọn thành phố khác hoặc quay lại sau khi lịch chiếu được cập
          nhật.
        </Empty>
      ) : (
        groups.map((id) => {
          const group = shows.filter((show) => show.cinemaId === id),
            cinema = group[0];
          return (
            <section className="cinema-schedule" key={String(id)}>
              <div className="cinema-label">
                <span className="round-icon">
                  <Icon name="pin" />
                </span>
                <div>
                  <h3>{cinema.cinemaName || "Rạp chiếu"}</h3>
                  <p>{cinema.cinemaAddress || cinema.cinemaCity}</p>
                </div>
              </div>
              <div className="showtime-list">
                {group.map((show) => (
                  <button
                    className="showtime-button"
                    key={show.showtimeId}
                    onClick={() => onSelect(show)}
                  >
                    <strong>{time(show.startTime)}</strong>
                    {!movieId && (
                      <span>
                        {movies.find((movie) => movie.movieId === show.movieId)
                          ?.title || `Phim #${show.movieId}`}
                      </span>
                    )}
                    <span>
                      {show.roomType || "2D"} · {money(show.basePrice)}
                    </span>
                    <small>
                      {show.roomName} · {date(show.startTime)}
                    </small>
                  </button>
                ))}
              </div>
            </section>
          );
        })
      )}
    </div>
  );
}
