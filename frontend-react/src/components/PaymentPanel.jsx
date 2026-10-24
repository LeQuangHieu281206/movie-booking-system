import React, { useEffect, useRef, useState } from "react";
import { Button, ErrorBox, Icon } from "./UI.jsx";
import { statusText, money } from "../lib/domain.js";
import { assertBooking } from "../booking/flow.js";
import { bookingStore } from "../booking/storage.js";
import { runBookingAction } from "../booking/payment.js";
import "../booking/booking.css";

export default function PaymentPanel({
  api,
  mode,
  booking,
  onUpdate,
  onBusy = () => {},
}) {
  const [current, setCurrent] = useState(booking),
    [verified, setVerified] = useState(false);
  const [busy, setBusy] = useState(false),
    [reading, setReading] = useState(true),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [confirmCancel, setConfirmCancel] = useState(false);
  const [intent, setIntent] = useState(() =>
    bookingStore.action(mode, booking),
  );
  const alive = useRef(false),
    controller = useRef(null),
    locked = useRef(false),
    callbacks = useRef({ onUpdate, onBusy });
  callbacks.current = { onUpdate, onBusy };
  useEffect(() => {
    setCurrent(booking);
  }, [booking]);
  function publish(latest) {
    if (!alive.current) return;
    setCurrent(latest);
    setVerified(true);
    callbacks.current.onUpdate(latest);
    if (latest.status !== "PENDING") bookingStore.clearAction(mode, latest);
    setIntent(bookingStore.action(mode, latest));
  }
  useEffect(() => {
    alive.current = true;
    const request = new AbortController();
    controller.current = request;
    api
      .booking(booking.bookingId, { signal: request.signal })
      .then((latest) => {
        if (!request.signal.aborted) publish(assertBooking(latest, booking));
      })
      .catch((error) => {
        if (!request.signal.aborted && alive.current) {
          setError(error.message);
          setVerified(false);
        }
      })
      .finally(() => {
        if (!request.signal.aborted && alive.current) setReading(false);
      });
    return () => {
      alive.current = false;
      controller.current?.abort();
    };
  }, [
    api,
    mode,
    booking.bookingId,
    booking.bookingCode,
    booking.userId,
    booking.showtimeId,
  ]);
  async function refresh() {
    if (locked.current) return;
    locked.current = true;
    setReading(true);
    setError("");
    setNotice("");
    controller.current?.abort();
    const request = new AbortController();
    controller.current = request;
    try {
      const latest = assertBooking(
        await api.booking(booking.bookingId, { signal: request.signal }),
        booking,
      );
      if (!request.signal.aborted && alive.current) {
        publish(latest);
        if (latest.status === "PENDING")
          setNotice(
            "Vé vẫn đang chờ thanh toán hoặc xác nhận. Nếu đã gửi yêu cầu, vui lòng chờ và cập nhật lại trạng thái.",
          );
      }
    } catch (error) {
      if (!request.signal.aborted && alive.current) {
        setError(error.message);
        setVerified(false);
      }
    } finally {
      locked.current = false;
      if (alive.current) setReading(false);
    }
  }
  async function action(kind) {
    if (locked.current || !verified) return;
    locked.current = true;
    setBusy(true);
    callbacks.current.onBusy(true);
    setError("");
    setNotice("");
    controller.current?.abort();
    const request = new AbortController();
    controller.current = request;
    try {
      const latest = await runBookingAction({
        api,
        mode,
        booking: current,
        kind,
        store: bookingStore,
        signal: request.signal,
        onUpdate: publish,
      });
      if (alive.current && !request.signal.aborted) {
        setConfirmCancel(false);
        if (latest.status === "PENDING")
          setNotice(
            "Đã gửi yêu cầu nhưng vé chưa được xác nhận. Bạn có thể cập nhật trạng thái; không cần gửi lại yêu cầu.",
          );
      }
    } catch (error) {
      if (alive.current && !request.signal.aborted)
        setError(
          `${error.message}${bookingStore.action(mode, current) ? " Chưa xác định được kết quả cuối cùng; hãy cập nhật trạng thái trước khi thao tác tiếp." : ""}`,
        );
    } finally {
      locked.current = false;
      if (alive.current) {
        setBusy(false);
        setIntent(bookingStore.action(mode, current));
      }
      callbacks.current.onBusy(false);
    }
  }
  const blocked = busy || reading || !verified;
  return (
    <section className="payment-panel pvt-payment">
      <div className="subheading">
        <h3>Trạng thái vé</h3>
        <span
          className={`badge ${String(current.status).toLowerCase()}`}
          role="status"
        >
          {statusText(current.status)}
        </span>
      </div>
      {reading && (
        <p className="hint" role="status">
          Đang kiểm tra trạng thái mới nhất…
        </p>
      )}
      {current.status === "PAID" ? (
        <div className="success-note">
          <Icon name="check" />
          Booking đã xác nhận thanh toán. Hẹn bạn tại rạp!
        </div>
      ) : current.status === "PENDING" ? (
        <>
          <p>
            Thanh toán mô phỏng · {money(current.totalAmount)}. Không thu tiền
            hoặc yêu cầu thông tin thẻ ngân hàng.
          </p>
          <div className="button-row">
            <Button
              primary
              disabled={blocked || Boolean(intent)}
              onClick={() => action("pay")}
            >
              {busy ? "Đang xử lý…" : "Mô phỏng thanh toán thành công"}
            </Button>
            <Button
              disabled={blocked || Boolean(intent)}
              onClick={() => action("fail")}
            >
              Thử thanh toán thất bại
            </Button>
          </div>
          {intent && (
            <div className="notice" role="status">
              Đã ghi nhận lần gửi yêu cầu{" "}
              {intent.kind === "cancel" ? "hủy vé" : "thanh toán"} cho vé này.
              Hãy cập nhật trạng thái để kiểm tra kết quả. Tải lại trang không
              gửi lại yêu cầu.
            </div>
          )}
        </>
      ) : (
        <p>
          Vé{" "}
          {current.status === "CANCELLED"
            ? "đã hủy"
            : current.status === "EXPIRED"
              ? "đã hết hạn"
              : "không ở trạng thái có thể thanh toán"}
          . Bạn có thể chọn suất chiếu và đặt vé mới.
        </p>
      )}
      {notice && (
        <div className="notice" role="status">
          {notice}
        </div>
      )}
      {error && <ErrorBox message={error} />}
      <div className="button-row payment-secondary">
        <Button className="small" disabled={busy || reading} onClick={refresh}>
          <Icon name="refresh" size={15} />
          Cập nhật trạng thái
        </Button>
        {current.status === "PENDING" && !intent && !confirmCancel && (
          <Button
            disabled={blocked}
            className="small danger"
            onClick={() => setConfirmCancel(true)}
          >
            Hủy vé đang chờ
          </Button>
        )}
      </div>
      {confirmCancel && current.status === "PENDING" && !intent && (
        <div className="notice">
          <p>Hủy booking này và trả lại các ghế đang giữ?</p>
          <div className="button-row">
            <Button
              disabled={blocked}
              className="small danger"
              onClick={() => action("cancel")}
            >
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
