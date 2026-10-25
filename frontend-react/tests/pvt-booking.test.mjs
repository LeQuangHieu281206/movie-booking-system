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
  receiptText,
} from "../src/booking/flow.js";
import { createBookingStore } from "../src/booking/storage.js";
import { runBookingAction } from "../src/booking/payment.js";

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
const memoryStorage = () => {
  const data = new Map();
  return {
    getItem: (key) => data.get(key) || null,
    setItem: (key, value) => data.set(key, value),
    removeItem: (key) => data.delete(key),
  };
};
function args(latestShow = show(), latestSeats = seats()) {
  const original = { ...latestShow };
  return {
    api: { showtime: async () => latestShow, seats: async () => latestSeats },
    input: {
      user: { userId: 2 },
      show: original,
      seats: seats(),
      ids: [101, 102],
      paymentMethod: "BANK_TRANSFER",
    },
  };
}

test("Giữ ghế còn trống và bỏ ghế sai suất/BOOKED/HELD khi cập nhật", () => {
  const current = seats();
  current[1].status = "HELD";
  current[2].showtimeId = 8;
  assert.deepEqual(reconcileSeats([101, 102, 103, 101], current, 7), [101]);
});
test("Preflight dùng dữ liệu mới để tạo đúng payload", async () => {
  const { api, input } = args();
  const result = await prepareBooking(api, input);
  assert.equal(result.payload.totalAmount, 170000);
  assert.deepEqual(result.payload.showtimeSeatIds, [101, 102]);
  assert.equal(result.review, undefined);
});
test("Ghế bị chiếm yêu cầu chọn lại, giữ phần còn trống và chưa có payload để POST", async () => {
  const { api, input } = args();
  api.seats = async () =>
    seats().map((seat) => ({
      ...seat,
      status: seat.showtimeSeatId === 102 ? "BOOKED" : "AVAILABLE",
    }));
  const result = await prepareBooking(api, input);
  assert.equal(result.review, "seats");
  assert.deepEqual(result.ids, [101]);
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
test("Bản ghi ghế chỉ khớp đúng mã booking, người dùng và chế độ", () => {
  const store = createBookingStore(memoryStorage()),
    b = booking();
  store.saveReceipt("live", b, show(), { title: "Phim thử" }, seats());
  assert.equal(store.receipt("live", b).seats[0].label, "A1");
  assert.equal(store.receipt("demo", b), null);
  assert.equal(store.receipt("live", { ...b, bookingCode: "BK-NEW" }), null);
  assert.equal(store.receipt("live", { ...b, userId: 9 }), null);
});
test("Trình duyệt chặn storage không làm vỡ luồng lưu bản ghi", () => {
  const store = createBookingStore({
      getItem() {
        throw Error("blocked");
      },
      setItem() {
        throw Error("blocked");
      },
      removeItem() {
        throw Error("blocked");
      },
    }),
    b = booking();
  assert.equal(store.saveReceipt("live", b, show(), null, seats()), false);
  assert.equal(store.receipt("live", b).seats.length, 3);
  store.markAction("live", b, "pay");
  assert.equal(store.action("live", b).kind, "pay");
});
test("Bản ghi tải xuống không giả làm vé thật hoặc tự bổ sung ghế không có", () => {
  const text = receiptText(booking(), null);
  assert.match(text, /Bản ghi tham khảo/);
  assert.match(text, /Không có bản ghi ghế/);
  assert.doesNotMatch(text, /Ghế theo bản ghi/);
});
test("Ghi nhớ tạo booking chưa rõ kết quả, xóa khi người dùng đã đối chiếu", () => {
  const storage = memoryStorage(),
    a = createBookingStore(storage);
  a.markCreation("live", 2, 7);
  const b = createBookingStore(storage);
  assert.ok(b.creation("live", 2, 7));
  assert.equal(b.creation("demo", 2, 7), null);
  b.clearCreation("live", 2, 7);
  assert.equal(b.creation("live", 2, 7), null);
});
test("Payment SUCCESS nhưng booking vẫn PENDING thì không tự báo PAID", async () => {
  const b = booking(),
    store = createBookingStore(memoryStorage());
  let posted = 0;
  const result = await runBookingAction({
    api: {
      booking: async () => b,
      pay: async () => {
        posted++;
        return { status: "SUCCESS" };
      },
    },
    mode: "live",
    booking: b,
    kind: "pay",
    store,
    attempts: 2,
    wait: async () => {},
  });
  assert.equal(result.status, "PENDING");
  assert.equal(posted, 1);
  assert.equal(store.action("live", b).kind, "pay");
});
test("Payment chỉ hoàn tất khi getBookingById thực sự trả PAID", async () => {
  const b = booking(),
    store = createBookingStore(memoryStorage());
  let reads = 0;
  const api = {
    booking: async () => ({ ...b, status: ++reads >= 3 ? "PAID" : "PENDING" }),
    pay: async () => ({ status: "SUCCESS" }),
  };
  const result = await runBookingAction({
    api,
    mode: "live",
    booking: b,
    kind: "pay",
    store,
    wait: async () => {},
  });
  assert.equal(result.status, "PAID");
  assert.equal(store.action("live", b), null);
});
test("Booking đã PAID trước khi bấm thì không gửi payment", async () => {
  const b = booking(),
    store = createBookingStore(memoryStorage());
  let posted = 0;
  const result = await runBookingAction({
    api: {
      booking: async () => ({ ...b, status: "PAID" }),
      pay: async () => {
        posted++;
      },
    },
    mode: "live",
    booking: b,
    kind: "pay",
    store,
  });
  assert.equal(result.status, "PAID");
  assert.equal(posted, 0);
});
test("Mất phản hồi POST payment giữ dấu lần gửi và chặn gửi lại sau remount", async () => {
  const b = booking(),
    storage = memoryStorage(),
    store = createBookingStore(storage);
  let posted = 0;
  const api = {
    booking: async () => b,
    pay: async () => {
      posted++;
      throw Error("Mất kết nối");
    },
  };
  await assert.rejects(
    runBookingAction({ api, mode: "live", booking: b, kind: "pay", store }),
    /Mất kết nối/,
  );
  await assert.rejects(
    runBookingAction({
      api,
      mode: "live",
      booking: b,
      kind: "pay",
      store: createBookingStore(storage),
    }),
    /không gửi lặp/,
  );
  assert.equal(posted, 1);
});
test("Dấu thanh toán không áp dụng nhầm cho booking demo tái sử dụng ID", () => {
  const store = createBookingStore(memoryStorage()),
    b = booking();
  store.markAction("demo", b, "pay");
  assert.equal(store.action("demo", { ...b, bookingCode: "DEMO-NEW" }), null);
});
test("Hủy vé gửi đúng một lần, tin trạng thái backend trả về và xóa dấu khi CANCELLED", async () => {
  const b = booking(),
    store = createBookingStore(memoryStorage());
  let cancelled = 0;
  const result = await runBookingAction({
    api: {
      booking: async () => b,
      cancel: async (id) => {
        assert.equal(id, b.bookingId);
        cancelled++;
        return { ...b, status: "CANCELLED" };
      },
    },
    mode: "live",
    booking: b,
    kind: "cancel",
    store,
  });
  assert.equal(cancelled, 1);
  assert.equal(result.status, "CANCELLED");
  assert.equal(store.action("live", b), null);
});
test("Khóa thao tác đồng thời trên cùng booking trong ứng dụng", async () => {
  const b = { ...booking(), bookingId: 91 },
    store = createBookingStore(memoryStorage());
  let releaseRead;
  const gate = new Promise((resolve) => (releaseRead = resolve));
  const first = runBookingAction({
    api: {
      booking: async () => {
        await gate;
        return { ...b, status: "PAID" };
      },
    },
    mode: "live",
    booking: b,
    kind: "pay",
    store,
  });
  await assert.rejects(
    runBookingAction({
      api: { booking: async () => b },
      mode: "live",
      booking: b,
      kind: "cancel",
      store,
    }),
    /đang được xử lý/,
  );
  releaseRead();
  await first;
});
test("Sai response của GET trước payment thì không được POST", async () => {
  const b = booking(),
    store = createBookingStore(memoryStorage());
  let posted = 0;
  await assert.rejects(
    runBookingAction({
      api: {
        booking: async () => ({ ...b, userId: 99 }),
        pay: async () => posted++,
      },
      mode: "live",
      booking: b,
      kind: "pay",
      store,
    }),
    /không khớp/,
  );
  assert.equal(posted, 0);
  assert.equal(store.action("live", b), null);
});
test("Unmount dừng poll nhưng vẫn giữ dấu payment đã gửi", async () => {
  const b = booking(),
    store = createBookingStore(memoryStorage()),
    controller = new AbortController();
  let posted = 0;
  await assert.rejects(
    runBookingAction({
      api: {
        booking: async () => b,
        pay: async () => {
          posted++;
          controller.abort();
        },
      },
      mode: "live",
      booking: b,
      kind: "pay",
      store,
      signal: controller.signal,
    }),
    (e) => e.name === "AbortError",
  );
  assert.equal(posted, 1);
  assert.ok(store.action("live", b));
});
