import React, { useEffect, useRef, useState } from "react";
import { GoogleLogin } from "@react-oauth/google";
import { Button, ErrorBox, Icon } from "../components/UI.jsx";
import { labelOf } from "../lib/domain.js";

const PHONE_PATTERN = "[0-9]{8,11}|[+]84[0-9]{9}";
const PHONE_TITLE = "Số điện thoại gồm 8-11 chữ số hoặc +84 theo sau 9 chữ số";
const GOOGLE_CLIENT_ID =
  import.meta.env?.VITE_GOOGLE_CLIENT_ID ||
  "your-google-client-id.apps.googleusercontent.com";

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

function encodeBase64Url(obj) {
  const json = JSON.stringify(obj);
  const bytes = new TextEncoder().encode(json);
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function createDevGoogleIdToken(emailInput = "", nameInput = "") {
  const email =
    emailInput && emailInput.includes("@")
      ? emailInput.trim().toLowerCase()
      : "cineroom.google@gmail.com";
  const defaultName = email
    .split("@")[0]
    .replace(/[._-]+/g, " ")
    .replace(/\b\w/g, (ch) => ch.toUpperCase());
  const name = nameInput?.trim() || defaultName || "Google User";
  const nowSec = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: "https://accounts.google.com",
    sub: `google-${email}`,
    email,
    email_verified: true,
    name,
    iat: nowSec,
    exp: nowSec + 3600,
  };
  return `${encodeBase64Url(header)}.${encodeBase64Url(payload)}.dev-local-signature`;
}

function GoogleMark() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
    >
      <path
        fill="#4285F4"
        d="M23.49 12.27c0-.79-.07-1.54-.19-2.27H12v4.51h6.47c-.29 1.48-1.14 2.73-2.4 3.58v2.98h3.86c2.26-2.09 3.56-5.17 3.56-8.8Z"
      />
      <path
        fill="#34A853"
        d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.86-2.98c-1.08.72-2.45 1.16-4.07 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.09C3.26 21.3 7.31 24 12 24Z"
      />
      <path
        fill="#FBBC05"
        d="M5.28 14.31c-.24-.72-.38-1.49-.38-2.31s.14-1.59.38-2.31V6.6H1.29C.47 8.23 0 10.06 0 12s.47 3.77 1.29 5.4l3.99-3.09Z"
      />
      <path
        fill="#EA4335"
        d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.6l3.99 3.09c.95-2.85 3.6-4.94 6.72-4.94Z"
      />
    </svg>
  );
}

function roleLabel(role) {
  return labelOf(role);
}

function initials(name = "") {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(-2)
      .map((part) => part[0] || "")
      .join("")
      .toUpperCase() || "CR"
  );
}

function ProfilePanel({ auth, api, user, onUser }) {
  const [fullName, setFullName] = useState(user.fullName || "");
  const [phoneNumber, setPhoneNumber] = useState(user.phoneNumber || "");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setFullName(user.fullName || "");
    setPhoneNumber(user.phoneNumber || "");
  }, [user.fullName, user.phoneNumber]);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");
    const name = fullName.trim();
    if (name.length < 2) {
      setError("Họ tên phải có ít nhất 2 ký tự.");
      return;
    }
    setBusy(true);
    try {
      const updated = await api.updateProfile({
        fullName: name,
        phoneNumber: phoneNumber.trim() || null,
      });
      onUser(updated);
      auth.updateUser(updated);
      setSuccess("Đã cập nhật thông tin cá nhân.");
    } catch (cause) {
      setError(cause.message || "Không cập nhật được thông tin. Thử lại sau.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container page-section" id="auth-page">
      <div className="profile-card">
        <div className="profile-head">
          <div className="profile-avatar" aria-hidden="true">
            {initials(user.fullName)}
          </div>
          <div className="profile-identity">
            <span className="eyebrow">TÀI KHOẢN CINEROOM</span>
            <h1>{user.fullName}</h1>
            <p>{user.email}</p>
            <span className="profile-role">{roleLabel(user.role)}</span>
          </div>
        </div>
        <div className="profile-divider" />
        <form className="profile-form" onSubmit={submit}>
          <div className="profile-section-title">
            <h2>Thông tin cá nhân</h2>
            <p>
              Cập nhật tên hiển thị và số điện thoại. Email dùng để đăng nhập
              nên không thể thay đổi tại đây.
            </p>
          </div>
          {error && <ErrorBox message={error} />}
          {success && (
            <div className="success-note" role="status">
              <Icon name="check" size={18} />
              {success}
            </div>
          )}
          <div className="form-grid">
            <label>
              Họ và tên
              <input
                name="fullName"
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
                required
                minLength={2}
                maxLength={100}
                autoComplete="name"
              />
            </label>
            <label>
              Số điện thoại
              <input
                name="phoneNumber"
                value={phoneNumber}
                onChange={(event) => setPhoneNumber(event.target.value)}
                maxLength={12}
                pattern={PHONE_PATTERN}
                title={PHONE_TITLE}
                inputMode="tel"
                autoComplete="tel"
                placeholder="0912345678"
              />
            </label>
          </div>
          <label>
            Email
            <input value={user.email} disabled readOnly />
          </label>
          <div className="profile-meta-row">
            <span>Vai trò</span>
            <b>{roleLabel(user.role)}</b>
          </div>
          <div className="profile-actions">
            <Button primary type="submit" disabled={busy}>
              {busy ? "Đang lưu…" : "Lưu thay đổi"}
              <Icon name="check" size={16} />
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function Auth({ auth, api, user, onUser, onDone }) {
  const [tab, setTab] = useState("login");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const formRef = useRef(null);
  const hasRealClientId = !isPlaceholderClientId(GOOGLE_CLIENT_ID);

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

  function onGoogleButtonClick() {
    if (busy) return;
    if (hasRealClientId && globalThis.google?.accounts?.id) {
      globalThis.google.accounts.id.prompt();
      return;
    }
    const formData = formRef.current
      ? Object.fromEntries(new FormData(formRef.current))
      : {};
    const devToken = createDevGoogleIdToken(formData.email, formData.fullName);
    handleGoogleCredential(devToken);
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
            <span className="eyebrow">CINEROOM / TÀI KHOẢN</span>
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
          <div className="auth-divider" role="separator" aria-label="hoặc">
            <span>hoặc</span>
          </div>
          <div className="google-auth-section">
            <button
              type="button"
              className="button full google-auth-button"
              disabled={busy}
              onClick={onGoogleButtonClick}
            >
              <GoogleMark />
              <span>Đăng nhập bằng Google</span>
            </button>
            {hasRealClientId && (
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
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
