import React, { useEffect, useMemo, useRef, useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { useNow } from "../lib/useNow.js";
import { useHorizontalWheel } from "../lib/useHorizontalWheel.js";
import { dayKey, date, time, money } from "../lib/domain.js";
import {
  bookableShowtimes,
  cinemaList,
  cityList,
  longDayLabel,
  movieOptions,
  sameId,
  showtimeDays,
  weekdayLabel,
} from "../lib/venues.js";
import { Button, Empty, ErrorBox, Icon, SearchSelect } from "./UI.jsx";
import { ScheduleSkeleton } from "./Skeleton.jsx";

// Luồng đặt vé: Phim → Thành phố → Rạp chiếu → Ngày → Suất chiếu.
// Bước chọn ghế nằm ở Booking (mở khi bấm một suất chiếu).
const STEPS = [
  ["movie", "Phim"],
  ["city", "Thành phố"],
  ["cinema", "Rạp chiếu"],
  ["date", "Ngày"],
  ["showtime", "Suất chiếu"],
];

export default function ScheduleBoard({
  api,
  movieId = "",
  movieTitle = "",
  movies = [],
  moviePicker = false,
  onMovieChange,
  cinemaId = "",
  cinemaName = "",
  onSelect,
  onClearCinema,
}) {
  const result = useLoad(
    (options) => api.showtimes(movieId, options),
    [api, movieId],
  );
  const [chosenCity, setChosenCity] = useState(""),
    [chosenCinema, setChosenCinema] = useState(""),
    [chosenDate, setChosenDate] = useState("");
  const stepsRef = useHorizontalWheel();
  const datesRef = useHorizontalWheel();
  // Mở thẳng bước ngày khi đã có rạp chỉ định (từ trang Rạp chiếu).
  const [step, setStep] = useState(cinemaId ? 3 : 1);
  // Tính lại mỗi phút: suất vừa bắt đầu sẽ tự biến mất khỏi danh sách.
  const tick = useNow();
  const eligible = useMemo(
    () => bookableShowtimes(result.data, { cinemaId }),
    [result.data, cinemaId, tick],
  );
  const title =
    movieTitle ||
    movies.find((movie) => sameId(movie.movieId, movieId))?.title ||
    "";
  const cities = cityList(eligible, "cinemaCity");
  // Thành phố đã chọn mà không còn suất nào (ví dụ sau khi làm mới) thì bỏ chọn.
  // Hệ thống chỉ có một thành phố thì coi như đã chọn để không chặn các bước sau.
  const city = cities.includes(chosenCity)
    ? chosenCity
    : cities.length === 1
      ? cities[0]
      : "";
  const cinemas = cinemaList(eligible).filter(
    (item) => !city || item.city === city,
  );
  const cinema =
    cinemas.find((item) => item.key === chosenCinema) ||
    (cinemaId
      ? cinemas.find((item) => sameId(item.cinemaId, cinemaId))
      : null);
  const days = showtimeDays(cinema?.shows || []);
  const currentDate = days.some((item) => item.key === chosenDate)
    ? chosenDate
    : days[0]?.key || "";
  const shows = (cinema?.shows || []).filter(
    (show) => dayKey(show.startTime) === currentDate,
  );
  // Thành phố duy nhất thì không bắt người dùng bấm thêm một bước vô nghĩa.
  // Chỉ tự nhảy một lần sau khi dữ liệu về, để người dùng vẫn quay lại được.
  const autoAdvanced = useRef(false);
  useEffect(() => {
    if (autoAdvanced.current || !result.data) return;
    autoAdvanced.current = true;
    if (!cinemaId && cities.length <= 1) setStep(2);
  }, [result.data, cinemaId, cities.length]);
  // Bỏ lọc "một rạp" (từ trang Rạp chiếu) thì quay về bước chọn rạp.
  const hadCinemaFilter = useRef(Boolean(cinemaId));
  useEffect(() => {
    if (!hadCinemaFilter.current || cinemaId) return;
    hadCinemaFilter.current = false;
    setChosenCinema("");
    setChosenDate("");
    setStep((current) => (current > 2 ? 2 : current));
  }, [cinemaId]);

  if (result.loading) return <ScheduleSkeleton />;
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

  const movieMissing = !movieId;
  const chosenDay = days.find((item) => item.key === currentDate);
  const values = {
    movie: title,
    city,
    cinema: cinema?.name || "",
    date: chosenDay ? longDayLabel(chosenDay.value) : "",
    showtime: "",
  };
  // Bước chỉ mở khi đã có lựa chọn của các bước trước.
  const unlocked = (index) => {
    if (index === 0) return moviePicker;
    if (movieMissing) return false;
    if (index >= 2 && !city) return false;
    if (index >= 3 && !cinema) return false;
    if (index >= 4 && !currentDate) return false;
    return true;
  };
  const dayCount = (value) =>
    eligible.filter((show) => show.cinemaCity === value).length;
  return (
    <div className="schedule-board fade-in">
      <ol className="flow-steps" ref={stepsRef}>
        {STEPS.map(([key, label], index) => (
          <li
            key={key}
            className={`${index === step ? "active" : ""} ${index < step ? "done" : ""}`}
          >
            <button
              type="button"
              disabled={!unlocked(index)}
              aria-current={index === step ? "step" : undefined}
              onClick={() => setStep(index)}
            >
              <span className="flow-index">{index + 1}</span>
              <span className="flow-text">
                <b>{label}</b>
                {values[key] && <small>{values[key]}</small>}
              </span>
            </button>
          </li>
        ))}
      </ol>
      <div className="flow-panel">
        <div className="step-in" key={step}>
        {step === 0 &&
          (moviePicker ? (
            <SearchSelect
              label="Chọn phim"
              placeholder="Tìm phim đang chiếu…"
              emptyText="Không tìm thấy phim đang chiếu"
              options={movieOptions(movies, movieId).map((movie) => ({
                id: movie.id,
                label: movie.title,
              }))}
              value={movieId}
              onChange={(id) => onMovieChange?.(id)}
            />
          ) : (
            <p className="flow-note">
              <Icon name="ticket" size={16} />
              Bạn đang đặt vé cho <strong>{title}</strong>.
            </p>
          ))}
        {step === 1 && (
          <div className="choice-chips" role="group" aria-label="Chọn thành phố">
            {cities.map((value) => (
              <button
                key={value}
                type="button"
                className={value === city ? "active" : ""}
                aria-pressed={value === city}
                onClick={() => {
                  setChosenCity(value);
                  setChosenCinema("");
                  setChosenDate("");
                  setStep(2);
                }}
              >
                <b>{value}</b>
                <small>{dayCount(value)} suất</small>
              </button>
            ))}
          </div>
        )}
        {step === 2 && (
          <div className="choice-list" role="group" aria-label="Chọn rạp chiếu">
            {cinemas.map((item) => (
              <button
                key={item.key}
                type="button"
                className={`choice-card ${cinema?.key === item.key ? "active" : ""}`}
                aria-pressed={cinema?.key === item.key}
                onClick={() => {
                  setChosenCinema(item.key);
                  setChosenDate("");
                  setStep(3);
                }}
              >
                <span className="round-icon">
                  <Icon name="pin" size={17} />
                </span>
                <span className="choice-text">
                  <strong>{item.name}</strong>
                  <small>{item.address || item.city}</small>
                </span>
                <span className="choice-count">{item.shows.length} suất</span>
              </button>
            ))}
          </div>
        )}
        {step === 3 &&
          (days.length ? (
            <div
              className="date-tabs"
              role="group"
              aria-label="Chọn ngày chiếu"
              ref={datesRef}
            >
              {days.map(({ key, value }) => (
                <button
                  type="button"
                  key={key}
                  className={currentDate === key ? "active" : ""}
                  aria-pressed={currentDate === key}
                  aria-label={longDayLabel(value)}
                  onClick={() => {
                    setChosenDate(key);
                    setStep(4);
                  }}
                >
                  <span>{weekdayLabel(value)}</span>
                  <strong>
                    {key.slice(8, 10)}/{key.slice(5, 7)}
                  </strong>
                </button>
              ))}
            </div>
          ) : (
            <Empty title="Rạp này chưa có ngày chiếu">Hãy chọn rạp khác.</Empty>
          ))}
        {step === 4 &&
          (shows.length ? (
            <>
              <div
                className="showtime-grid"
                role="group"
                aria-label="Chọn suất chiếu"
              >
                {shows.map((show) => (
                  <button
                    type="button"
                    className="showtime-button"
                    key={show.showtimeId}
                    aria-label={`Chọn suất ${time(show.startTime)} ${date(show.startTime)}, ${title}, ${cinema?.name}${show.roomName ? `, ${show.roomName}` : ""}. Tiếp tục chọn ghế.`}
                    onClick={() => onSelect(show)}
                  >
                    <strong>{time(show.startTime)}</strong>
                    <span>
                      {show.roomType || "2D"} · {money(show.basePrice)}
                    </span>
                    <small>
                      {[show.roomName, date(show.startTime)]
                        .filter(Boolean)
                        .join(" · ")}
                    </small>
                  </button>
                ))}
              </div>
            </>
          ) : (
            <Empty title="Ngày này chưa có suất chiếu">
              Hãy chọn ngày khác hoặc rạp khác.
            </Empty>
          ))}
        </div>
      </div>
    </div>
  );
}
