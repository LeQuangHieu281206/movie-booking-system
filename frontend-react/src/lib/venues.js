// Quy tắc thuần cho Rạp / Lịch chiếu (Frontend 3).
// Tách khỏi component để kiểm thử bằng `npm test` mà không cần trình duyệt.
import { dayKey, isBookable } from "./domain.js";

const TZ = "Asia/Ho_Chi_Minh";
const weekdayFormat = new Intl.DateTimeFormat("vi-VN", {
  timeZone: TZ,
  weekday: "short",
});
const longDayFormat = new Intl.DateTimeFormat("vi-VN", {
  timeZone: TZ,
  weekday: "long",
  day: "2-digit",
  month: "2-digit",
});

// ID từ API có thể là number hoặc string; so sánh luôn qua String để không tráo ID.
export const sameId = (a, b) =>
  a !== undefined && a !== null && b !== undefined && b !== null
    ? String(a) === String(b)
    : false;

export const weekdayLabel = (value) => weekdayFormat.format(new Date(value));
export const longDayLabel = (value) => longDayFormat.format(new Date(value));

export function cityList(items = [], field = "city") {
  return [
    ...new Set(
      items
        .map((item) => item?.[field])
        .filter((value) => typeof value === "string" && value.trim()),
    ),
  ].sort((a, b) => a.localeCompare(b, "vi"));
}

// Chỉ giữ suất SCHEDULED, còn trong tương lai, có showtimeId; sắp theo giờ chiếu.
// Không sửa mảng đầu vào.
export function bookableShowtimes(shows, { cinemaId = "" } = {}) {
  return (Array.isArray(shows) ? shows : [])
    .filter(
      (show) =>
        show &&
        show.showtimeId !== undefined &&
        show.showtimeId !== null &&
        isBookable(show),
    )
    .filter((show) => !cinemaId || sameId(show.cinemaId, cinemaId))
    .sort(
      (a, b) =>
        new Date(a.startTime) - new Date(b.startTime) ||
        Number(a.showtimeId) - Number(b.showtimeId),
    );
}

// Các ngày (theo giờ Việt Nam) có suất chiếu, theo thứ tự thời gian.
export function showtimeDays(shows) {
  const days = new Map();
  for (const show of shows)
    if (!days.has(dayKey(show.startTime)))
      days.set(dayKey(show.startTime), show.startTime);
  return [...days].map(([key, value]) => ({ key, value }));
}

// Gom suất theo rạp. Nếu thiếu cinemaId thì dùng tên rạp, để không gộp nhầm các rạp khác nhau.
export function groupByCinema(shows) {
  const groups = new Map();
  for (const show of shows) {
    const key = String(show.cinemaId ?? show.cinemaName ?? "unknown");
    if (!groups.has(key))
      groups.set(key, {
        key,
        cinemaId: show.cinemaId,
        name: show.cinemaName || "Rạp chiếu",
        address: show.cinemaAddress || show.cinemaCity || "",
        shows: [],
      });
    groups.get(key).shows.push(show);
  }
  return [...groups.values()];
}

// Tùy chọn phim cho ô chọn: phim đang chiếu + phim đang được chọn (kể cả sắp chiếu
// hoặc danh mục chưa tải xong) để <select> không bị trống sai.
export function movieOptions(movies, selectedId) {
  const showing = movies.filter((movie) => movie.status === "SHOWING");
  if (!selectedId || showing.some((m) => sameId(m.movieId, selectedId)))
    return showing.map((m) => ({ id: m.movieId, title: m.title }));
  const selected = movies.find((m) => sameId(m.movieId, selectedId));
  return [
    ...showing.map((m) => ({ id: m.movieId, title: m.title })),
    { id: selectedId, title: selected?.title || `Phim #${selectedId}` },
  ];
}

export const telHref = (phone) => {
  const digits = String(phone ?? "").replace(/[^\d+]/g, "");
  return digits.replace(/\D/g, "").length >= 6 ? `tel:${digits}` : "";
};
