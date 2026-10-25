import test from "node:test";
import assert from "node:assert/strict";
import {
  customerErrorMessage,
  normalizeCustomerFields,
  parseCustomerId,
  toCustomerProfile,
} from "../src/lib/customer.js";
import { createLiveApi } from "../src/services/api.js";
import { createDemoApi } from "../src/services/demo.js";

test("Mã khách hàng: nhận số nguyên dương và dạng #12, từ chối phần còn lại", () => {
  assert.equal(parseCustomerId(" 12 "), 12);
  assert.equal(parseCustomerId("#7"), 7);
  for (const bad of ["", "0", "-3", "1.5", "abc", "1e3", "12abc", "99999999999999999999", null])
    assert.throws(() => parseCustomerId(bad), /số nguyên dương/, String(bad));
});

test("Thông tin tạo hồ sơ được cắt khoảng trắng và kiểm tra", () => {
  assert.deepEqual(
    normalizeCustomerFields({
      fullName: "  Nguyễn   Văn  An ",
      email: " an@example.com ",
      phoneNumber: " 0901 234 567 ",
    }),
    { fullName: "Nguyễn Văn An", email: "an@example.com", phoneNumber: "0901 234 567" },
  );
  assert.equal(
    normalizeCustomerFields({ fullName: "An", email: "a@b.vn" }).phoneNumber,
    "",
  );
  assert.throws(() => normalizeCustomerFields({ fullName: "   ", email: "a@b.vn" }), /họ và tên/);
  assert.throws(() => normalizeCustomerFields({ fullName: "An", email: "an@" }), /Email/);
  assert.throws(() => normalizeCustomerFields({ fullName: "An", email: "a b@c.vn" }), /Email/);
  assert.throws(
    () => normalizeCustomerFields({ fullName: "An", email: "a@b.vn", phoneNumber: "abc" }),
    /điện thoại/,
  );
  assert.throws(
    () => normalizeCustomerFields({ fullName: "An", email: "a@b.vn", phoneNumber: "123" }),
    /điện thoại/,
  );
});

test("Hồ sơ trả về phải có userId thật, khớp mã đã nhập, không phải ADMIN", () => {
  const profile = toCustomerProfile(
    { userId: "5", fullName: " An ", email: "an@example.com", role: "CUSTOMER" },
    5,
  );
  assert.equal(profile.userId, 5);
  assert.equal(profile.fullName, "An");
  assert.throws(() => toCustomerProfile({ fullName: "An" }), /mã khách hàng hợp lệ/);
  assert.throws(() => toCustomerProfile({ userId: 0 }), /mã khách hàng hợp lệ/);
  assert.throws(() => toCustomerProfile({ userId: 6 }, 5), /không khớp/);
  assert.throws(() => toCustomerProfile({ userId: 1, role: "admin" }), /không phải hồ sơ khách hàng/);
});

test("Thiếu tên/email vẫn không làm hỏng giao diện (header gọi fullName.split)", () => {
  assert.equal(toCustomerProfile({ userId: 3, fullName: null, email: "x@y.vn" }).fullName, "x@y.vn");
  const bare = toCustomerProfile({ userId: 3 });
  assert.equal(bare.fullName, "Khách #3");
  assert.equal(bare.email, "");
  assert.doesNotThrow(() => bare.fullName.split(" ").at(-1));
});

test("Thông báo lỗi: email trùng, mã không tồn tại, lỗi khác giữ nguyên", () => {
  assert.match(customerErrorMessage(Object.assign(new Error("x"), { status: 409 }), "create"), /đã có hồ sơ/);
  assert.match(customerErrorMessage(new Error("Email already exists"), "create"), /đã có hồ sơ/);
  assert.match(customerErrorMessage(Object.assign(new Error("x"), { status: 404 }), "lookup"), /Không tìm thấy/);
  assert.match(customerErrorMessage(new Error("User not found"), "lookup"), /Không tìm thấy/);
  assert.equal(customerErrorMessage(new Error("Backend phản hồi quá lâu."), "create"), "Backend phản hồi quá lâu.");
  assert.equal(customerErrorMessage(null, "lookup"), "Có lỗi xảy ra. Vui lòng thử lại.");
});

test("Tạo hồ sơ qua API thật luôn gửi role CUSTOMER dù dữ liệu form có role", async () => {
  let sent;
  const api = createLiveApi(async (path, options) => {
    sent = { path, ...options };
    return { userId: 9, fullName: "An", email: "an@example.com", role: "CUSTOMER" };
  });
  const fields = { ...normalizeCustomerFields({ fullName: "An", email: "an@example.com" }), role: "ADMIN" };
  const profile = toCustomerProfile(await api.createCustomer(fields));
  assert.equal(sent.body.role, "CUSTOMER");
  assert.equal(profile.userId, 9);
});

test("Demo: email trùng bị từ chối, mã lạ báo không tìm thấy, mã đúng trả hồ sơ", async () => {
  const store = new Map();
  const api = createDemoApi({ getItem: (k) => store.get(k), setItem: (k, v) => store.set(k, v) });
  const fields = normalizeCustomerFields({ fullName: "An", email: "An@Example.com" });
  const user = toCustomerProfile(await api.createCustomer(fields));
  await assert.rejects(
    api.createCustomer({ ...fields, email: "an@example.com" }),
    (e) => /đã có hồ sơ/.test(customerErrorMessage(e, "create")),
  );
  await assert.rejects(api.user(999), (e) => /Không tìm thấy/.test(customerErrorMessage(e, "lookup")));
  assert.equal(toCustomerProfile(await api.user(user.userId), user.userId).email, "An@Example.com");
});
