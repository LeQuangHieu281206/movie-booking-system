import test from "node:test";
import assert from "node:assert/strict";
import { createDemoApi } from "../src/services/demo.js";
import { bookingPayload } from "../src/lib/domain.js";
const storage = () => {
  const data = new Map();
  return {
    getItem: (key) => data.get(key),
    setItem: (key, value) => data.set(key, value),
  };
};
async function setup() {
  const api = createDemoApi(storage());
  const user = await api.createCustomer({
    fullName: "Khách thử",
    email: "test@example.com",
    phoneNumber: "",
  });
  const show = (await api.showtimes(1))[0];
  const seats = await api.seats(show);
  return { api, user, show, seats };
}
test("Luồng mẫu tạo vé PENDING → thanh toán → PAID và giữ ghế", async () => {
  const { api, user, show, seats } = await setup();
  const seat = seats.find((s) => s.status === "AVAILABLE");
  const booking = await api.createBooking(
    bookingPayload(user, show, seats, [seat.showtimeSeatId]),
  );
  assert.equal(booking.status, "PENDING");
  assert.equal(
    (await api.seats(show)).find(
      (s) => s.showtimeSeatId === seat.showtimeSeatId,
    ).status,
    "BOOKED",
  );
  await assert.rejects(
    api.createBooking(bookingPayload(user, show, seats, [seat.showtimeSeatId])),
    /không còn trống/,
  );
  await api.pay(booking);
  assert.equal((await api.booking(booking.bookingId)).status, "PAID");
  assert.equal((await api.bookings(user.userId)).length, 1);
});
test("Thanh toán thất bại hoặc hủy vé đang chờ trả ghế", async () => {
  for (const action of ["failed", "cancel"]) {
    const { api, user, show, seats } = await setup();
    const seat = seats.find((s) => s.status === "AVAILABLE");
    const booking = await api.createBooking(
      bookingPayload(user, show, seats, [seat.showtimeSeatId]),
    );
    if (action === "failed") await api.pay(booking, true);
    else await api.cancel(booking.bookingId);
    assert.equal((await api.booking(booking.bookingId)).status, "CANCELLED");
    assert.equal(
      (await api.seats(show)).find(
        (s) => s.showtimeSeatId === seat.showtimeSeatId,
      ).status,
      "AVAILABLE",
    );
  }
});
test("Dữ liệu mẫu và hồ sơ tồn tại qua lần khởi tạo adapter mới", async () => {
  const shared = storage(),
    first = createDemoApi(shared);
  const user = await first.createCustomer({
    fullName: "An",
    email: "an@example.com",
    phoneNumber: "",
  });
  assert.equal(
    (await createDemoApi(shared).user(user.userId)).email,
    "an@example.com",
  );
});
