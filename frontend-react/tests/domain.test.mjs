import test from "node:test";
import assert from "node:assert/strict";
import {
  bookingPayload,
  joinSeats,
  filterMovies,
  dayKey,
} from "../src/lib/domain.js";
const show = {
  showtimeId: 7,
  basePrice: 85000,
  status: "SCHEDULED",
  startTime: new Date(Date.now() + 86400000).toISOString(),
};
const user = { userId: 2 };
const seats = [
  { showtimeSeatId: 91, seatId: 10, showtimeId: 7, status: "AVAILABLE" },
  { showtimeSeatId: 92, seatId: 11, showtimeId: 7, status: "BOOKED" },
];
test("Ghép vị trí ghế bằng seatId, giữ showtimeSeatId để đặt", () => {
  const merged = joinSeats(seats, [
    { id: 11, seatRow: "B", seatNumber: 2 },
    { id: 10, seatRow: "A", seatNumber: 3, seatType: "VIP" },
  ]);
  assert.equal(merged[0].label, "A3");
  assert.equal(merged[0].showtimeSeatId, 91);
  assert.equal(merged[1].status, "BOOKED");
  assert.deepEqual(bookingPayload(user, show, merged, [91]), {
    userId: 2,
    showtimeId: 7,
    showtimeSeatIds: [91],
    totalAmount: 85000,
    paymentMethod: "BANK_TRANSFER",
  });
});
test("Không dựng vị trí giả nếu thiếu metadata ghế", () =>
  assert.throws(() => joinSeats(seats, []), /chưa khớp/));
test("Từ chối ghế bị đặt, ghế trùng, ghế suất khác, không có ghế", () => {
  for (const ids of [[92], [91, 91], [], [100]])
    assert.throws(() => bookingPayload(user, show, seats, ids));
  assert.throws(() =>
    bookingPayload(user, show, [{ ...seats[0], showtimeId: 8 }], [91]),
  );
});
test("Từ chối suất đã bắt đầu, đã hủy, giá sai, khách chưa có mã", () => {
  assert.throws(() =>
    bookingPayload(
      user,
      { ...show, startTime: "2020-01-01T10:00:00Z" },
      seats,
      [91],
    ),
  );
  assert.throws(() =>
    bookingPayload(user, { ...show, status: "CANCELLED" }, seats, [91]),
  );
  assert.throws(() =>
    bookingPayload(user, { ...show, basePrice: 0 }, seats, [91]),
  );
  assert.throws(() => bookingPayload(null, show, seats, [91]));
});
test("Tìm phim không dấu, phối hợp thể loại và trạng thái", () => {
  const movies = [
    { title: "Đảo Mây", status: "SHOWING", genres: [{ genreId: 4 }] },
    { title: "Đảo Mây 2", status: "UPCOMING", genres: [{ genreId: 4 }] },
  ];
  assert.equal(
    filterMovies(movies, { query: "dao may", status: "SHOWING", genre: "4" })
      .length,
    1,
  );
  assert.equal(
    filterMovies(movies, { query: "dao", status: "SHOWING", genre: "2" })
      .length,
    0,
  );
});
test("Lịch chiếu lấy ngày Việt Nam kể cả qua nửa đêm UTC", () =>
  assert.equal(dayKey("2026-10-22T18:00:00Z"), "2026-10-23"));

test("Tạo mã ở môi trường HTTP không có crypto.randomUUID", async () => {
  const { randomId } = await import("../src/lib/randomId.js");
  const value = randomId({
    getRandomValues: (bytes) => {
      bytes.fill(170);
      return bytes;
    },
  });
  assert.match(
    value,
    /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/,
  );
});
