import test from "node:test";
import assert from "node:assert/strict";
import { createHttpClient, createLiveApi } from "../src/services/api.js";

function withStorage(run, session) {
  const map = new Map();
  if (session) map.set("cineroom-auth", JSON.stringify(session));
  const storage = {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
  };
  const original = globalThis.localStorage;
  globalThis.localStorage = storage;
  return Promise.resolve(run(storage)).finally(() => {
    if (original === undefined) delete globalThis.localStorage;
    else globalThis.localStorage = original;
  });
}

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
          fieldErrors: { seatIds: "Thiếu ghế" },
        }),
        { status: 400 },
      ),
  );
  await assert.rejects(
    request("/createBooking", { method: "POST", body: {} }),
    (e) => e.status === 400 && e.message === "Thiếu ghế",
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
test("Booking, thanh toán và hủy dùng đúng DTO của backend", async () => {
  const calls = [];
  const api = createLiveApi(async (path, options) => {
    calls.push({ path, ...options });
    return {};
  });
  const body = { showtimeId: 7, seatIds: [91], paymentMethod: "CASH" };
  await api.createBooking(body);
  await api.createPayment(4);
  await api.me();
  await api.cancel(4);
  assert.deepEqual(calls[0], { path: "/createBooking", method: "POST", body });
  assert.deepEqual(calls[1], {
    path: "/createPayment",
    method: "POST",
    body: { bookingId: 4 },
  });
  assert.equal(calls[2].path, "/users/me");
  assert.equal(calls[3].path, "/updateBooking?id=4");
  assert.deepEqual(calls[3].body, { status: "CANCELLED" });
});
test("Mọi request gắn Bearer token của phiên đăng nhập", () =>
  withStorage(
    async () => {
      const request = createHttpClient(async (url, options) => {
        assert.equal(options.headers.Authorization, "Bearer access-1");
        return new Response(JSON.stringify({ userId: 1 }));
      });
      assert.deepEqual(await createLiveApi(request).me(), { userId: 1 });
    },
    {
      accessToken: "access-1",
      refreshToken: "refresh-1",
      user: { userId: 1, fullName: "A", email: "a@b.c", role: "CUSTOMER" },
    },
  ));
test("401 tự đổi refresh token rồi gửi lại đúng một lần", () =>
  withStorage(
    async (storage) => {
      const calls = [];
      const request = createHttpClient(async (url, options) => {
        calls.push({ url, auth: options.headers?.Authorization });
        if (url === "/api/auth/refresh")
          return new Response(
            JSON.stringify({
              accessToken: "access-2",
              refreshToken: "refresh-2",
              user: { id: 1, fullName: "A", email: "a@b.c", role: "CUSTOMER" },
            }),
          );
        if (calls.length === 1)
          return new Response(JSON.stringify({ message: "Hết hạn" }), {
            status: 401,
          });
        return new Response(JSON.stringify({ userId: 1 }));
      });
      assert.deepEqual(await createLiveApi(request).me(), { userId: 1 });
      assert.deepEqual(
        calls.map((call) => call.url),
        ["/api/users/me", "/api/auth/refresh", "/api/users/me"],
      );
      assert.equal(calls[2].auth, "Bearer access-2");
      assert.match(storage.getItem("cineroom-auth"), /access-2/);
    },
    {
      accessToken: "access-1",
      refreshToken: "refresh-1",
      user: { userId: 1, fullName: "A", email: "a@b.c", role: "CUSTOMER" },
    },
  ));
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
