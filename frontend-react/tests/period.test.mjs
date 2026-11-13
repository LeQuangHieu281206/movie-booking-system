import test from "node:test";
import assert from "node:assert/strict";
import {
  PERIOD_IDS,
  labelStep,
  periodOf,
  periodRange,
  pointLabel,
} from "../src/lib/period.js";

test("Ba kỳ xem doanh số: ngày, tháng, năm", () => {
  assert.deepEqual(PERIOD_IDS, ["day", "month", "year"]);
  assert.equal(periodOf("month").tab, "Tháng");
  assert.equal(periodOf("unknown").tab, "Ngày");
});

test("Mỗi kỳ là một đơn vị thời gian đang diễn ra", () => {
  assert.equal(periodOf("day").chart, "Doanh thu hôm nay");
  assert.equal(periodOf("month").chart, "Doanh thu tháng này");
  assert.equal(periodOf("year").chart, "Doanh thu năm nay");
  assert.deepEqual(
    PERIOD_IDS.map((id) => periodOf(id).unit),
    ["giờ", "ngày", "tháng"],
  );
});

test("Nhãn mốc rút gọn theo kiểu key của từng kỳ", () => {
  assert.equal(pointLabel("2026-11-12 09"), "09h");
  assert.equal(pointLabel("2026-11-06"), "06/11");
  assert.equal(pointLabel("2026-11"), "11/26");
  assert.equal(pointLabel(""), "");
});

test("Dải thời gian đúng định dạng từng kỳ", () => {
  assert.equal(periodRange("day", ["2026-11-12 00"]), "Hôm nay, 12/11/2026");
  assert.equal(periodRange("month", ["2026-11-01"]), "Tháng 11/2026");
  assert.equal(periodRange("year", ["2026-01"]), "Năm 2026");
});

test("Dải rỗng dùng mô tả mặc định của kỳ", () => {
  assert.equal(periodRange("day", []), "hôm nay");
  assert.equal(periodRange("month", []), "tháng này");
  assert.equal(periodRange("year", []), "năm nay");
});

test("Nhãn trục được giãn khi có nhiều mốc", () => {
  assert.equal(labelStep(12), 2);
  assert.equal(labelStep(24), 4);
  assert.equal(labelStep(1), 1);
});
