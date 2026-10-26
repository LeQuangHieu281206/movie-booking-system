export const money = (value) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);
export const dayKey = (value) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value));
export const time = (value) =>
  new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
export const date = (value) =>
  new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
  }).format(new Date(value));
export const statusText = (value) =>
  ({
    PENDING: "Chờ thanh toán",
    PAID: "Đã thanh toán",
    CANCELLED: "Đã hủy",
    EXPIRED: "Đã hết hạn",
  })[value] || value;
export const isBookable = (show) =>
  show.status === "SCHEDULED" &&
  new Date(show.startTime).getTime() > Date.now();

// Movie-service owns status; venue-service owns the physical seat coordinates.
// booking-service nhận seatId (ghế vật lý), không phải showtimeSeatId: ghế trống
// chưa có bản ghi showtime_seats nên showtimeSeatId = null.
export function joinSeats(showtimeSeats, physicalSeats) {
  const physical = new Map(
    physicalSeats.map((seat) => [String(seat.id), seat]),
  );
  return showtimeSeats
    .map((item) => {
      const seat = physical.get(String(item.seatId));
      if (!seat || !seat.seatRow || !Number.isFinite(Number(seat.seatNumber))) {
        throw new Error(
          "Dữ liệu ghế giữa suất chiếu và phòng chưa khớp. Vui lòng kiểm tra lại dữ liệu phòng.",
        );
      }
      return { ...seat, ...item, label: `${seat.seatRow}${seat.seatNumber}` };
    })
    .sort(
      (a, b) =>
        a.seatRow.localeCompare(b.seatRow, "vi", { numeric: true }) ||
        Number(a.seatNumber) - Number(b.seatNumber),
    );
}

// booking-service lấy userId từ JWT và tự tính tổng tiền; client chỉ gửi
// suất chiếu, danh sách seatId (ghế vật lý) và phương thức thanh toán.
export function bookingPayload(
  show,
  seats,
  ids,
  paymentMethod = "BANK_TRANSFER",
) {
  if (!show || !isBookable(show))
    throw new Error("Suất chiếu đã bắt đầu hoặc không còn nhận đặt vé.");
  if (!ids.length || ids.length > 8 || new Set(ids).size !== ids.length)
    throw new Error("Vui lòng chọn từ 1 đến 8 ghế khác nhau.");
  const selected = ids.map((id) =>
    seats.find((seat) => seat.seatId === id),
  );
  if (
    selected.some(
      (seat) =>
        !seat ||
        !Number.isFinite(Number(seat.seatId)) ||
        seat.status !== "AVAILABLE" ||
        String(seat.showtimeId) !== String(show.showtimeId),
    )
  ) {
    throw new Error("Một số ghế không còn trống. Hãy tải lại sơ đồ ghế.");
  }
  const price = Number(show.basePrice);
  if (!Number.isFinite(price) || price <= 0)
    throw new Error("Suất chiếu chưa có giá vé hợp lệ.");
  if (!["CASH", "BANK_TRANSFER", "CARD", "E_WALLET"].includes(paymentMethod))
    throw new Error("Phương thức thanh toán không hợp lệ.");
  return {
    showtimeId: show.showtimeId,
    seatIds: ids,
    paymentMethod,
  };
}

// Nhận link watch/youtu.be/shorts/embed và trả URL nhúng cho iframe.
export function youtubeEmbed(url) {
  const match = String(url || "").match(
    /(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/|live\/)|youtu\.be\/)([A-Za-z0-9_-]{6,})/i,
  );
  return match ? `https://www.youtube.com/embed/${match[1]}` : "";
}

export function filterMovies(
  movies,
  { query = "", status = "SHOWING", genre = "" } = {},
) {
  const normalize = (value) =>
    value
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/đ/g, "d")
      .replace(/Đ/g, "D")
      .toLowerCase();
  return movies.filter(
    (movie) =>
      (!status || movie.status === status) &&
      (!genre ||
        movie.genres?.some((g) => String(g.genreId) === String(genre))) &&
      normalize(movie.title).includes(normalize(query.trim())),
  );
}
