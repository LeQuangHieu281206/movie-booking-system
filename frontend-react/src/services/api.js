import { joinSeats } from "../lib/domain.js";
import { readSession, refreshSession } from "./session.js";

export function createHttpClient(fetchImpl = globalThis.fetch) {
  async function send(path, { method = "GET", body, signal }, canRefresh) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15000);
    const abort = () => controller.abort();
    signal?.addEventListener("abort", abort, { once: true });
    if (signal?.aborted) controller.abort();
    try {
      const token = readSession()?.accessToken;
      const response = await fetchImpl(`/api${path}`, {
        method,
        signal: controller.signal,
        headers: {
          ...(body === undefined
            ? {}
            : { "Content-Type": "application/json" }),
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      });
      const text = await response.text();
      // Access token hết hạn (2 giờ): đổi bằng refresh token rồi gửi lại đúng một lần.
      if (
        response.status === 401 &&
        canRefresh &&
        !path.startsWith("/auth/") &&
        (await refreshSession(fetchImpl))?.accessToken
      )
        return send(path, { method, body, signal }, false);
      let data;
      try {
        data = text ? JSON.parse(text) : null;
      } catch {
        throw new Error(
          "Phản hồi không phải JSON. Kiểm tra Gateway và BACKEND_URL.",
        );
      }
      if (!response.ok || data?.success === false) {
        const details = data?.fieldErrors
          ? Object.values(data.fieldErrors).join(". ")
          : "";
        throw Object.assign(
          new Error(
            details ||
              data?.message ||
              `API báo lỗi HTTP ${response.status}. Kiểm tra dịch vụ backend tương ứng.`,
          ),
          { status: response.status },
        );
      }
      return data && typeof data.success === "boolean" && "data" in data
        ? data.data
        : data;
    } catch (error) {
      if (signal?.aborted)
        throw Object.assign(new Error("Đã hủy yêu cầu."), {
          name: "AbortError",
        });
      if (controller.signal.aborted)
        throw new Error(
          "Backend phản hồi quá lâu. Hãy kiểm tra kết nối trước khi thử lại.",
        );
      if (error instanceof TypeError)
        throw new Error(
          "Không kết nối được backend. Kiểm tra Gateway và các dịch vụ Java.",
        );
      throw error;
    } finally {
      clearTimeout(timer);
      signal?.removeEventListener("abort", abort);
    }
  }
  return (path, options = {}) => send(path, options, true);
}

export function createLiveApi(request = createHttpClient()) {
  const list = async (path, options) => {
    const result = await request(path, options);
    if (!Array.isArray(result))
      throw new Error("API trả dữ liệu danh sách không đúng định dạng.");
    return result;
  };
  const id = (value) => encodeURIComponent(value);
  return {
    movies: (options) => list("/getMovies", options),
    genres: (options) => list("/getGenres", options),
    cinemas: (options) => list("/getCinemas", options),
    showtimes: (movieId, options) =>
      list(`/getShowtimes${movieId ? `?movieId=${id(movieId)}` : ""}`, options),
    showtime: (showtimeId, options) =>
      request(`/getShowtimeById?id=${id(showtimeId)}`, options),
    async seats(show, options) {
      const [states, physical] = await Promise.all([
        list(`/getShowtimeSeats?showtimeId=${id(show.showtimeId)}`, options),
        list(`/getSeatsByRoomId?roomId=${id(show.roomId)}`, options),
      ]);
      return joinSeats(states, physical);
    },
    // Hồ sơ của chính mình (JWT). /getUserById chỉ dành cho ADMIN.
    me: (options) => request("/users/me", options),
    bookings: (userId, options) =>
      list(`/getBookingsByUserId?userId=${id(userId)}`, options),
    booking: (bookingId, options) =>
      request(`/getBookingById?id=${id(bookingId)}`, options),
    // userId và tổng tiền do booking-service tự tính từ JWT và giá suất chiếu.
    createBooking: (body) =>
      request("/createBooking", { method: "POST", body }),
    cancel: (bookingId) =>
      request(`/updateBooking?id=${id(bookingId)}`, {
        method: "PUT",
        body: { status: "CANCELLED" },
      }),
    // Trả { qrUrl, message } của SePay; booking chỉ PAID sau khi webhook chuyển khoản về.
    createPayment: (bookingId) =>
      request("/createPayment", { method: "POST", body: { bookingId } }),
  };
}

export function createApi() {
  return createLiveApi();
}
