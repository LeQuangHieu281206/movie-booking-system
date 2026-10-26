import test from "node:test";
import assert from "node:assert/strict";
import {
  bookingPayload,
  joinSeats,
  filterMovies,
  dayKey,
  youtubeEmbed,
} from "../src/lib/domain.js";
const show = {
  showtimeId: 7,
  basePrice: 85000,
  status: "SCHEDULED",
  startTime: new Date(Date.now() + 86400000).toISOString(),
};
// Ghế trống thật không có bản ghi showtime_seats -> showtimeSeatId = null.
const seats = [
  { showtimeSeatId: null, seatId: 10, showtimeId: 7, status: "AVAILABLE" },
  { showtimeSeatId: 92, seatId: 11, showtimeId: 7, status: "BOOKED" },
];
test("Ghép vị trí ghế bằng seatId và gửi seatId khi đặt vé", () => {
  const merged = joinSeats(seats, [
    { id: 11, seatRow: "B", seatNumber: 2 },
    { id: 10, seatRow: "A", seatNumber: 3, seatType: "VIP" },
  ]);
  assert.equal(merged[0].label, "A3");
  assert.equal(merged[0].seatId, 10);
  assert.equal(merged[1].status, "BOOKED");
  assert.deepEqual(bookingPayload(show, merged, [10]), {
    showtimeId: 7,
    seatIds: [10],
    paymentMethod: "BANK_TRANSFER",
  });
});
test("Không dựng vị trí giả nếu thiếu metadata ghế", () =>
  assert.throws(() => joinSeats(seats, []), /chưa khớp/));
test("Từ chối ghế bị đặt, ghế trùng, ghế suất khác, không có ghế", () => {
  for (const ids of [[11], [10, 10], [], [100]])
    assert.throws(() => bookingPayload(show, seats, ids));
  assert.throws(() =>
    bookingPayload(show, [{ ...seats[0], showtimeId: 8 }], [10]),
  );
});
test("Từ chối suất đã bắt đầu, đã hủy, giá sai, thiếu suất chiếu", () => {
  assert.throws(() =>
    bookingPayload({ ...show, startTime: "2020-01-01T10:00:00Z" }, seats, [10]),
  );
  assert.throws(() =>
    bookingPayload({ ...show, status: "CANCELLED" }, seats, [10]),
  );
  assert.throws(() =>
    bookingPayload({ ...show, basePrice: 0 }, seats, [10]),
  );
  assert.throws(() => bookingPayload(null, seats, [10]));
});
test("Chuẩn hoá link YouTube sang URL nhúng, link khác bị bỏ", () => {
  assert.equal(
    youtubeEmbed("https://www.youtube.com/embed/Rvr68u6c5sI"),
    "https://www.youtube.com/embed/Rvr68u6c5sI",
  );
  assert.equal(
    youtubeEmbed("https://www.youtube.com/watch?v=4vPeTSRd580&t=10s"),
    "https://www.youtube.com/embed/4vPeTSRd580",
  );
  assert.equal(
    youtubeEmbed("https://youtu.be/sY1S34973zA"),
    "https://www.youtube.com/embed/sY1S34973zA",
  );
  assert.equal(
    youtubeEmbed("https://www.youtube.com/shorts/P5ieIbInFpg"),
    "https://www.youtube.com/embed/P5ieIbInFpg",
  );
  assert.equal(youtubeEmbed("https://vimeo.com/123"), "");
  assert.equal(youtubeEmbed(null), "");
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
