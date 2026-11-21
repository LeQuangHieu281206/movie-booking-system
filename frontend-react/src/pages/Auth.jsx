import React, { useEffect, useRef, useState } from "react";
import { GoogleLogin } from "@react-oauth/google";
import { Button, ErrorBox, Icon } from "../components/UI.jsx";
import { labelOf } from "../lib/domain.js";

const PHONE_PATTERN = "[0-9]{8,11}|[+]84[0-9]{9}";
const PHONE_TITLE = "Số điện thoại gồm 8-11 chữ số hoặc +84 theo sau 9 chữ số";
const GOOGLE_CLIENT_ID =
  import.meta.env?.VITE_GOOGLE_CLIENT_ID ||
  "your-google-client-id.apps.googleusercontent.com";

// Không có Client ID thật thì ẩn hẳn nút Google: trước đây nút này tự tạo
// token giả và đăng nhập luôn, không hề mở bảng chọn tài khoản Google.

function isPlaceholderClientId(clientId) {
  if (!clientId || typeof clientId !== "string") return true;
  const normalized = clientId.trim().toLowerCase();
  return (
    !normalized ||
    normalized.startsWith("your-") ||
    normalized.includes("placeholder") ||
    normalized === "your-google-client-id.apps.googleusercontent.com"
  );
}

export default function Auth({ auth, api, user, onUser, onDone }) {
  const [tab, setTab] = useState("login");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const formRef = useRef(null);
  const hasRealClientId = !isPlaceholderClientId(GOOGLE_CLIENT_ID);
  const [googleFailed, setGoogleFailed] = useState(false);

  // Google chỉ vẽ nút khi origin đã được khai báo; nếu iframe co về 0px thì
  // báo lỗi cấu hình thay vì để một khoảng trống im lặng.
  useEffect(() => {
    if (!hasRealClientId) return undefined;
    const frameSize = () => {
      const frame = document.querySelector(".google-oauth-widget iframe");
      return frame ? frame.getBoundingClientRect().width : 0;
    };
    const timer = window.setTimeout(() => setGoogleFailed(frameSize() < 10), 4000);
    const poll = window.setInterval(() => {
      if (frameSize() >= 10) {
        setGoogleFailed(false);
        window.clearInterval(poll);
      }
    }, 1000);
    return () => {
      window.clearTimeout(timer);
      window.clearInterval(poll);
    };
  }, [hasRealClientId]);

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

  async function handleGoogleCredential(idToken) {
    if (!idToken) {
      setError("Không nhận được Google ID Token. Vui lòng thử lại.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      const session = await auth.googleLogin(idToken);
      onUser(session.user);
      onDone();
    } catch (cause) {
      setError(
        cause.message || "Không đăng nhập được bằng Google. Thử lại sau.",
      );
    } finally {
      setBusy(false);
    }
  }

  if (user) {
    return <ProfilePanel auth={auth} api={api} user={user} onUser={onUser} />;
  }

  return (
    <div className="container page-section" id="auth-page">
      <div className="auth-shell">
        <div className="auth-tabs" role="group" aria-label="Đăng nhập hoặc đăng ký">
          <button
            type="button"
            className={tab === "login" ? "active" : ""}
            aria-pressed={tab === "login"}
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
            aria-pressed={tab === "register"}
            onClick={() => {
              setTab("register");
              setError("");
            }}
          >
            Đăng ký
          </button>
        </div>
        <div className="auth-card">
          <div className="auth-intro">
            <h1>{tab === "login" ? "Chào mừng trở lại" : "Tạo tài khoản mới"}</h1>
            <p>
              {tab === "login"
                ? "Đăng nhập để đặt vé và xem vé của bạn."
                : "Chỉ mất một phút để bắt đầu đặt vé."}
            </p>
          </div>
          {error && <ErrorBox message={error} />}
          <form ref={formRef} onSubmit={submit} key={tab}>
            <fieldset disabled={busy}>
              {tab === "register" && (
                <label>
                  Họ và tên
                  <input
                    name="fullName"
                    required
                    minLength={2}
                    maxLength={100}
                    autoComplete="name"
                    placeholder="Nguyễn Văn A"
                  />
                </label>
              )}
              <label>
                Email
                <input
                  name="email"
                  type="email"
                  required
                  maxLength={150}
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
                    <input
                      name="phoneNumber"
                      inputMode="tel"
                      maxLength={12}
                      pattern={PHONE_PATTERN}
                      title={PHONE_TITLE}
                      placeholder="0912345678"
                    />
                  </label>
                </>
              )}
              <Button primary className="full" type="submit">
                {busy
                  ? "Đang xử lý…"
                  : tab === "login"
                    ? "Đăng nhập"
                    : "Tạo tài khoản"}
                <Icon name="arrow" size={16} />
              </Button>
            </fieldset>
          </form>
          {hasRealClientId && (
            <>
              <div className="auth-divider" role="separator" aria-label="hoặc">
                <span>hoặc</span>
              </div>
              <div className="google-auth-section">
                <div className="google-oauth-widget">
                  <GoogleLogin
                    onSuccess={(response) =>
                      handleGoogleCredential(response?.credential)
                    }
                    onError={() =>
                      setError(
                        "Đăng nhập Google không thành công. Vui lòng thử lại.",
                      )
                    }
                    useOneTap={false}
                    shape="pill"
                    size="large"
                    text="signin_with"
                    locale="vi"
                  />
                </div>
                {googleFailed && (
                  <p className="hint google-auth-hint">
                    Không tải được nút Google. Kiểm tra mục Authorized
                    JavaScript origins của OAuth Client ID đã có
                    http://localhost:5173 chưa, rồi tải lại trang.
                  </p>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
