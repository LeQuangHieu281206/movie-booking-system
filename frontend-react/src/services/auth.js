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
