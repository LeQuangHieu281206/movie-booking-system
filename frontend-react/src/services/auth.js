import { randomId } from "../lib/randomId.js";

const STORAGE_KEY = "cineroom-auth";

// Chuyển UserSummary của backend ({id, email, fullName, role}) sang shape
// {userId, fullName, email} mà App/Tickets/Booking đang dùng.
function toProfile(summary) {
  return {
    userId: summary.id,
    fullName: summary.fullName,
    email: summary.email,
    role: summary.role,
  };
}

function read() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : null;
    return parsed && parsed.user ? parsed : null;
  } catch {
    return null;
  }
}

function write(session) {
  try {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    /* phiên vẫn dùng được trong tab hiện tại. */
  }
}

export function createAuthService(request) {
  return {
    restore: () => {
      const session = read();
      if (!session) return null;
      // Session lưu profile đã map ({userId}); tránh map lặp nếu vẫn còn shape gốc ({id}).
      const user = session.user?.id !== undefined
        ? toProfile(session.user)
        : session.user;
      return { ...session, user };
    },
    persist: (session) => write(session),
    logout: () => write(null),
    async login(email, password) {
      const data = await request("/auth/login", {
        method: "POST",
        body: { email: email.trim(), password },
      });
      const session = {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: toProfile(data.user),
      };
      write(session);
      return session;
    },
    async register(fields) {
      await request("/auth/register", {
        method: "POST",
        body: {
          fullName: fields.fullName.trim(),
          email: fields.email.trim(),
          password: fields.password,
          phoneNumber: fields.phoneNumber?.trim() || null,
        },
      });
      // Đăng ký xong đăng nhập luôn để có token.
      return this.login(fields.email, fields.password);
    },
    async refresh(refreshToken) {
      const data = await request("/auth/refresh", {
        method: "POST",
        body: { refreshToken },
      });
      const session = {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: toProfile(data.user),
      };
      write(session);
      return session;
    },
  };
}

// Demo mode: không gọi backend — đăng nhập/đăng ký ghi user vào cùng kho dữ
// liệu mẫu với đặt vé (qua ensureDemoUser) để booking tìm thấy khách hàng.
// Mật khẩu lưu riêng theo email; tài khoản mẫu: demo@cineroom / demopass.
import { ensureDemoUser } from "./demo.js";

export function createDemoAuthService() {
  const USERS_KEY = "cineroom-demo-users";
  const readUsers = () => {
    try {
      return JSON.parse(localStorage.getItem(USERS_KEY)) || [
        { email: "demo@cineroom", password: "demopass" },
      ];
    } catch {
      return [{ email: "demo@cineroom", password: "demopass" }];
    }
  };
  const saveUsers = (users) => {
    try {
      localStorage.setItem(USERS_KEY, JSON.stringify(users));
    } catch {
      /* bỏ qua nếu trình duyệt chặn localStorage. */
    }
  };
  const checkPassword = (email, password) => {
    const users = readUsers();
    if (users.some((item) => item.email === email.trim() && item.password === password))
      return users;
    throw Object.assign(
      new Error("Email hoặc mật khẩu không đúng (chế độ dữ liệu mẫu)."),
      { status: 401 },
    );
  };
  return {
    restore: () => read(),
    persist: (session) => write(session),
    logout: () => write(null),
    async login(email, password) {
      checkPassword(email, password);
      // Đồng bộ user vào kho đặt vé và dùng userId của kho đó.
      const user = await ensureDemoUser({
        email,
        fullName: email.split("@")[0],
      });
      const session = { accessToken: null, refreshToken: null, user };
      write(session);
      return session;
    },
    async register(fields) {
      const users = readUsers();
      if (users.some((item) => item.email === fields.email.trim()))
        throw Object.assign(new Error("Email này đã được đăng ký."), {
          status: 409,
        });
      const user = await ensureDemoUser(fields);
      users.push({ email: user.email, password: fields.password });
      saveUsers(users);
      const session = { accessToken: null, refreshToken: null, user };
      write(session);
      return session;
    },
    async refresh() {
      return this.restore();
    },
  };
}

export { randomId };
