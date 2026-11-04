import test from "node:test";
import assert from "node:assert/strict";
import { createHttpClient, createLiveApi } from "../src/services/api.js";
import {
  ADMIN_RESOURCES,
  fromDateTimeInput,
  toDateTimeInput,
} from "../src/lib/admin.js";

const resource = (id) => ADMIN_RESOURCES.find((item) => item.id === id);

test("Admin CRUD gọi đúng path, method và id trên query", async () => {
  const calls = [];
  const api = createLiveApi(async (path, options = {}) => {
    calls.push({ path, options });
    return path.startsWith("/get") ? [] : {};
  });
  await api.admin.list("/getGenres");
  await api.admin.create("/createGenre", { name: "A" });
  await api.admin.update("/updateGenre", 7, { name: "B" });
  await api.admin.remove("/deleteGenre", 7);
  assert.deepEqual(
    calls.map((call) => call.path),
    ["/getGenres", "/createGenre", "/updateGenre?id=7", "/deleteGenre?id=7"],
  );
  assert.equal(calls[2].options.method, "PUT");
  assert.equal(calls[3].options.method, "DELETE");
});

test("Admin đọc thống kê qua ApiResponse wrapper", async () => {
  const request = createHttpClient(
    async () =>
      new Response(
        JSON.stringify({
          success: true,
          data: { revenue: 123000, paidBookings: 2, ticketsSold: 5 },
        }),
        { status: 200, headers: { "content-type": "application/json" } },
      ),
  );
  const stats = await createLiveApi(request).admin.get("/getBookingStats");
  assert.deepEqual(stats, {
    revenue: 123000,
    paidBookings: 2,
    ticketsSold: 5,
  });
});

test("Xoá trả text/plain vẫn được coi là thành công", async () => {
  const request = createHttpClient(
    async () =>
      new Response("User deleted successfully", {
        status: 200,
        headers: { "content-type": "text/plain;charset=UTF-8" },
      }),
  );
  assert.equal(
    await request("/deleteUser?id=1", { method: "DELETE" }),
    "User deleted successfully",
  );
});

test("Payload phim chuyển số, null và thể loại đúng kiểu", () => {
  const payload = resource("movies").toPayload({
    title: "  Dune  ",
    description: "",
    durationMinutes: "166",
    language: "EN",
    releaseDate: "2026-11-04",
    posterUrl: "",
    trailerUrl: "",
    ageRestriction: "T16",
    status: "SHOWING",
    director: "",
    actors: "A, B",
    genreIds: ["1", 2],
  });
  assert.equal(payload.title, "Dune");
  assert.equal(payload.description, null);
  assert.equal(payload.durationMinutes, 166);
  assert.deepEqual(payload.genreIds, [1, 2]);
});

test("Suất chiếu chuyển datetime-local sang ISO và giữ giờ địa phương", () => {
  const form = resource("showtimes").toForm({
    movieId: 1,
    roomId: 2,
    startTime: "2026-11-04T12:30:00+07:00",
    endTime: "2026-11-04T14:30:00+07:00",
    basePrice: 90000,
    status: "SCHEDULED",
  });
  assert.match(form.startTime, /^2026-11-04T\d{2}:30$/);
  const payload = resource("showtimes").toPayload(form);
  assert.equal(payload.movieId, 1);
  assert.equal(payload.roomId, 2);
  assert.equal(new Date(payload.startTime).getTime(), new Date(form.startTime).getTime());
  assert.match(payload.startTime, /Z$/);
});

test("Sửa người dùng không gửi email và mật khẩu rỗng thành null", () => {
  const payload = resource("users").toPayload(
    {
      fullName: "Quản trị",
      email: "admin@example.com",
      password: "",
      phoneNumber: "",
      role: "ADMIN",
    },
    true,
  );
  assert.equal("email" in payload, false);
  assert.equal(payload.password, null);
  assert.equal(payload.phoneNumber, null);
  assert.equal(payload.role, "ADMIN");
});

test("Ghế chuẩn hoá hàng ghế và số ghế", () => {
  const payload = resource("seats").toPayload({
    roomId: "3",
    seatRow: " a ",
    seatNumber: "12",
    seatType: "VIP",
  });
  assert.deepEqual(payload, {
    roomId: 3,
    seatRow: "A",
    seatNumber: 12,
    seatType: "VIP",
  });
});

test("Chuyển đổi datetime input qua lại không lệch phút", () => {
  const value = fromDateTimeInput("2026-11-04T19:30");
  assert.equal(toDateTimeInput(value), "2026-11-04T19:30");
});
