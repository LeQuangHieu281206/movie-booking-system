import React, { useEffect, useRef, useState } from "react";
import { Button, ErrorBox, Icon } from "./UI.jsx";
import { statusText, money } from "../lib/domain.js";

const DEFAULT_BANK = "MBBank";
const DEFAULT_ACCOUNT = "1234567890";
const DEFAULT_HOLDER = "CINEROOM CINEMA";

function parseQrParams(qrUrl, bookingId) {
  const fallback = {
    bankName: DEFAULT_BANK,
    accountNumber: DEFAULT_ACCOUNT,
    accountName: DEFAULT_HOLDER,
    referenceCode: `DH${bookingId}`,
  };
  if (!qrUrl) return fallback;
  try {
    const url = new URL(qrUrl);
    return {
      bankName: url.searchParams.get("bank") || fallback.bankName,
      accountNumber: url.searchParams.get("acc") || fallback.accountNumber,
      accountName: fallback.accountName,
      referenceCode: url.searchParams.get("des") || fallback.referenceCode,
    };
  } catch {
    return fallback;
  }
}

// SePay: tạo QR chuyển khoản, hiển thị chi tiết chuyển khoản + sao chép nhanh + lịch sử giao dịch.
export default function PaymentPanel({
  api,
  booking,
  onUpdate,
  onBusy = () => {},
}) {
  const [qr, setQr] = useState("");
  const [meta, setMeta] = useState(() => parseQrParams("", booking.bookingId));
  const [history, setHistory] = useState([]);
  const [copiedField, setCopiedField] = useState("");
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [confirmCancel, setConfirmCancel] = useState(false);
  const lock = useRef(false);
  const update = useRef(onUpdate);
  update.current = onUpdate;

  async function loadPaymentHistory(bookingId) {
    if (typeof api.payments !== "function") return [];
    try {
      const items = await api.payments(bookingId);
      if (Array.isArray(items)) {
        setHistory(items);
        const pendingItem = items.find(
          (item) => item.status === "PENDING" && item.qrUrl,
        );
        if (pendingItem && !qr) {
          setQr(pendingItem.qrUrl);
          setMeta((prev) => ({
            ...parseQrParams(pendingItem.qrUrl, bookingId),
            referenceCode: pendingItem.referenceCode || prev.referenceCode,
          }));
        }
        return items;
      }
    } catch {
      /* Bỏ qua nếu dịch vụ lịch sử chưa phản hồi */
    }
    return [];
  }

  useEffect(() => {
    setMeta(parseQrParams(qr, booking.bookingId));
    loadPaymentHistory(booking.bookingId);
  }, [booking.bookingId]);

  async function copyValue(key, value) {
    try {
      await navigator.clipboard?.writeText(String(value));
      setCopiedField(key);
      setTimeout(() => {
        setCopiedField((curr) => (curr === key ? "" : curr));
      }, 1800);
    } catch {
      /* Clipboard không khả dụng */
    }
  }

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
      const parsed = parseQrParams(payment.qrUrl, latest.bookingId);
      setMeta({
        bankName: payment.bankName || parsed.bankName,
        accountNumber: payment.accountNumber || parsed.accountNumber,
        accountName: payment.accountName || parsed.accountName,
        referenceCode: payment.referenceCode || parsed.referenceCode,
      });
      setNotice(payment.message || "Quét mã QR để hoàn tất thanh toán.");
      await loadPaymentHistory(latest.bookingId);
    });
  }

  function refresh() {
    return run(async () => {
      const [latest] = await Promise.all([
        api.booking(booking.bookingId),
        loadPaymentHistory(booking.bookingId),
      ]);
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
        await loadPaymentHistory(booking.bookingId);
      } catch {
        /* Bỏ qua lần hỏi lỗi, lần sau thử lại. */
      }
    }, 5000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [qr, booking.status, api, booking.bookingId]);

  const latestFailed = history.find(
    (item) => item.status === "FAILED" && item.failureReason,
  );

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
            thanh toán tự động qua SePay (VietQR 24/7).
          </p>
          {latestFailed && (
            <ErrorBox
              message={`Giao dịch gần nhất chưa hợp lệ: ${latestFailed.failureReason}`}
            />
          )}
          {qr ? (
            <div className="sepay-checkout-grid">
              <div className="qr-box">
                <img src={qr} alt="Mã QR chuyển khoản SePay" />
                <span className="sepay-live-pill">
                  <span className="spinner" />
                  Đang chờ xác nhận tự động từ SePay…
                </span>
              </div>
              <div className="sepay-transfer-card">
                <div className="sepay-transfer-head">
                  <span className="eyebrow">THÔNG TIN CHUYỂN KHOẢN</span>
                  <p className="hint">
                    Quét mã QR hoặc chuyển khoản thủ công chính xác theo thông
                    tin dưới đây:
                  </p>
                </div>
                <div className="sepay-transfer-rows">
                  <div className="sepay-row">
                    <span>Ngân hàng</span>
                    <strong>{meta.bankName}</strong>
                  </div>
                  <div className="sepay-row">
                    <span>Chủ tài khoản</span>
                    <strong>{meta.accountName}</strong>
                  </div>
                  <div className="sepay-row">
                    <span>Số tài khoản</span>
                    <div className="sepay-copy-group">
                      <strong>{meta.accountNumber}</strong>
                      <button
                        type="button"
                        className="sepay-copy-btn"
                        onClick={() => copyValue("acc", meta.accountNumber)}
                      >
                        {copiedField === "acc" ? "Đã chép" : "Sao chép"}
                      </button>
                    </div>
                  </div>
                  <div className="sepay-row">
                    <span>Số tiền</span>
                    <div className="sepay-copy-group">
                      <strong className="sepay-highlight">
                        {money(booking.totalAmount)}
                      </strong>
                      <button
                        type="button"
                        className="sepay-copy-btn"
                        onClick={() => copyValue("amount", booking.totalAmount)}
                      >
                        {copiedField === "amount" ? "Đã chép" : "Sao chép"}
                      </button>
                    </div>
                  </div>
                  <div className="sepay-row">
                    <span>Nội dung CK (bắt buộc)</span>
                    <div className="sepay-copy-group">
                      <strong className="sepay-code">
                        {meta.referenceCode}
                      </strong>
                      <button
                        type="button"
                        className="sepay-copy-btn"
                        onClick={() => copyValue("des", meta.referenceCode)}
                      >
                        {copiedField === "des" ? "Đã chép" : "Sao chép"}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
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
        {booking.status === "PENDING" && qr && (
          <Button className="small" disabled={busy} onClick={createQr}>
            Làm mới mã QR
          </Button>
        )}
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
