import {
  readSession,
  sessionFromAuthResponse,
  toProfile,
  writeSession,
} from "./session.js";

export function createAuthService(request) {
  return {
    restore: () => {
      const session = readSession();
      if (!session) return null;
      return { ...session, user: toProfile(session.user) };
    },
    logout: () => writeSession(null),
    async login(email, password) {
      const session = sessionFromAuthResponse(
        await request("/auth/login", {
          method: "POST",
          body: { email: email.trim(), password },
        }),
      );
      writeSession(session);
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
      const session = sessionFromAuthResponse(
        await request("/auth/refresh", {
          method: "POST",
          body: { refreshToken },
        }),
      );
      writeSession(session);
      return session;
    },
  };
}
