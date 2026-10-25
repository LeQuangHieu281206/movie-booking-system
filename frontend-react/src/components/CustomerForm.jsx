import React, { useState } from "react";
import { Button, ErrorBox, Icon } from "./UI.jsx";
import {
  customerErrorMessage,
  normalizeCustomerFields,
  parseCustomerId,
  toCustomerProfile,
} from "../lib/customer.js";

export default function CustomerForm({ api, user, onUser, disabled = false }) {
  const [tab, setTab] = useState("new"),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const fields = Object.fromEntries(new FormData(event.currentTarget));
    setError("");
    setBusy(true);
    const action = tab === "new" ? "create" : "lookup";
    try {
      if (action === "create") {
        // Role luôn do api.createCustomer đặt là CUSTOMER; form không cho nhập role.
        const created = await api.createCustomer(
          normalizeCustomerFields(fields),
        );
        onUser(toCustomerProfile(created));
      } else {
        const id = parseCustomerId(fields.userId);
        onUser(toCustomerProfile(await api.user(id), id));
      }
    } catch (error) {
      setError(customerErrorMessage(error, action));
    } finally {
      setBusy(false);
    }
  }
  function switchTab(next) {
    setTab(next);
    setError("");
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
      <div className="segmented" role="group" aria-label="Loại hồ sơ">
        <button
          type="button"
          className={tab === "new" ? "active" : ""}
          aria-pressed={tab === "new"}
          onClick={() => switchTab("new")}
        >
          Khách hàng mới
        </button>
        <button
          type="button"
          className={tab === "existing" ? "active" : ""}
          aria-pressed={tab === "existing"}
          onClick={() => switchTab("existing")}
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
                  minLength={2}
                  maxLength={100}
                  pattern=".*\S.*"
                  title="Vui lòng nhập họ và tên"
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
                  Số điện thoại (không bắt buộc)
                  <input
                    name="phoneNumber"
                    type="tel"
                    autoComplete="tel"
                    inputMode="tel"
                    maxLength={20}
                    placeholder="Số điện thoại liên hệ"
                  />
                </label>
              </div>
              <p className="hint">
                Tạo hồ sơ để gắn thông tin khách hàng với vé đặt. Hồ sơ này
                không có mật khẩu: hãy ghi nhớ mã khách hàng được cấp.
              </p>
            </>
          ) : (
            <>
              <label>
                Mã khách hàng
                <input
                  required
                  name="userId"
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={16}
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
