import React, { useMemo, useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { useNow } from "../lib/useNow.js";
import { dayKey, date, time, money } from "../lib/domain.js";
import {
  bookableShowtimes,
  cityList,
  groupByCinema,
  longDayLabel,
  sameId,
  showtimeDays,
  weekdayLabel,
} from "../lib/venues.js";
import { Button, Empty, ErrorBox, Loading, Icon } from "./UI.jsx";

export default function ScheduleBoard({
  api,
  movieId = "",
  cinemaId = "",
  cinemaName = "",
  movies = [],
  onSelect,
  onClearCinema,
}) {
  const result = useLoad(
    (options) => api.showtimes(movieId, options),
    [api, movieId],
  );
  const [chosenDate, setChosenDate] = useState(""),
    [chosenCity, setChosenCity] = useState("");
  // Tính lại mỗi phút: suất vừa bắt đầu sẽ tự biến mất khỏi danh sách.
  const tick = useNow();
  const eligible = useMemo(
    () => bookableShowtimes(result.data, { cinemaId }),
    [result.data, cinemaId, tick],
  );
  if (result.loading) return <Loading label="Đang tìm suất chiếu…" />;
  if (result.error)
    return <ErrorBox message={result.error} onRetry={result.reload} />;

  if (!eligible.length)
    return (
      <>
        <Empty title="Chưa có suất chiếu sắp tới">
          {cinemaId
            ? `${cinemaName || "Rạp này"} hiện chưa có suất chiếu nào còn mở đặt vé${movieId ? " cho phim đã chọn" : ""}.`
            : movieId
              ? "Phim này chưa có suất chiếu nào còn mở đặt vé. Hãy quay lại sau khi lịch chiếu được cập nhật."
              : "Chưa có suất chiếu nào còn mở đặt vé. Hãy quay lại sau khi lịch chiếu được cập nhật."}
        </Empty>
        {cinemaId && onClearCinema && (
          <div className="schedule-empty-action">
            <Button onClick={onClearCinema}>Xem lịch tất cả rạp</Button>
          </div>
        )}
      </>
    );

  const cities = cityList(eligible, "cinemaCity");
  // Thành phố đã chọn mà không còn suất nào (ví dụ sau khi làm mới) thì quay về "Tất cả".
  const city = cities.includes(chosenCity) ? chosenCity : "";
  const filtered = eligible.filter((show) => !city || show.cinemaCity === city);
  const days = showtimeDays(filtered);
  const currentDate = days.some(({ key }) => key === chosenDate)
    ? chosenDate
    : days[0]?.key;
  const shows = filtered.filter(
    (show) => dayKey(show.startTime) === currentDate,
  );
  const groups = groupByCinema(shows);
  return (
    <div className="schedule-board">
      <div className="schedule-filter">
        <div className="date-tabs" role="group" aria-label="Chọn ngày chiếu">
          {days.map(({ key, value }) => (
            <button
              type="button"
              key={key}
              className={currentDate === key ? "active" : ""}
              aria-pressed={currentDate === key}
              aria-label={longDayLabel(value)}
              onClick={() => setChosenDate(key)}
            >
              <span>{weekdayLabel(value)}</span>
              <strong>
                {key.slice(8, 10)}/{key.slice(5, 7)}
              </strong>
            </button>
          ))}
        </div>
        {cities.length > 1 && (
          <label className="city-select">
            <span className="sr-only">Thành phố</span>
            <select
              value={city}
              onChange={(event) => {
                setChosenCity(event.target.value);
                setChosenDate("");
              }}
            >
              <option value="">Tất cả thành phố</option>
              {cities.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </label>
        )}
      </div>
      {groups.map((group) => (
        <section className="cinema-schedule" key={group.key}>
          <div className="cinema-label">
            <span className="round-icon">
              <Icon name="pin" />
            </span>
            <div>
              <h3>{group.name}</h3>
              <p>{group.address}</p>
            </div>
          </div>
          <div className="showtime-list">
            {group.shows.map((show) => {
              const title =
                movies.find((movie) => sameId(movie.movieId, show.movieId))
                  ?.title || `Phim #${show.movieId}`;
              return (
                <button
                  type="button"
                  className="showtime-button"
                  key={show.showtimeId}
                  aria-label={`Chọn suất ${time(show.startTime)} ${date(show.startTime)}, ${title}, ${group.name}${show.roomName ? `, ${show.roomName}` : ""}`}
                  onClick={() => onSelect(show)}
                >
                  <strong>{time(show.startTime)}</strong>
                  {!movieId && <span>{title}</span>}
                  <span>
                    {show.roomType || "2D"} · {money(show.basePrice)}
                  </span>
                  <small>
                    {[show.roomName, date(show.startTime)]
                      .filter(Boolean)
                      .join(" · ")}
                  </small>
                </button>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}
