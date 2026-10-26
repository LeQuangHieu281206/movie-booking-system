import test from "node:test";
import assert from "node:assert/strict";
import {
  prepareBooking,
  reconcileSeats,
  readInventory,
  assertBooking,
  assertCreatedBooking,
  assertUserBookings,
  filterTickets,
} from "../src/booking/flow.js";

const show = () => ({
  showtimeId: 7,
  movieId: 1,
  roomId: 3,
  startTime: new Date(Date.now() + 86400000).toISOString(),
  basePrice: 85000,
  status: "SCHEDULED",
  cinemaName: "Rạp thử",
  roomName: "Phòng 1",
});
const seats = () =>
  [1, 2, 3].map((id) => ({
    showtimeSeatId: id + 100,
    showtimeId: 7,
    seatId: id,
    seatRow: "A",
    seatNumber: id,
    label: `A${id}`,
    status: "AVAILABLE",
  }));
const booking = () => ({
  bookingId: 90,
  bookingCode: "BK-TEST",
  userId: 2,
  showtimeId: 7,
  totalAmount: 170000,
  status: "PENDING",
  bookingTime: new Date().toISOString(),
});
function args(latestShow = show(), latestSeats = seats()) {
  const original = { ...latestShow };
  return {
    api: { showtime: async () => latestShow, seats: async () => latestSeats },
    input: {
      user: { userId: 2 },
      show: original,
      seats: seats(),
      ids: [1, 2],
      paymentMethod: "BANK_TRANSFER",
    },
  };
}

test("Giữ ghế còn trống và bỏ ghế sai suất/BOOKED/HELD khi cập nhật", () => {
  const current = seats();
  current[1].status = "HELD";
  current[2].showtimeId = 8;
  assert.deepEqual(reconcileSeats([1, 2, 3, 1], current, 7), [1]);
});
test("Preflight dùng dữ liệu mới để tạo đúng payload", async () => {
  const { api, input } = args();
  const result = await prepareBooking(api, input);
  assert.equal(result.payload.showtimeId, 7);
  assert.deepEqual(result.payload.seatIds, [1, 2]);
  assert.equal(result.payload.paymentMethod, "BANK_TRANSFER");
  assert.equal(result.review, undefined);
});
test("Ghế bị chiếm yêu cầu chọn lại, giữ phần còn trống và chưa có payload để POST", async () => {
  const { api, input } = args();
  api.seats = async () =>
    seats().map((seat) => ({
      ...seat,
      status: seat.seatId === 2 ? "BOOKED" : "AVAILABLE",
    }));
  const result = await prepareBooking(api, input);
  assert.equal(result.review, "seats");
  assert.deepEqual(result.ids, [1]);
  assert.equal(result.payload, undefined);
  assert.match(result.message, /A2/);
});
test("Giá vé đổi yêu cầu xác nhận lại trên giá mới", async () => {
  const { api, input } = args();
  api.showtime = async () => ({ ...input.show, basePrice: 100000 });
  const result = await prepareBooking(api, input);
  assert.equal(result.review, "show");
  assert.equal(result.inventory.show.basePrice, 100000);
  assert.equal(result.payload, undefined);
});
test("Phòng hoặc giờ chiếu đổi phải kiểm tra lại trước POST", async () => {
  for (const patch of [
    { roomId: 8 },
    { startTime: new Date(Date.now() + 172800000).toISOString() },
  ]) {
    const { api, input } = args();
    api.showtime = async () => ({ ...input.show, ...patch });
    assert.equal((await prepareBooking(api, input)).review, "show");
  }
});
test("Suất bắt đầu hoặc bị hủy không được tạo payload", async () => {
  for (const patch of [
    { startTime: "2020-01-01T10:00:00Z" },
    { status: "CANCELLED" },
  ]) {
    const { api, input } = args();
    api.showtime = async () => ({ ...input.show, ...patch });
    assert.equal((await prepareBooking(api, input)).review, "closed");
  }
});
test("Bước chọn ghế có thể kiểm tra trước khi tạo hồ sơ khách", async () => {
  const { api, input } = args();
  const result = await prepareBooking(api, {
    ...input,
    user: null,
    reviewOnly: true,
  });
  assert.equal(result.payload, null);
  assert.equal(result.review, undefined);
});
test("API sai suất hoặc lẫn ghế khác suất phải dừng", async () => {
  await assert.rejects(
    readInventory({ showtime: async () => ({ ...show(), showtimeId: 9 }) }, 7),
    /sai suất/,
  );
  await assert.rejects(
    readInventory(
      {
        showtime: async () => show(),
        seats: async () => [{ ...seats()[0], showtimeId: 8 }],
      },
      7,
    ),
    /không thuộc/,
  );
});
test("Phản hồi booking sai chủ sở hữu hoặc sai ID không được xử lý tiếp", () => {
  assert.throws(
    () => assertUserBookings([{ ...booking(), userId: 99 }], 2),
    /không khớp/,
  );
  const owned = booking();
  assert.deepEqual(assertUserBookings([owned], 2), [owned]);
  const current = booking();
  assert.throws(
    () => assertBooking({ ...current, userId: 3 }, current),
    /không khớp/,
  );
  assert.throws(
    () => assertBooking({ ...current, bookingCode: "BK-OTHER" }, current),
    /không khớp/,
  );
  assert.throws(
    () =>
      assertCreatedBooking(
        { ...current, bookingId: null },
        { userId: 2, showtimeId: 7 },
      ),
    /Chưa xác nhận/,
  );
});
test("Lọc vé phối hợp trạng thái, mã, tên phim không dấu và sắp xếp", () => {
  const all = [
    { ...booking(), bookingId: 1, status: "PAID", bookingTime: "2026-01-01" },
    {
      ...booking(),
      bookingId: 2,
      bookingCode: "BK-NEW",
      bookingTime: "2026-02-01",
    },
  ];
  assert.deepEqual(
    filterTickets(all, {
      query: "dao may",
      status: "PAID",
      titleFor: () => "Đảo Mây",
    }).map((b) => b.bookingId),
    [1],
  );
  assert.deepEqual(
    filterTickets(all).map((b) => b.bookingId),
    [2, 1],
  );
  assert.deepEqual(
    filterTickets(all, { query: "BK-NEW", order: "oldest" }).map(
      (b) => b.bookingId,
    ),
    [2],
  );
});
