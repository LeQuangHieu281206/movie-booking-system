import { joinSeats } from "../lib/domain.js";
import { createDemoApi } from "./demo.js";
import { randomId } from "../lib/randomId.js";

export function createHttpClient(fetchImpl = globalThis.fetch) {
  return async function request(path, { method = "GET", body, signal } = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15000);
    const abort = () => controller.abort();
    signal?.addEventListener("abort", abort, { once: true });
    if (signal?.aborted) controller.abort();
    try {
      const response = await fetchImpl(`/api${path}`, {
        method,
        signal: controller.signal,
        ...(body === undefined
          ? {}
          : {
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify(body),
            }),
      });
      const text = await response.text();
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
  };
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
    user: (userId, options) =>
      request(`/getUserById?id=${id(userId)}`, options),
    createCustomer: (fields) =>
      request("/createUser", {
        method: "POST",
        body: {
          fullName: fields.fullName.trim(),
          email: fields.email.trim(),
          phoneNumber: fields.phoneNumber.trim(),
          password: randomId(),
          role: "CUSTOMER",
        },
      }),
    bookings: (userId, options) =>
      list(`/getBookingsByUserId?userId=${id(userId)}`, options),
    booking: (bookingId, options) =>
      request(`/getBookingById?id=${id(bookingId)}`, options),
    createBooking: (body) =>
      request("/createBooking", { method: "POST", body }),
    cancel: (bookingId) =>
      request(`/updateBooking?id=${id(bookingId)}`, {
        method: "PUT",
        body: { status: "CANCELLED" },
      }),
    pay: (booking, simulateFailure = false) =>
      request("/createPayment", {
        method: "POST",
        body: {
          bookingId: booking.bookingId,
          userId: booking.userId,
          amount: booking.totalAmount,
          simulateFailure,
        },
      }),
  };
}

export function createApi(mode) {
  return mode === "demo" ? createDemoApi() : createLiveApi();
}
