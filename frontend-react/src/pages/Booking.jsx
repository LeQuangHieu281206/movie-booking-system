import React, { useEffect, useRef, useState } from "react";
import { money, time, date, isBookable } from "../lib/domain.js";
import {
  Button,
  Empty,
  ErrorBox,
  Icon,
  Loading,
  Modal,
  Poster,
} from "../components/UI.jsx";
import CustomerForm from "../components/CustomerForm.jsx";
import PaymentPanel from "../components/PaymentPanel.jsx";
import TicketReceipt from "../booking/TicketReceipt.jsx";
import {
  prepareBooking,
  reconcileSeats,
  changedShow,
  sameId,
  assertCreatedBooking,
  assertUserBookings,
  readableDate,
} from "../booking/flow.js";
import { bookingStore } from "../booking/storage.js";
import { useInventory } from "../booking/useInventory.js";
import "../booking/booking.css";

export default function Booking({
  api,
  mode,
  showtimeId,
  movie,
  user,
  onUser,
  onClose,
  onTickets,
}) {
  const [selected, setSelected] = useState([]),
    [step, setStep] = useState(1),
    [method, setMethod] = useState("BANK_TRANSFER");
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [booking, setBooking] = useState(null),
    [agreed, setAgreed] = useState(false),
    [exitPrompt, setExitPrompt] = useState(false);
  const [now, setNow] = useState(Date.now()),
    [recovery, setRecovery] = useState(null),
    [acknowledged, setAcknowledged] = useState(false),
    [storageWarning, setStorageWarning] = useState(false);
  const submitting = useRef(false),
    previous = useRef(null),
    exitRef = useRef(null);
  const result = useInventory(api, showtimeId, busy || Boolean(booking));
  const show = result.data?.show,
    seats = result.data?.seats || [];
  const chosen = seats.filter((seat) =>
    selected.some((id) => sameId(id, seat.showtimeSeatId)),
  );
  const creation =
    user && !booking
      ? bookingStore.creation(mode, user.userId, showtimeId)
      : null;
  const uncertain = Boolean(creation);
  const bookable =
    show && isBookable(show) && now < new Date(show.startTime).getTime();
  const unavailable =
    result.loading || result.refreshing || Boolean(result.error) || !bookable;
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 10000);
    return () => clearInterval(timer);
  }, []);
  useEffect(() => {
    if (exitPrompt) exitRef.current?.scrollIntoView({ block: "nearest" });
  }, [exitPrompt]);
  useEffect(() => {
    if (!result.data || booking) return;
    const latest = result.data;
    setSelected((ids) => reconcileSeats(ids, latest.seats, showtimeId));
    const removed = selected.filter(
      (id) =>
        !reconcileSeats(selected, latest.seats, showtimeId).some((value) =>
          sameId(id, value),
        ),
    );
    if (removed.length) {
      setStep(1);
      setAgreed(false);
      setNotice(
        "Một số ghế vừa thay đổi trạng thái. Đã bỏ ghế không còn trống; vui lòng kiểm tra lại.",
      );
    } else if (
      previous.current &&
      changedShow(previous.current.show, latest.show)
    ) {
      setAgreed(false);
      setNotice(
        "Thông tin suất chiếu hoặc giá vé vừa thay đổi. Hãy kiểm tra và xác nhận lại trước khi đặt.",
      );
    }
    previous.current = latest;
  }, [result.data, booking, showtimeId]);
  function toggle(seat) {
    if (busy || unavailable || seat.status !== "AVAILABLE") return;
    if (
      !selected.some((id) => sameId(id, seat.showtimeSeatId)) &&
      selected.length >= 8
    ) {
      setNotice("Bạn có thể chọn tối đa 8 ghế cho mỗi lần đặt.");
      return;
    }
    setError("");
    setAgreed(false);
    setStep(1);
    setSelected((ids) => {
      if (ids.some((id) => sameId(id, seat.showtimeSeatId)))
        return ids.filter((id) => !sameId(id, seat.showtimeSeatId));
      if (ids.length >= 8) {
        return ids;
      }
      return [...ids, seat.showtimeSeatId];
    });
  }
  async function checkSelection(reviewOnly) {
    if (submitting.current || !show || uncertain || !selected.length) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    setNotice("");
    let posted = false;
    try {
      const review = await prepareBooking(api, {
        user,
        show,
        seats,
        ids: selected,
        paymentMethod: method,
        reviewOnly,
      });
      result.adopt(review.inventory);
      setSelected(review.ids);
      if (review.review) {
        setAgreed(false);
        setNotice(review.message);
        setStep(review.review === "show" && review.ids.length ? 2 : 1);
        return;
      }
      if (reviewOnly) {
        setStep(2);
        setAgreed(false);
        return;
      }
      if (!agreed)
        throw new Error(
          "Vui lòng xác nhận đã kiểm tra thông tin trước khi đặt.",
        );
      bookingStore.markCreation(mode, user.userId, showtimeId);
      posted = true;
      const saved = assertCreatedBooking(
        await api.createBooking(review.payload),
        review.payload,
      );
      setStorageWarning(
        !bookingStore.saveReceipt(
          mode,
          saved,
          review.inventory.show,
          movie,
          review.inventory.seats.filter((seat) =>
            review.ids.some((id) => sameId(id, seat.showtimeSeatId)),
          ),
        ),
      );
      bookingStore.clearCreation(mode, user.userId, showtimeId);
      setBooking(saved);
      setStep(3);
    } catch (error) {
      if (posted && error.status >= 400 && error.status < 500) {
        bookingStore.clearCreation(mode, user.userId, showtimeId);
        setSelected([]);
        setAgreed(false);
        setStep(1);
        result.refresh({ force: true });
      }
      setError(error.message);
      if (posted && (!error.status || error.status >= 500))
        setNotice(
          "Kết quả tạo booking chưa rõ. Yêu cầu đã được ghi nhớ; hãy kiểm tra Vé của tôi trước khi thử đặt lại.",
        );
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }
  async function checkUncertain() {
    if (submitting.current || !user) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    try {
      const bookings = assertUserBookings(
        await api.bookings(user.userId),
        user.userId,
      );
      const candidates = bookings.filter(
        (item) =>
          sameId(item.showtimeId, showtimeId) &&
          new Date(item.bookingTime).getTime() >= (creation?.at || 0) - 30000,
      );
      setRecovery({ at: Date.now(), candidates });
      setAcknowledged(false);
    } catch (error) {
      setError(error.message);
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }
  function allowRetry() {
    if (
      !acknowledged ||
      !recovery ||
      recovery.candidates.length ||
      !creation ||
      Date.now() - creation.at < 30000
    )
      return;
    bookingStore.clearCreation(mode, user.userId, showtimeId);
    setRecovery(null);
    setAcknowledged(false);
    setSelected([]);
    setAgreed(false);
    setStep(1);
    setError("");
    setNotice(
      "Đã mở lại bước chọn ghế theo xác nhận của bạn. Hãy kiểm tra kỹ trước khi gửi một yêu cầu mới.",
    );
    result.refresh();
  }
  function close() {
    if (busy) return;
    if (booking?.status === "PENDING" || uncertain) setExitPrompt(true);
    else onClose();
  }
  const counts = {
    available: seats.filter((seat) => seat.status === "AVAILABLE").length,
    held: seats.filter((seat) => seat.status === "HELD").length,
    booked: seats.filter((seat) => seat.status === "BOOKED").length,
  };
  return (
    <Modal
      wide
      title={booking ? "Thông tin vé của bạn" : "Đặt một chỗ cho cảm xúc"}
      onClose={close}
      busy={busy}
    >
      <div className="pvt-booking">
        {exitPrompt && (
          <div className="notice pvt-exit" ref={exitRef} role="alert">
            <p>
              {uncertain
                ? "Kết quả đặt vé chưa rõ. Hãy kiểm tra lịch sử để tránh gửi trùng."
                : "Booking đang chờ thanh toán. Đóng cửa sổ không tự hủy vé hoặc trả ghế."}
            </p>
            <div className="button-row">
              <Button className="small" onClick={() => setExitPrompt(false)}>
                Tiếp tục xử lý vé
              </Button>
              <Button className="small" primary onClick={onTickets}>
                Xem Vé của tôi
              </Button>
              <Button className="small" onClick={onClose}>
                Để chờ và đóng
              </Button>
            </div>
          </div>
        )}
        <ol className="booking-steps">
          {["Chọn ghế", "Thông tin & xác nhận", "Vé & thanh toán"].map(
            (title, index) => (
              <li
                key={title}
                className={step >= index + 1 ? "active" : ""}
                aria-current={step === index + 1 ? "step" : undefined}
              >
                <span>{index + 1}</span>
                {title}
              </li>
            ),
          )}
        </ol>
        {result.loading && !result.data ? (
          <Loading label="Đang tải sơ đồ ghế…" />
        ) : !show ? (
          <ErrorBox
            message={result.error || "Chưa tải được suất chiếu."}
            onRetry={result.refresh}
          />
        ) : (
          <>
            {result.error && (
              <ErrorBox
                message={`Chưa cập nhật được ghế: ${result.error}`}
                onRetry={result.refresh}
              />
            )}
            {!bookable && !booking && (
              <ErrorBox message="Suất chiếu này đã bắt đầu hoặc ngừng nhận đặt vé. Vui lòng chọn suất khác." />
            )}
            {uncertain && (
              <div className="pvt-recovery notice" role="alert">
                <h3>Kiểm tra yêu cầu đặt vé trước đó</h3>
                <p>
                  Không tự gửi lại khi chưa biết yêu cầu trước đã tạo vé hay
                  chưa.
                </p>
                <div className="button-row">
                  <Button
                    className="small"
                    disabled={busy}
                    onClick={checkUncertain}
                  >
                    Kiểm tra booking gần đây
                  </Button>
                  <Button className="small" disabled={busy} onClick={onTickets}>
                    Mở Vé của tôi
                  </Button>
                </div>
                {recovery &&
                  (recovery.candidates.length ? (
                    <p>
                      Đã tìm thấy booking cùng suất gần thời điểm gửi:{" "}
                      {recovery.candidates
                        .map((item) => item.bookingCode)
                        .join(", ")}
                      . Hãy kiểm tra chi tiết trong Vé của tôi; danh sách này
                      chưa đủ để tự khẳng định đó là cùng ghế.
                    </p>
                  ) : (
                    <>
                      <p>
                        Hiện chưa tìm thấy booking cùng suất gần thời điểm gửi.
                        Yêu cầu trước vẫn có thể đang được xử lý.
                      </p>
                      <label className="checkbox-label">
                        <input
                          type="checkbox"
                          checked={acknowledged}
                          onChange={(event) =>
                            setAcknowledged(event.target.checked)
                          }
                        />
                        Tôi đã kiểm tra lịch sử và hiểu rằng gửi lại vẫn có thể
                        tạo booking trùng.
                      </label>
                      <Button
                        className="small"
                        disabled={
                          !acknowledged || now - creation.at < 30000 || busy
                        }
                        onClick={allowRetry}
                      >
                        Cho phép chọn ghế để thử lại
                      </Button>
                      {now - creation.at < 30000 && (
                        <p className="hint">
                          Vui lòng chờ ít nhất 30 giây sau lần gửi trước.
                        </p>
                      )}
                    </>
                  ))}
              </div>
            )}
            <div className="booking-layout">
              <div className="booking-main">
                {step === 1 && !booking && (
                  <>
                    <div className="subheading">
                      <h3>Chọn vị trí yêu thích</h3>
                      <Button
                        className="small"
                        disabled={busy || result.refreshing}
                        onClick={result.refresh}
                      >
                        <Icon name="refresh" size={14} />
                        {result.refreshing ? "Đang cập nhật…" : "Cập nhật ghế"}
                      </Button>
                    </div>
                    <p className="hint">
                      Tối đa 8 ghế mỗi lần đặt. Sơ đồ tự cập nhật sau mỗi 30
                      giây khi bạn đang chọn.
                    </p>
                    <div className="pvt-seat-stats" aria-live="polite">
                      <span>
                        <b>{counts.available}</b> ghế trống
                      </span>
                      <span>
                        <b>{counts.held}</b> đang giữ
                      </span>
                      <span>
                        <b>{counts.booked}</b> đã đặt
                      </span>
                      <small>Cập nhật {time(result.data.loadedAt)}</small>
                    </div>
                    {!seats.length ? (
                      <Empty title="Suất chiếu chưa có ghế">
                        Vui lòng chọn suất khác hoặc liên hệ nhóm để kiểm tra dữ
                        liệu phòng.
                      </Empty>
                    ) : (
                      <>
                        <div className="seat-map-scroll">
                          <div className="seat-map">
                            <div className="screen">
                              <span>MÀN HÌNH</span>
                            </div>
                            {[
                              ...new Set(seats.map((seat) => seat.seatRow)),
                            ].map((row) => (
                              <div className="seat-row" key={row}>
                                <span className="row-label">{row}</span>
                                <div className="seat-row-inner">
                                  {seats
                                    .filter((seat) => seat.seatRow === row)
                                    .map((seat) => (
                                      <button
                                        key={seat.showtimeSeatId}
                                        disabled={
                                          busy ||
                                          unavailable ||
                                          seat.status !== "AVAILABLE"
                                        }
                                        className={`seat ${seat.seatType === "VIP" ? "vip" : ""} ${seat.status === "HELD" ? "held" : ""} ${selected.some((id) => sameId(id, seat.showtimeSeatId)) ? "selected" : ""}`}
                                        aria-label={`Ghế ${seat.label}${seat.status !== "AVAILABLE" ? ", không còn trống" : ""}`}
                                        aria-pressed={selected.some((id) =>
                                          sameId(id, seat.showtimeSeatId),
                                        )}
                                        title={`${seat.label} · ${seat.status} · ${seat.seatType || "NORMAL"}`}
                                        onClick={() => toggle(seat)}
                                      >
                                        {seat.seatNumber}
                                      </button>
                                    ))}
                                </div>
                                <span className="row-label">{row}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                        <div className="seat-legend">
                          <span>
                            <i />
                            Ghế trống
                          </span>
                          <span>
                            <i className="vip" />
                            VIP
                          </span>
                          <span>
                            <i className="selected" />
                            Đang chọn
                          </span>
                          <span>
                            <i className="held" />
                            Đang giữ
                          </span>
                          <span>
                            <i className="booked" />
                            Đã đặt
                          </span>
                        </div>
                        <p className="hint">
                          Giá theo suất chiếu áp dụng như nhau cho mỗi ghế.
                        </p>
                      </>
                    )}
                  </>
                )}
                {step === 2 && !booking && (
                  <>
                    <h3>Thông tin khách hàng</h3>
                    <CustomerForm
                      api={api}
                      user={user}
                      onUser={onUser}
                      disabled={busy || uncertain}
                    />
                    <label className="payment-method">
                      Phương thức ghi nhận
                      <select
                        value={method}
                        disabled={busy || uncertain}
                        onChange={(event) => {
                          setMethod(event.target.value);
                          setAgreed(false);
                        }}
                      >
                        <option value="BANK_TRANSFER">Chuyển khoản</option>
                        <option value="CASH">Tiền mặt</option>
                        <option value="CARD">Thẻ</option>
                        <option value="E_WALLET">Ví điện tử</option>
                      </select>
                    </label>
                    <div className="notice">
                      Xác nhận sẽ tạo booking chờ thanh toán. Thanh toán tiếp
                      theo là mô phỏng, không thu tiền thật.
                    </div>
                    <label className="checkbox-label">
                      <input
                        type="checkbox"
                        checked={agreed}
                        disabled={busy || uncertain}
                        onChange={(event) => setAgreed(event.target.checked)}
                      />
                      Tôi đã kiểm tra phim, rạp, suất chiếu, ghế và tổng tiền.
                    </label>
                  </>
                )}
                {booking && (
                  <>
                    <div className="ticket-confirmation">
                      <span className="round-icon">
                        <Icon name="ticket" size={28} />
                      </span>
                      <p>MÃ ĐẶT VÉ</p>
                      <h2>{booking.bookingCode}</h2>
                      <p>
                        Khách hàng #{booking.userId} · Booking #
                        {booking.bookingId}
                      </p>
                      <p>
                        {mode === "demo"
                          ? "Vé mẫu, chỉ lưu trong trình duyệt này."
                          : "Booking đã được trả về từ backend."}
                      </p>
                    </div>
                    <PaymentPanel
                      api={api}
                      mode={mode}
                      booking={booking}
                      onUpdate={setBooking}
                      onBusy={setBusy}
                    />
                    <TicketReceipt mode={mode} booking={booking} />
                    {storageWarning && (
                      <p className="hint">
                        Trình duyệt không cho lưu bền thông tin ghế. Hãy tải bản
                        ghi vé trước khi đóng trang.
                      </p>
                    )}
                  </>
                )}
                {notice && (
                  <div className="notice" role="status">
                    {notice}
                  </div>
                )}
                {error && <ErrorBox message={error} />}
                <div className="booking-actions">
                  {step === 1 && !booking && (
                    <Button
                      primary
                      disabled={
                        busy || unavailable || !selected.length || uncertain
                      }
                      onClick={() => checkSelection(true)}
                    >
                      {busy ? "Đang kiểm tra…" : "Tiếp tục"}
                      <Icon name="arrow" size={16} />
                    </Button>
                  )}
                  {step === 2 && !booking && (
                    <>
                      <Button
                        disabled={busy}
                        onClick={() => {
                          setStep(1);
                          setAgreed(false);
                        }}
                      >
                        Quay lại chọn ghế
                      </Button>
                      <Button
                        primary
                        disabled={
                          !user ||
                          !agreed ||
                          !selected.length ||
                          busy ||
                          uncertain ||
                          unavailable
                        }
                        onClick={() => checkSelection(false)}
                      >
                        {busy ? "Đang xác nhận…" : "Xác nhận đặt vé"}
                      </Button>
                    </>
                  )}
                  {(booking || uncertain) && (
                    <Button disabled={busy} onClick={onTickets}>
                      Xem Vé của tôi
                      <Icon name="arrow" size={16} />
                    </Button>
                  )}
                </div>
              </div>
              <aside className="booking-summary">
                {movie && (
                  <div className="summary-movie">
                    <Poster movie={movie} />
                    <div>
                      <span className="eyebrow">BỘ PHIM CỦA BẠN</span>
                      <h3>{movie.title}</h3>
                      <p>
                        {movie.ageRestriction} · {movie.durationMinutes} phút
                      </p>
                    </div>
                  </div>
                )}
                <dl>
                  <dt>Rạp chiếu</dt>
                  <dd>{show.cinemaName}</dd>
                  <dt>Phòng chiếu</dt>
                  <dd>
                    {show.roomName} · {show.roomType}
                  </dd>
                  <dt>Suất chiếu</dt>
                  <dd>{readableDate(show.startTime)}</dd>
                  <dt>Ghế của bạn</dt>
                  <dd>
                    {chosen.length ? (
                      <div className="pvt-seat-chips">
                        {chosen.map((seat) => (
                          <button
                            key={seat.showtimeSeatId}
                            disabled={busy || Boolean(booking) || uncertain}
                            aria-label={`Bỏ chọn ghế ${seat.label}`}
                            onClick={() => toggle(seat)}
                          >
                            {seat.label}
                            {!booking && <span aria-hidden="true"> ×</span>}
                          </button>
                        ))}
                      </div>
                    ) : (
                      "Chưa chọn ghế"
                    )}
                  </dd>
                  <dt>Giá vé / ghế</dt>
                  <dd>{money(show.basePrice)}</dd>
                </dl>
                <div className="summary-total">
                  <span>Tổng cộng</span>
                  <strong>
                    {money(
                      booking?.totalAmount ??
                        Number(show.basePrice) * selected.length,
                    )}
                  </strong>
                </div>
                <p className="hint">
                  {selected.length} vé ·{" "}
                  {mode === "demo" ? "Dữ liệu mẫu" : "Dữ liệu backend"}
                </p>
              </aside>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}
