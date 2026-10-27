import test from "node:test";
import assert from "node:assert/strict";
import { createHttpClient, createLiveApi } from "../src/services/api.js";
test("Đọc JSON thô và ApiResponse wrapper", async () => {
  for (const data of [
    [{ movieId: 1 }],
    { success: true, data: [{ movieId: 1 }] },
  ]) {
    const request = createHttpClient(async (url, options) => {
      assert.equal(url, "/api/getMovies");
      assert.equal(options.method, "GET");
      return new Response(JSON.stringify(data));
    });
    assert.deepEqual(await createLiveApi(request).movies(), [{ movieId: 1 }]);
  }
});
test("Lỗi validation được hiển thị, không dùng dữ liệu mẫu thay thế", async () => {
  const request = createHttpClient(
    async () =>
      new Response(
        JSON.stringify({
          message: "Không hợp lệ",
          fieldErrors: { userId: "Thiếu khách hàng" },
        }),
        { status: 400 },
      ),
  );
  await assert.rejects(
    request("/createBooking", { method: "POST", body: {} }),
    (e) => e.status === 400 && e.message === "Thiếu khách hàng",
  );
});
test("Phản hồi HTML không bị hiểu nhầm thành thành công", async () => {
  const request = createHttpClient(
    async () => new Response("<html>Error</html>", { status: 502 }),
  );
  await assert.rejects(request("/getMovies"), /không phải JSON/);
});
test("Ghế phải gọi 2 API public và ghép bằng đúng ID", async () => {
  const paths = [];
  const api = createLiveApi(async (path) => {
    paths.push(path);
    return path.includes("getShowtimeSeats")
      ? [{ showtimeSeatId: 201, showtimeId: 2, seatId: 9, status: "AVAILABLE" }]
      : [{ id: 9, seatRow: "C", seatNumber: 4, seatType: "VIP" }];
  });
  assert.equal((await api.seats({ showtimeId: 2, roomId: 3 }))[0].label, "C4");
  assert.deepEqual(
    paths.sort(),
    ["/getSeatsByRoomId?roomId=3", "/getShowtimeSeats?showtimeId=2"].sort(),
  );
});
test("Đặt vé và thanh toán dùng DTO và đường dẫn SOA, không gọi bridge cũ", async () => {
  const calls = [];
  const api = createLiveApi(async (path, options) => {
    calls.push({ path, ...options });
    return {};
  });
  const body = {
    userId: 3,
    showtimeId: 7,
    showtimeSeatIds: [91],
    totalAmount: 85000,
    paymentMethod: "CASH",
  };
  await api.createBooking(body);
  await api.pay({ bookingId: 4, userId: 3, totalAmount: 85000 }, true);
  await api.cancel(4);
  assert.deepEqual(calls[0], { path: "/createBooking", method: "POST", body });
  assert.deepEqual(calls[1].body, {
    bookingId: 4,
    userId: 3,
    amount: 85000,
    simulateFailure: true,
  });
  assert.equal(calls[2].path, "/updateBooking?id=4");
  assert.deepEqual(calls[2].body, { status: "CANCELLED" });
});
test("Tạo hồ sơ luôn CUSTOMER, không lấy danh sách người dùng", async () => {
  let sent;
  const api = createLiveApi(async (path, options) => {
    sent = { path, ...options };
    return { userId: 1 };
  });
  await api.createCustomer({
    fullName: " An ",
    email: " an@example.com ",
    phoneNumber: " 0123456789 ",
    role: "ADMIN",
  });
  assert.equal(sent.path, "/createUser");
  assert.equal(sent.body.role, "CUSTOMER");
  assert.equal(sent.body.fullName, "An");
  assert.ok(sent.body.password.length >= 32);
});
test("Không tự gửi lại POST khi kết nối lỗi", async () => {
  let count = 0;
  const request = createHttpClient(async () => {
    count++;
    throw new TypeError("offline");
  });
  await assert.rejects(
    request("/createBooking", { method: "POST", body: {} }),
    /Không kết nối/,
  );
  assert.equal(count, 1);
});
