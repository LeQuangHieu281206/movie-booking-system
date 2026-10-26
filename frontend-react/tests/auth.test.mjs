import test from "node:test";
import assert from "node:assert/strict";
import { createAuthService } from "../src/services/auth.js";

// Storage giả lập thay cho localStorage trong Node.
function fakeStorage() {
  const map = new Map();
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
  };
}

function withStorage(run) {
  const storage = fakeStorage();
  const original = globalThis.localStorage;
  globalThis.localStorage = storage;
  return Promise.resolve(run()).finally(() => {
    if (original === undefined) delete globalThis.localStorage;
    else globalThis.localStorage = original;
  });
}

test("login thành công trả session có user profile dạng userId", () =>
  withStorage(async () => {
    const calls = [];
    const auth = createAuthService(async (path, options) => {
      calls.push({ path, options });
      return {
        accessToken: "at",
        refreshToken: "rt",
        tokenType: "Bearer",
        expiresIn: 7200,
        user: {
          id: 7,
          email: "a@b.c",
          fullName: "Nguyễn Văn A",
          role: "CUSTOMER",
        },
      };
    });
    const session = await auth.login("a@b.c", "secret123");
    assert.equal(calls[0].path, "/auth/login");
    assert.deepEqual(calls[0].options.body, {
      email: "a@b.c",
      password: "secret123",
    });
    assert.deepEqual(session.user, {
      userId: 7,
      email: "a@b.c",
      fullName: "Nguyễn Văn A",
      role: "CUSTOMER",
    });
    assert.equal(session.accessToken, "at");
    assert.equal(auth.restore().accessToken, "at");
  }));

test("register gửi đúng payload rồi tự đăng nhập", () =>
  withStorage(async () => {
    const calls = [];
    const auth = createAuthService(async (path, options) => {
      calls.push({ path, body: options.body });
      if (path === "/auth/register")
        return { id: 9, email: "x@y.z", fullName: "B", role: "CUSTOMER" };
      return {
        accessToken: "at2",
        refreshToken: "rt2",
        user: { id: 9, email: "x@y.z", fullName: "B", role: "CUSTOMER" },
      };
    });
    const session = await auth.register({
      fullName: "  B  ",
      email: "  x@y.z ",
      password: "longenough",
      phoneNumber: "090",
    });
    assert.deepEqual(calls[0], {
      path: "/auth/register",
      body: {
        fullName: "B",
        email: "x@y.z",
        password: "longenough",
        phoneNumber: "090",
      },
    });
    assert.equal(calls[1].path, "/auth/login");
    assert.equal(session.user.userId, 9);
  }));

test("refresh đổi token và ghi đè session đã lưu", () =>
  withStorage(async () => {
    const auth = createAuthService(async () => ({
      accessToken: "at3",
      refreshToken: "rt3",
      user: { id: 3, email: "r@r.r", fullName: "R", role: "CUSTOMER" },
    }));
    await auth.refresh("old-rt");
    const restored = auth.restore();
    assert.equal(restored.accessToken, "at3");
    assert.equal(restored.user.userId, 3);
  }));

test("restore trả null khi chưa có session và logout xóa session", () =>
  withStorage(async () => {
    const auth = createAuthService(async () => ({
      accessToken: "at",
      refreshToken: "rt",
      user: { id: 5, email: "a@b.c", fullName: "A", role: "CUSTOMER" },
    }));
    assert.equal(auth.restore(), null);
    await auth.login("a@b.c", "secret123");
    assert.ok(auth.restore());
    auth.logout();
    assert.equal(auth.restore(), null);
  }));

test("lỗi từ backend được chuyển tiếp kèm message tiếng Việt", () =>
  withStorage(async () => {
    const auth = createAuthService(async () => {
      throw new Error("Email hoặc mật khẩu không đúng");
    });
    await assert.rejects(
      () => auth.login("a@b.c", "wrong"),
      /Email hoặc mật khẩu không đúng/,
    );
  }));
