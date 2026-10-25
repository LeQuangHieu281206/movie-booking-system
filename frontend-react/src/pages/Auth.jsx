import React, { useState } from "react";
import { Button, ErrorBox, Icon } from "../components/UI.jsx";

export default function Auth({ auth, mode, user, onUser, onDone }) {
  const [tab, setTab] = useState("login");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError("");
    const fields = Object.fromEntries(new FormData(event.currentTarget));
    if (tab === "register" && fields.password !== fields.confirm)
      return setError("Mật khẩu và xác nhận mật khẩu không khớp.");
    if (tab === "register" && fields.password.length < 8)
      return setError("Mật khẩu tối thiểu 8 ký tự.");
    setBusy(true);
    try {
      const session =
        tab === "login"
          ? await auth.login(fields.email, fields.password)
          : await auth.register(fields);
      onUser(session.user);
      onDone();
    } catch (cause) {
      setError(cause.message || "Không xác thực được. Thử lại sau.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container page-section" id="auth-page">
      <div className="auth-card">
        <div className="segmented" role="group" aria-label="Đăng nhập hoặc đăng ký">
          <button
            type="button"
            className={tab === "login" ? "active" : ""}
            onClick={() => {
              setTab("login");
              setError("");
            }}
          >
            Đăng nhập
          </button>
          <button
            type="button"
            className={tab === "register" ? "active" : ""}
            onClick={() => {
              setTab("register");
              setError("");
            }}
          >
            Đăng ký
          </button>
        </div>
        {mode === "demo" && (
          <p className="auth-hint">
            Chế độ dữ liệu mẫu: dùng tài khoản <b>demo@cineroom / demopass</b>{" "}
            hoặc đăng ký tài khoản mới (chỉ lưu trong trình duyệt).
          </p>
        )}
        {error && <ErrorBox message={error} />}
        <form onSubmit={submit} key={tab}>
          <fieldset disabled={busy}>
            {tab === "register" && (
              <label>
                Họ và tên
                <input name="fullName" required placeholder="Nguyễn Văn A" />
              </label>
            )}
            <label>
              Email
              <input
                name="email"
                type="email"
                required
                autoComplete="email"
                placeholder="ban@example.com"
              />
            </label>
            <label>
              Mật khẩu
              <input
                name="password"
                type="password"
                required
                minLength={tab === "register" ? 8 : undefined}
                autoComplete={
                  tab === "login" ? "current-password" : "new-password"
                }
                placeholder="••••••••"
              />
            </label>
            {tab === "register" && (
              <>
                <label>
                  Xác nhận mật khẩu
                  <input
                    name="confirm"
                    type="password"
                    required
                    autoComplete="new-password"
                    placeholder="••••••••"
                  />
                </label>
                <label>
                  Số điện thoại (không bắt buộc)
                  <input name="phoneNumber" inputMode="tel" placeholder="09xx xxx xxx" />
                </label>
              </>
            )}
            <Button primary className="full" type="submit">
              {busy
                ? "Đang xử lý..."
                : tab === "login"
                  ? "Đăng nhập"
                  : "Tạo tài khoản"}
              <Icon name="arrow" size={16} />
            </Button>
          </fieldset>
        </form>
        {user && tab === "login" && (
          <p className="auth-hint">
            Bạn đang đăng nhập với <b>{user.email}</b>. Đăng nhập lại để đổi
            tài khoản.
          </p>
        )}
      </div>
    </div>
  );
}
