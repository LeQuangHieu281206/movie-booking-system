import React, { useState } from "react";
import { Button, ErrorBox, Icon } from "./UI.jsx";

export default function CustomerForm({ api, user, onUser, disabled = false }) {
  const [tab, setTab] = useState("new"),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const fields = Object.fromEntries(new FormData(event.currentTarget));
    try {
      onUser(
        tab === "new"
          ? await api.createCustomer(fields)
          : await api.user(Number(fields.userId)),
      );
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(false);
    }
  }
  if (user)
    return (
      <div className="customer-card">
        <span className="round-icon">
          <Icon name="user" />
        </span>
        <div>
          <strong>{user.fullName}</strong>
          <p>{user.email}</p>
          <small>
            Mã khách hàng: <b>#{user.userId}</b> · Hãy lưu mã để xem lại vé
          </small>
        </div>
        <Button
          disabled={disabled}
          className="small"
          onClick={() => onUser(null)}
        >
          Đổi hồ sơ
        </Button>
      </div>
    );
  return (
    <section className="customer-form">
      <div className="segmented">
        <button
          type="button"
          className={tab === "new" ? "active" : ""}
          onClick={() => {
            setTab("new");
            setError("");
          }}
        >
          Khách hàng mới
        </button>
        <button
          type="button"
          className={tab === "existing" ? "active" : ""}
          onClick={() => {
            setTab("existing");
            setError("");
          }}
        >
          Đã có mã khách hàng
        </button>
      </div>
      <form onSubmit={submit}>
        <fieldset disabled={busy || disabled}>
          {tab === "new" ? (
            <>
              <label>
                Họ và tên
                <input
                  required
                  name="fullName"
                  autoComplete="name"
                  maxLength={100}
                  placeholder="Nhập họ và tên"
                />
              </label>
              <div className="form-grid">
                <label>
                  Email
                  <input
                    required
                    type="email"
                    name="email"
                    autoComplete="email"
                    maxLength={150}
                    placeholder="ban@example.com"
                  />
                </label>
                <label>
                  Số điện thoại
                  <input
                    name="phoneNumber"
                    type="tel"
                    autoComplete="tel"
                    maxLength={20}
                    placeholder="Số điện thoại liên hệ"
                  />
                </label>
              </div>
              <p className="hint">
                Tạo hồ sơ để gắn thông tin khách hàng với vé đặt.
              </p>
            </>
          ) : (
            <>
              <label>
                Mã khách hàng
                <input
                  required
                  name="userId"
                  type="number"
                  min="1"
                  step="1"
                  placeholder="Ví dụ: 12"
                />
              </label>
              <p className="hint">
                Dùng mã đã được cấp khi tạo hồ sơ. Mã dữ liệu mẫu và mã backend
                được lưu riêng.
              </p>
            </>
          )}
          {error && <ErrorBox message={error} />}
          <Button primary type="submit" className="full">
            {busy
              ? "Đang xử lý…"
              : tab === "new"
                ? "Tạo hồ sơ khách hàng"
                : "Lấy thông tin khách hàng"}
            <Icon name="arrow" size={16} />
          </Button>
        </fieldset>
      </form>
    </section>
  );
}
