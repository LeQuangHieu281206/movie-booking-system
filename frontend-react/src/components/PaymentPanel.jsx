import React, { useEffect, useRef, useState } from "react";
import { Button, ErrorBox, Icon } from "./UI.jsx";
import { statusText, money } from "../lib/domain.js";

// SePay: tạo QR chuyển khoản, booking chỉ PAID sau khi webhook báo tiền về.
export default function PaymentPanel({
  api,
  booking,
  onUpdate,
  onBusy = () => {},
}) {
  const [qr, setQr] = useState("");
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [confirmCancel, setConfirmCancel] = useState(false);
  const lock = useRef(false);
  const update = useRef(onUpdate);
  update.current = onUpdate;
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
  function createQr() {
    return run(async () => {
      const latest = await api.booking(booking.bookingId);
      update.current(latest);
      if (latest.status !== "PENDING") return;
      const payment = await api.createPayment(latest.bookingId);
      setQr(payment.qrUrl);
      setNotice(payment.message || "Quét mã QR để hoàn tất thanh toán.");
    });
  }
  function refresh() {
    return run(async () => {
      const latest = await api.booking(booking.bookingId);
      update.current(latest);
      if (latest.status === "PENDING")
        setNotice(
          "Booking vẫn đang chờ. Nếu vừa chuyển khoản, chờ webhook SePay vài giây rồi cập nhật lại.",
        );
    });
  }
  function cancel() {
    return run(async () => {
      const latest = await api.booking(booking.bookingId);
      if (latest.status !== "PENDING") {
        update.current(latest);
        return;
      }
      update.current(await api.cancel(booking.bookingId));
      setConfirmCancel(false);
    });
  }
  // Tự hỏi trạng thái tối đa 3 phút sau khi hiện QR, chờ webhook chuyển khoản.
  useEffect(() => {
    if (!qr || booking.status !== "PENDING") return;
    let alive = true,
      misses = 0;
    const timer = setInterval(async () => {
      if (++misses > 36) {
        clearInterval(timer);
        if (alive)
          setNotice(
            "Chưa nhận được xác nhận thanh toán. Hãy chuyển khoản đúng nội dung trong mã QR rồi bấm Cập nhật trạng thái.",
          );
        return;
      }
      try {
        const latest = await api.booking(booking.bookingId);
        if (alive) update.current(latest);
      } catch {
        /* Bỏ qua lần hỏi lỗi, lần sau thử lại. */
      }
    }, 5000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [qr, booking.status, api, booking.bookingId]);
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
            Số tiền cần chuyển: <strong>{money(booking.totalAmount)}</strong> ·
            thanh toán qua SePay (chuyển khoản ngân hàng).
          </p>
          {qr ? (
            <div className="qr-box">
              <img src={qr} alt="Mã QR chuyển khoản SePay" />
              <p className="hint">
                Mở ứng dụng ngân hàng và quét mã. Số tiền và nội dung chuyển
                khoản đã có trong mã; vé tự xác nhận sau khi webhook báo tiền
                về.
              </p>
            </div>
          ) : (
            <div className="button-row">
              <Button primary disabled={busy} onClick={createQr}>
                {busy ? "Đang tạo mã…" : "Tạo mã QR thanh toán"}
              </Button>
            </div>
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
        {booking.status === "PENDING" && !confirmCancel && (
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
