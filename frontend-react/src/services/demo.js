import { movies, genres, cinemas } from "./demo-data.js";
import { bookingPayload } from "../lib/domain.js";
import { randomId } from "../lib/randomId.js";

const KEY = "cineroom-react-demo-v1";
export function createDemoApi(storage = globalThis.localStorage) {
  const clone = (value) => structuredClone(value);
  function seed() {
    const showtimes = [];
    for (let day = 1; day <= 6; day++)
      for (const movie of movies.filter((m) => m.status === "SHOWING"))
        for (const cinema of cinemas)
          for (const hour of [15, 19, 21]) {
            const start = new Date();
            start.setUTCDate(start.getUTCDate() + day);
            start.setUTCHours(hour - 7, 30, 0, 0);
            showtimes.push({
              showtimeId: showtimes.length + 1,
              movieId: movie.movieId,
              roomId: cinema.cinemaId * 10 + movie.movieId,
              roomName: `Phòng ${movie.movieId}`,
              roomType: movie.movieId === 1 ? "IMAX" : "2D",
              cinemaId: cinema.cinemaId,
              cinemaName: cinema.name,
              cinemaAddress: cinema.address,
              cinemaCity: cinema.city,
              startTime: start.toISOString(),
              endTime: new Date(
                +start + movie.durationMinutes * 60000,
              ).toISOString(),
              basePrice: movie.movieId === 1 ? 110000 : 85000,
              status: "SCHEDULED",
            });
          }
    return {
      created: Date.now(),
      showtimes,
      users: [],
      bookings: [],
      seatIds: {},
      nextUser: 1,
      nextBooking: 1,
    };
  }
  const save = (data) => storage.setItem(KEY, JSON.stringify(data));
  function load() {
    try {
      const data = JSON.parse(storage.getItem(KEY));
      if (data?.created && Date.now() - data.created < 86400000) return data;
    } catch {
      /* Start a separate sample dataset. */
    }
    const data = seed();
    save(data);
    return data;
  }
  function find(items, key, value, message) {
    const item = items.find((entry) => Number(entry[key]) === Number(value));
    if (!item) throw new Error(message);
    return item;
  }
  function seats(data, show) {
    return Array.from({ length: 64 }, (_, index) => {
      const seatId = index + 1,
        showtimeSeatId = show.showtimeId * 100 + seatId;
      const seatRow = "ABCDEFGH"[Math.floor(index / 8)],
        seatNumber = (index % 8) + 1;
      const reserved = data.bookings.some(
        (b) =>
          ["PENDING", "PAID"].includes(b.status) &&
          (data.seatIds[b.bookingId] || []).includes(showtimeSeatId),
      );
      return {
        seatId,
        showtimeSeatId,
        showtimeId: show.showtimeId,
        seatRow,
        seatNumber,
        label: `${seatRow}${seatNumber}`,
        seatType: index >= 24 && index < 48 ? "VIP" : "NORMAL",
        status:
          reserved || [10, 11, 27, 35].includes(index) ? "BOOKED" : "AVAILABLE",
      };
    });
  }
  return {
    async movies() {
      return clone(movies);
    },
    async genres() {
      return clone(genres);
    },
    async cinemas() {
      return clone(cinemas);
    },
    async showtimes(movieId) {
      return load().showtimes.filter(
        (s) => !movieId || Number(s.movieId) === Number(movieId),
      );
    },
    async showtime(showtimeId) {
      return find(
        load().showtimes,
        "showtimeId",
        showtimeId,
        "Không tìm thấy suất chiếu mẫu.",
      );
    },
    async seats(show) {
      const data = load();
      return seats(
        data,
        find(
          data.showtimes,
          "showtimeId",
          show.showtimeId,
          "Không tìm thấy suất chiếu mẫu.",
        ),
      );
    },
    async user(userId) {
      return find(
        load().users,
        "userId",
        userId,
        "Không tìm thấy mã khách hàng trong dữ liệu mẫu.",
      );
    },
    async userByEmail(email) {
      const found = load().users.find(
        (u) => u.email.toLowerCase() === String(email).trim().toLowerCase(),
      );
      if (!found) throw new Error("Không tìm thấy khách hàng theo email.");
      return found;
    },
    async createCustomer(fields) {
      const data = load();
      if (
        data.users.some(
          (u) => u.email.toLowerCase() === fields.email.trim().toLowerCase(),
        )
      )
        throw new Error(
          "Email đã tồn tại trong dữ liệu mẫu. Hãy dùng mã khách hàng đã tạo.",
        );
      const user = {
        userId: data.nextUser++,
        fullName: fields.fullName.trim(),
        email: fields.email.trim(),
        phoneNumber: fields.phoneNumber.trim(),
        role: "CUSTOMER",
      };
      data.users.push(user);
      save(data);
      return user;
    },
    async bookings(userId) {
      return load()
        .bookings.filter((b) => Number(b.userId) === Number(userId))
        .reverse();
    },
    async booking(bookingId) {
      return find(
        load().bookings,
        "bookingId",
        bookingId,
        "Không tìm thấy vé mẫu.",
      );
    },
    async createBooking(body) {
      const data = load(),
        user = find(
          data.users,
          "userId",
          body.userId,
          "Không tìm thấy khách hàng.",
        );
      const show = find(
        data.showtimes,
        "showtimeId",
        body.showtimeId,
        "Không tìm thấy suất chiếu.",
      );
      const validated = bookingPayload(
        user,
        show,
        seats(data, show),
        body.showtimeSeatIds,
        body.paymentMethod,
      );
      const booking = {
        bookingId: data.nextBooking++,
        userId: user.userId,
        showtimeId: show.showtimeId,
        bookingCode: `DEMO-${randomId().slice(0, 8).toUpperCase()}`,
        totalAmount: validated.totalAmount,
        paymentMethod: body.paymentMethod,
        status: "PENDING",
        bookingTime: new Date().toISOString(),
      };
      data.bookings.push(booking);
      data.seatIds[booking.bookingId] = body.showtimeSeatIds;
      save(data);
      return booking;
    },
    async cancel(bookingId) {
      const data = load(),
        booking = find(
          data.bookings,
          "bookingId",
          bookingId,
          "Không tìm thấy vé.",
        );
      if (booking.status !== "PENDING")
        throw new Error("Chỉ có thể hủy vé đang chờ thanh toán.");
      booking.status = "CANCELLED";
      save(data);
      return booking;
    },
    async pay(booking, simulateFailure = false) {
      const data = load(),
        item = find(
          data.bookings,
          "bookingId",
          booking.bookingId,
          "Không tìm thấy vé.",
        );
      if (item.status !== "PENDING")
        throw new Error("Vé không còn ở trạng thái chờ thanh toán.");
      item.status = simulateFailure ? "CANCELLED" : "PAID";
      save(data);
      return {
        bookingId: item.bookingId,
        status: simulateFailure ? "FAILED" : "SUCCESS",
      };
    },
  };
}

// Đảm bảo user đăng nhập qua trang Tài khoản tồn tại trong cùng kho dữ liệu
// mẫu với đặt vé, để createBooking tìm thấy khách hàng. Trả về user trong kho.
export function ensureDemoUser({ email, fullName, phoneNumber = "" }) {
  const api = createDemoApi();
  return api
    .userByEmail(email)
    .catch(() => api.createCustomer({ fullName, email, phoneNumber }));
}
