import React, { useRef, useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { bookingPayload, money, time, date } from "../lib/domain.js";
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

export default function Booking({
  api,
  showtimeId,
  movie,
  user,
  onUser,
  onClose,
  onTickets,
}) {
  const result = useLoad(
    async (options) => {
      const show = await api.showtime(showtimeId, options);
      const seats = await api.seats(show, options);
      return { show, seats };
    },
    [api, showtimeId],
  );
  const [selected, setSelected] = useState([]),
    [step, setStep] = useState(1),
    [method, setMethod] = useState("BANK_TRANSFER");
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [booking, setBooking] = useState(null),
    [uncertain, setUncertain] = useState(false),
    [agreed, setAgreed] = useState(false);
  const [freshSeats, setFreshSeats] = useState(null);
  const submitting = useRef(false);
  const show = result.data?.show,
    seats = freshSeats || result.data?.seats || [];
  const chosen = selected
    .map((id) => seats.find((seat) => seat.showtimeSeatId === id))
    .filter(Boolean);
  function toggle(seat) {
    setError("");
    if (selected.includes(seat.showtimeSeatId))
      setSelected(selected.filter((id) => id !== seat.showtimeSeatId));
    else if (selected.length >= 8)
      setError("Bạn có thể chọn tối đa 8 ghế cho mỗi lần đặt.");
    else setSelected([...selected, seat.showtimeSeatId]);
  }
  function reloadSeats() {
    setSelected([]);
    setFreshSeats(null);
    setError("");
    result.reload();
  }
  async function submit() {
    if (submitting.current || uncertain) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    let posted = false;
    try {
      const latestShow = await api.showtime(showtimeId);
      const latestSeats = await api.seats(latestShow);
      setFreshSeats(latestSeats);
      const payload = bookingPayload(
        user,
        latestShow,
        latestSeats,
        selected,
        method,
      );
      if (Number(latestShow.basePrice) !== Number(show.basePrice))
        throw new Error(
          "Giá vé đã thay đổi. Hãy đóng cửa sổ và chọn lại suất chiếu để xác nhận giá mới.",
        );
      if (!agreed)
        throw new Error(
          "Vui lòng kiểm tra thông tin và xác nhận trước khi đặt.",
        );
      posted = true;
      const saved = await api.createBooking(payload);
      setBooking(saved);
      setStep(3);
    } catch (error) {
      if (posted && (!error.status || error.status >= 500)) {
        setUncertain(true);
        setError(
          `${error.message} Chưa xác định được booking đã tạo hay chưa. Hãy mở Vé của tôi để kiểm tra trước khi đặt lại.`,
        );
      } else {
        setError(error.message);
        if (posted) {
          setSelected([]);
          setStep(1);
          setFreshSeats(null);
          result.reload();
        }
      }
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }
  return (
    <Modal
      wide
      title={booking ? "Thông tin vé của bạn" : "Đặt một chỗ cho cảm xúc"}
      onClose={onClose}
      busy={busy}
    >
      <ol className="booking-steps">
        {["Chọn ghế", "Thông tin & xác nhận", "Vé & thanh toán"].map(
          (title, index) => (
            <li key={title} className={step >= index + 1 ? "active" : ""}>
              <span>{index + 1}</span>
              {title}
            </li>
          ),
        )}
      </ol>
      {result.loading ? (
        <Loading label="Đang tải sơ đồ ghế…" />
      ) : result.error ? (
        <ErrorBox message={result.error} onRetry={reloadSeats} />
      ) : (
        <div className="booking-layout">
          <div className="booking-main">
            {step === 1 && (
              <>
                <div className="subheading">
                  <h3>Chọn vị trí yêu thích</h3>
                  <Button className="small" onClick={reloadSeats}>
                    <Icon name="refresh" size={14} />
                    Tải lại ghế
                  </Button>
                </div>
                <p className="hint">
                  Ghế được xác nhận khi backend tạo booking. Tối đa 8 ghế mỗi
                  lần đặt.
                </p>
                {!seats.length ? (
                  <Empty title="Suất chiếu chưa có ghế">
                    Nhóm cần kiểm tra ghế của phòng và dữ liệu ghế theo suất
                    chiếu.
                  </Empty>
                ) : (
                  <>
                    <div className="seat-map-scroll">
                      <div className="seat-map">
                        <div className="screen">
                          <span>MÀN HÌNH</span>
                        </div>
                        {[...new Set(seats.map((seat) => seat.seatRow))].map(
                          (row) => (
                            <div className="seat-row" key={row}>
                              <span className="row-label">{row}</span>
                              <div className="seat-row-inner">
                                {seats
                                  .filter((seat) => seat.seatRow === row)
                                  .map((seat) => (
                                    <button
                                      key={seat.showtimeSeatId}
                                      disabled={seat.status !== "AVAILABLE"}
                                      className={`seat ${seat.seatType === "VIP" ? "vip" : ""} ${selected.includes(seat.showtimeSeatId) ? "selected" : ""}`}
                                      aria-label={`Ghế ${seat.label}${seat.status !== "AVAILABLE" ? ", không còn trống" : ""}`}
                                      aria-pressed={selected.includes(
                                        seat.showtimeSeatId,
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
                          ),
                        )}
                      </div>
                    </div>
                    <div className="seat-legend">
                      <span>
                        <i />
                        Ghế trống
                      </span>
                      <span>
                        <i className="vip" />
                        Ghế VIP
                      </span>
                      <span>
                        <i className="selected" />
                        Đang chọn
                      </span>
                      <span>
                        <i className="booked" />
                        Đã đặt / đang giữ
                      </span>
                    </div>
                    <p className="hint">
                      Giá theo suất chiếu áp dụng cho mỗi ghế; backend hiện chưa
                      có bảng phụ thu VIP.
                    </p>
                  </>
                )}
              </>
            )}
            {step === 2 && (
              <>
                <h3>Thông tin khách hàng</h3>
                <CustomerForm
                  api={api}
                  user={user}
                  onUser={onUser}
                  disabled={busy}
                />
                <label className="payment-method">
                  Phương thức ghi nhận
                  <select
                    value={method}
                    disabled={busy}
                    onChange={(event) => setMethod(event.target.value)}
                  >
                    <option value="BANK_TRANSFER">Chuyển khoản</option>
                    <option value="CASH">Tiền mặt</option>
                    <option value="CARD">Thẻ</option>
                    <option value="E_WALLET">Ví điện tử</option>
                  </select>
                </label>
                <div className="notice">
                  Bước này tạo booking chờ thanh toán. Phần thanh toán tiếp theo
                  là mô phỏng, không thu tiền thật.
                </div>
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={agreed}
                    disabled={busy}
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
                    Khách hàng #{booking.userId} · Booking #{booking.bookingId}
                  </p>
                  <p>Booking đã được trả về từ backend.</p>
                </div>
                <PaymentPanel
                  api={api}
                  booking={booking}
                  onUpdate={setBooking}
                  onBusy={setBusy}
                />
              </>
            )}
            {error && <ErrorBox message={error} />}
            <div className="booking-actions">
              {step === 1 && (
                <Button
                  primary
                  disabled={!selected.length || !seats.length}
                  onClick={() => setStep(2)}
                >
                  Tiếp tục
                  <Icon name="arrow" size={16} />
                </Button>
              )}
              {step === 2 && (
                <>
                  <Button
                    disabled={busy || uncertain}
                    onClick={() => setStep(1)}
                  >
                    Quay lại chọn ghế
                  </Button>
                  <Button
                    primary
                    disabled={!user || !agreed || busy || uncertain}
                    onClick={submit}
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
              <dd>
                {time(show.startTime)} · {date(show.startTime)}
              </dd>
              <dt>Ghế của bạn</dt>
              <dd>
                {chosen.length
                  ? chosen.map((seat) => seat.label).join(", ")
                  : "Chưa chọn ghế"}
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
            <p className="hint">{selected.length} vé · Dữ liệu backend</p>
          </aside>
        </div>
      )}
    </Modal>
  );
}
