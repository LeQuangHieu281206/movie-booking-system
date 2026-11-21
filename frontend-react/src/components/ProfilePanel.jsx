import React, { useEffect, useState } from "react";
import { Button, ErrorBox, Icon } from "./UI.jsx";

function roleLabel(role) {
  return role === "ADMIN" ? "Quản trị viên" : "Khách hàng";
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

export default function ProfilePanel({ auth, api, user, onUser }) {
  const [fullName, setFullName] = useState(user.fullName || "");
  const [phoneNumber, setPhoneNumber] = useState(user.phoneNumber || "");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState("");
  const [passwordBusy, setPasswordBusy] = useState(false);

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

  async function changePassword(event) {
    event.preventDefault();
    setPasswordError("");
    setPasswordSuccess("");
    if (newPassword.length < 8) {
      setPasswordError("Mật khẩu mới phải có ít nhất 8 ký tự.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError("Mật khẩu mới và xác nhận không khớp.");
      return;
    }
    setPasswordBusy(true);
    try {
      await api.changePassword({
        currentPassword: currentPassword || null,
        newPassword,
      });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setPasswordSuccess("Đã đổi mật khẩu.");
    } catch (cause) {
      setPasswordError(cause.message || "Không đổi được mật khẩu. Thử lại sau.");
    } finally {
      setPasswordBusy(false);
    }
  }

  return (
    <div className="container page-section" id="profile-page">
      <div className="profile-card">
        <div className="profile-head">
          <div className="profile-avatar" aria-hidden="true">
            {initials(user.fullName)}
          </div>
          <div className="profile-identity">
            <h1>{user.fullName}</h1>
            <p>{user.email}</p>
            <span className="profile-role">{roleLabel(user.role)}</span>
          </div>
        </div>
        <div className="profile-divider" />
        <form className="profile-form" onSubmit={submit}>
          <div className="profile-section-title">
            <h2>Thông tin cá nhân</h2>
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
        <div className="profile-divider" />
        <form className="profile-form" onSubmit={changePassword}>
          <div className="profile-section-title">
            <h2>Đổi mật khẩu</h2>
          </div>
          {passwordError && <ErrorBox message={passwordError} />}
          {passwordSuccess && (
            <div className="success-note" role="status">
              <Icon name="check" size={18} />
              {passwordSuccess}
            </div>
          )}
          <div className="form-grid">
            <label>
              Mật khẩu hiện tại
              <input
                name="currentPassword"
                type="password"
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                autoComplete="current-password"
                placeholder="Bỏ trống nếu chỉ đăng nhập bằng Google"
              />
            </label>
            <label>
              Mật khẩu mới
              <input
                name="newPassword"
                type="password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                required
                minLength={8}
                maxLength={100}
                autoComplete="new-password"
              />
            </label>
          </div>
          <label>
            Xác nhận mật khẩu mới
            <input
              name="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
              minLength={8}
              maxLength={100}
              autoComplete="new-password"
            />
          </label>
          <div className="profile-actions">
            <Button primary type="submit" disabled={passwordBusy}>
              {passwordBusy ? "Đang đổi…" : "Đổi mật khẩu"}
              <Icon name="check" size={16} />
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
