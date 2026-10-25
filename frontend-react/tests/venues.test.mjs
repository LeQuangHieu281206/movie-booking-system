import test from "node:test";
import assert from "node:assert/strict";
import {
  bookableShowtimes,
  cityList,
  groupByCinema,
  movieOptions,
  sameId,
  showtimeDays,
  telHref,
} from "../src/lib/venues.js";
import { createDemoApi } from "../src/services/demo.js";

const hours = (n) => new Date(Date.now() + n * 3600000).toISOString();
const show = (id, extra = {}) => ({
  showtimeId: id,
  movieId: 1,
  cinemaId: 1,
  cinemaName: "Rạp A",
  cinemaCity: "Hà Nội",
  status: "SCHEDULED",
  startTime: hours(5),
  ...extra,
});

test("So sánh ID không phân biệt number/string và không coi null là khớp", () => {
  assert.equal(sameId(3, "3"), true);
  assert.equal(sameId(3, 4), false);
  assert.equal(sameId(undefined, undefined), false);
  assert.equal(sameId(null, "null"), false);
});

test("Bỏ suất đã bắt đầu, bị hủy, đang chiếu, thiếu ID hoặc giờ lỗi; sắp theo giờ", () => {
  const input = [
    show(1, { startTime: hours(8) }),
    show(2, { startTime: hours(-1) }),
    show(3, { status: "CANCELLED" }),
    show(4, { status: "ONGOING" }),
    show(5, { showtimeId: null }),
    show(6, { startTime: "không phải giờ" }),
    show(7, { startTime: hours(2) }),
    null,
  ];
  const copy = structuredClone(input);
  assert.deepEqual(
    bookableShowtimes(input).map((s) => s.showtimeId),
    [7, 1],
  );
  assert.deepEqual(input, copy, "không được sửa mảng đầu vào");
  assert.deepEqual(bookableShowtimes(undefined), []);
});

test("Lọc theo rạp bằng ID, không tráo giữa rạp 1 và 10", () => {
  const input = [show(1), show(2, { cinemaId: 10 }), show(3, { cinemaId: 2 })];
  assert.deepEqual(
    bookableShowtimes(input, { cinemaId: "1" }).map((s) => s.showtimeId),
    [1],
  );
  assert.deepEqual(
    bookableShowtimes(input, { cinemaId: 10 }).map((s) => s.showtimeId),
    [2],
  );
});

test("Nhóm theo rạp giữ đúng showtimeId; thiếu cinemaId thì tách theo tên rạp", () => {
  const groups = groupByCinema([
    show(1),
    show(2, { cinemaId: 2, cinemaName: "Rạp B" }),
    show(3),
    show(4, { cinemaId: undefined, cinemaName: "Rạp C" }),
    show(5, { cinemaId: undefined, cinemaName: "Rạp D" }),
  ]);
  assert.equal(groups.length, 4);
  assert.deepEqual(
    groups[0].shows.map((s) => s.showtimeId),
    [1, 3],
  );
  assert.equal(groups[1].name, "Rạp B");
  assert.equal(groups[2].name, "Rạp C");
  assert.equal(groups[3].name, "Rạp D");
});

test("Ngày chiếu tính theo giờ Việt Nam, qua nửa đêm UTC vẫn đúng", () => {
  const days = showtimeDays([
    { startTime: "2099-10-22T16:59:00Z" }, // 23:59 ngày 22 (VN)
    { startTime: "2099-10-22T17:00:00Z" }, // 00:00 ngày 23 (VN)
    { startTime: "2099-10-23T01:00:00Z" },
  ]);
  assert.deepEqual(
    days.map((d) => d.key),
    ["2099-10-22", "2099-10-23"],
  );
});

test("Danh sách thành phố bỏ giá trị rỗng, bỏ trùng, sắp theo tiếng Việt", () => {
  assert.deepEqual(
    cityList([
      { city: "Hồ Chí Minh" },
      { city: "" },
      { city: null },
      { city: "Hà Nội" },
      { city: "Hà Nội" },
      {},
    ]),
    ["Hà Nội", "Hồ Chí Minh"],
  );
});

test("Ô chọn phim luôn có phim đang được chọn dù chưa chiếu hoặc chưa tải xong", () => {
  const movies = [
    { movieId: 1, title: "A", status: "SHOWING" },
    { movieId: 2, title: "B", status: "UPCOMING" },
  ];
  assert.deepEqual(
    movieOptions(movies, "").map((m) => m.id),
    [1],
  );
  assert.deepEqual(
    movieOptions(movies, "2").map((m) => m.title),
    ["A", "B"],
  );
  assert.equal(movieOptions([], "9")[0].title, "Phim #9");
});

test("Số điện thoại chỉ thành liên kết gọi khi đủ chữ số", () => {
  assert.equal(telHref("028 3822 1234"), "tel:02838221234");
  assert.equal(telHref("12"), "");
  assert.equal(telHref(null), "");
});

test("Dữ liệu mẫu: suất lấy từ lịch chiếu đều đặt được và thuộc đúng phim/rạp", async () => {
  const store = new Map();
  const api = createDemoApi({
    getItem: (k) => store.get(k),
    setItem: (k, v) => store.set(k, v),
  });
  const all = await api.showtimes();
  const forMovie = await api.showtimes(1);
  assert.ok(forMovie.length && forMovie.length < all.length);
  assert.ok(forMovie.every((s) => sameId(s.movieId, 1)));
  const usable = bookableShowtimes(all);
  assert.equal(usable.length, all.length);
  const first = usable[0];
  assert.equal((await api.showtime(first.showtimeId)).cinemaId, first.cinemaId);
});
