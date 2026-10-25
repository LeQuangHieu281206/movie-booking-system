import test from "node:test";
import assert from "node:assert/strict";
import { createDemoApi } from "../src/services/demo.js";

// Demo auth nằm trong auth.js nhưng dùng ensureDemoUser của demo.js; kiểm tra
// luồng chính: user tạo qua ensureDemoUser phải đặt vé được bằng createBooking.
function fakeStorage() {
  const map = new Map();
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
  };
}

test("ensureDemoUser cấp user trong kho đặt vé demo, createBooking tìm thấy", async () => {
  const { localStorage } = globalThis;
  const storage = fakeStorage();
  globalThis.localStorage = storage;
  try {
    const { ensureDemoUser } = await import("../src/services/demo.js");
    const user = await ensureDemoUser({
      email: "auth@test.vn",
      fullName: "Auth Tester",
      phoneNumber: "09",
    });
    assert.ok(user.userId >= 1);
    assert.equal(user.email, "auth@test.vn");

    const api = createDemoApi();
    // User này phải lấy lại được qua api.user (cùng kho).
    const found = await api.user(user.userId);
    assert.equal(found.email, "auth@test.vn");

    // Đặt vé với userId vừa cấp phải không báo "Không tìm thấy khách hàng".
    const showtimes = await api.showtimes();
    const seats = await api.seats(showtimes[0]);
    const booking = await api.createBooking({
      userId: user.userId,
      showtimeId: showtimes[0].showtimeId,
      showtimeSeatIds: seats.slice(0, 1).map((s) => s.showtimeSeatId),
      paymentMethod: "CASH",
    });
    assert.equal(booking.userId, user.userId);
    assert.equal(booking.status, "PENDING");

    // ensureDemoUser lặp lại phải trả user cũ (không tạo trùng).
    const again = await ensureDemoUser({ email: "auth@test.vn", fullName: "x" });
    assert.equal(again.userId, user.userId);
  } finally {
    if (localStorage === undefined) delete globalThis.localStorage;
    else globalThis.localStorage = localStorage;
  }
});
