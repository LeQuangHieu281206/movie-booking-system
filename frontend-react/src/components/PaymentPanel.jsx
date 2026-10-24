import React, { useRef, useState } from "react";
import { Button, ErrorBox, Icon } from "./UI.jsx";
import { statusText, money } from "../lib/domain.js";

// Payment is a simulator in the supplied backend, not a bank integration.
export default function PaymentPanel({
  api,
  mode,
  booking,
  onUpdate,
  onBusy = () => {},
}) {
  const key = `cineroom-payment-attempt:${mode}:${booking.bookingId}`;
  const [sent, setSent] = useState(() => sessionStorage.getItem(key) === "1");
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [confirmCancel, setConfirmCancel] = useState(false);
  const lock = useRef(false);
  async function run(action) {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    onBusy(true);
    setError("");
    setNotice("");
    try {
      await action();
    } catch (error) {
      setError(error.message);
    } finally {
      lock.current = false;
      setBusy(false);
      onBusy(false);
    }
  }
  function refresh() {
    return run(async () => {
      const latest = await api.booking(booking.bookingId);
      onUpdate(latest);
      if (latest.status === "PENDING")
        setNotice(
          "Booking vẫn đang chờ. Nếu vừa thanh toán, hãy kiểm tra RabbitMQ và dịch vụ booking rồi cập nhật lại.",
        );
    });
  }
  function pay(simulateFailure) {
    return run(async () => {
      const latest = await api.booking(booking.bookingId);
      onUpdate(latest);
      if (latest.status !== "PENDING") return;
      sessionStorage.setItem(key, "1");
      setSent(true);
      await api.pay(latest, simulateFailure);
      // Receiving a payment event is not proof the booking listener committed it.
      for (let attempt = 0; attempt < 8; attempt++) {
        const current = await api.booking(booking.bookingId);
        onUpdate(current);
        if (current.status !== "PENDING") return;
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
      setNotice(
        "Đã gửi yêu cầu thanh toán nhưng booking chưa xác nhận. Hãy cập nhật trạng thái; không gửi thanh toán lặp lại.",
      );
    });
  }
  function cancel() {
    return run(async () => {
      const latest = await api.booking(booking.bookingId);
      if (latest.status !== "PENDING") {
        onUpdate(latest);
        return;
      }
      onUpdate(await api.cancel(booking.bookingId));
      setConfirmCancel(false);
    });
  }
  return (
    <section className="payment-panel">
      <div className="subheading">
        <h3>Trạng thái vé</h3>
        <span className={`badge ${booking.status.toLowerCase()}`}>
          {statusText(booking.status)}
        </span>
      </div>
      {booking.status === "PAID" ? (
        <div className="success-note">
          <Icon name="check" />
          Booking đã xác nhận thanh toán. Hẹn bạn tại rạp!
        </div>
      ) : booking.status === "PENDING" ? (
        <>
          <p>
            Thanh toán mô phỏng · {money(booking.totalAmount)}. Không thu tiền
            hoặc yêu cầu thông tin thẻ ngân hàng.
          </p>
          <div className="button-row">
            <Button primary disabled={busy || sent} onClick={() => pay(false)}>
              {busy ? "Đang xử lý…" : "Mô phỏng thanh toán thành công"}
            </Button>
            <Button disabled={busy || sent} onClick={() => pay(true)}>
              Thử thanh toán thất bại
            </Button>
          </div>
          {sent && (
            <p className="hint">
              Đã thử gửi thanh toán cho vé này trong phiên hiện tại. Hãy cập
              nhật trạng thái để tránh gửi trùng.
            </p>
          )}
        </>
      ) : (
        <p>
          Vé {booking.status === "CANCELLED" ? "đã hủy" : "đã hết hạn"}. Bạn có
          thể chọn suất chiếu và đặt vé mới.
        </p>
      )}
      {notice && (
        <div className="notice" role="status">
          {notice}
        </div>
      )}
      {error && <ErrorBox message={error} />}
      <div className="button-row payment-secondary">
        <Button className="small" disabled={busy} onClick={refresh}>
          <Icon name="refresh" size={15} />
          Cập nhật trạng thái
        </Button>
        {booking.status === "PENDING" && !sent && !confirmCancel && (
          <Button
            disabled={busy}
            className="small danger"
            onClick={() => setConfirmCancel(true)}
          >
            Hủy vé đang chờ
          </Button>
        )}
      </div>
      {confirmCancel && (
        <div className="notice">
          <p>Hủy booking này và yêu cầu backend trả ghế?</p>
          <div className="button-row">
            <Button disabled={busy} className="small danger" onClick={cancel}>
              Xác nhận hủy vé
            </Button>
            <Button
              disabled={busy}
              className="small"
              onClick={() => setConfirmCancel(false)}
            >
              Giữ vé
            </Button>
          </div>
        </div>
      )}
    </section>
  );
}
