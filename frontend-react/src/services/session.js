// Phiên đăng nhập JWT lưu trong localStorage. Không chứa mật khẩu.
const KEY = "cineroom-auth";

// UserSummary của /auth/* dùng `id`, UserResponse của /users/me dùng `userId`.
export function toProfile(user) {
  return {
    userId: user.userId ?? user.id,
    fullName: user.fullName,
    email: user.email,
    role: user.role,
  };
}

export function readSession() {
  try {
    const raw = globalThis.localStorage?.getItem(KEY);
    const parsed = raw ? JSON.parse(raw) : null;
    return parsed?.user && parsed?.accessToken ? parsed : null;
  } catch {
    return null;
  }
}

export function writeSession(session) {
  try {
    if (session) globalThis.localStorage?.setItem(KEY, JSON.stringify(session));
    else globalThis.localStorage?.removeItem(KEY);
  } catch {
    /* Tab hiện tại vẫn dùng được phiên trong bộ nhớ. */
  }
}

export function sessionFromAuthResponse(data) {
  return {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    user: toProfile(data.user),
  };
}

let refreshing = null;

// Đổi refresh token (7 ngày) lấy access token mới (2 giờ). Gọi tuần tự, không song song.
export function refreshSession(fetchImpl = globalThis.fetch) {
  const session = readSession();
  if (!session?.refreshToken || !fetchImpl) return Promise.resolve(null);
  refreshing ??= (async () => {
    try {
      const response = await fetchImpl("/api/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      });
      if (!response.ok) {
        writeSession(null);
        return null;
      }
      const next = sessionFromAuthResponse(await response.json());
      writeSession(next);
      return next;
    } catch {
      return null;
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}
