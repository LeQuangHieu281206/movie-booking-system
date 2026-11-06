import React, { useEffect, useState } from "react";
import { Button, ErrorBox, Icon } from "../components/UI.jsx";
import { labelOf } from "../lib/domain.js";

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
                maxLength={20}
                inputMode="tel"
                autoComplete="tel"
                placeholder="09xx xxx xxx"
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
          <form onSubmit={submit} key={tab}>
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
                      maxLength={20}
                      placeholder="09xx xxx xxx"
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
        </div>
      </div>
    </div>
  );
}
