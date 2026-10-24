import React, { useEffect, useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { money, statusText } from "../lib/domain.js";
import { Button, Empty, ErrorBox, Icon, Loading } from "../components/UI.jsx";
import CustomerForm from "../components/CustomerForm.jsx";
import PaymentPanel from "../components/PaymentPanel.jsx";
import TicketReceipt from "../booking/TicketReceipt.jsx";
import {
  filterTickets,
  readableDate,
  sameId,
  assertUserBookings,
} from "../booking/flow.js";
import { bookingStore } from "../booking/storage.js";
import "../booking/booking.css";

function TicketList({ api, mode, user, movies, revision, onBusy }) {
  const result = useLoad(
    async (options) =>
      assertUserBookings(await api.bookings(user.userId, options), user.userId),
    [api, user.userId, revision],
  );
  const shows = useLoad((options) => api.showtimes("", options), [api]);
  const [updates, setUpdates] = useState({}),
    [expanded, setExpanded] = useState(null),
    [busyId, setBusyId] = useState(null);
  const [status, setStatus] = useState(""),
    [query, setQuery] = useState(""),
    [order, setOrder] = useState("newest");
  useEffect(() => {
    if (result.data) setUpdates({});
  }, [result.data]);
  if (result.loading) return <Loading label="Đang tải vé của bạn…" />;
  if (result.error)
    return <ErrorBox message={result.error} onRetry={result.reload} />;
  if (!result.data.length)
    return (
      <Empty title="Bạn chưa có vé nào">
        Chọn một bộ phim và đặt chỗ cho cuộc hẹn tiếp theo.
      </Empty>
    );
  const all = result.data.map(
    (original) => updates[original.bookingId] || original,
  );
  function details(booking) {
    const show = shows.data?.find((show) =>
      sameId(show.showtimeId, booking.showtimeId),
    );
    const movie = movies.find((movie) => sameId(movie.movieId, show?.movieId));
    const receipt = bookingStore.receipt(mode, booking);
    return {
      show,
      title:
        movie?.title || receipt?.title || `Suất chiếu #${booking.showtimeId}`,
      receipt,
    };
  }
  const visible = filterTickets(all, {
    query,
    status,
    order,
    titleFor: (booking) => details(booking).title,
  });
  const tabs = [
    ["", "Tất cả"],
    ["PENDING", "Chờ thanh toán"],
    ["PAID", "Đã thanh toán"],
    ["CANCELLED", "Đã hủy"],
    ["EXPIRED", "Hết hạn"],
  ];
  return (
    <div className="pvt-tickets">
      <div className="subheading">
        <h3>{all.length} vé đã đặt</h3>
        <Button
          className="small"
          disabled={busyId !== null}
          onClick={result.reload}
        >
          <Icon name="refresh" size={15} />
          Tải lại danh sách
        </Button>
      </div>
      <div
        className="pvt-ticket-tabs"
        role="group"
        aria-label="Lọc trạng thái vé"
      >
        {tabs.map(([value, label]) => (
          <button
            key={value}
            disabled={busyId !== null}
            aria-pressed={status === value}
            className={status === value ? "active" : ""}
            onClick={() => setStatus(value)}
          >
            {label}
            <span>
              {value
                ? all.filter((booking) => booking.status === value).length
                : all.length}
            </span>
          </button>
        ))}
      </div>
      <div className="pvt-ticket-filters">
        <label className="search-field">
          <Icon name="search" size={17} />
          <input
            disabled={busyId !== null}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            aria-label="Tìm vé theo mã hoặc tên phim"
            placeholder="Tìm mã đặt vé hoặc tên phim…"
          />
        </label>
        <label>
          <span className="sr-only">Sắp xếp vé</span>
          <select
            disabled={busyId !== null}
            value={order}
            onChange={(event) => setOrder(event.target.value)}
          >
            <option value="newest">Mới nhất trước</option>
            <option value="oldest">Cũ nhất trước</option>
          </select>
        </label>
        <span className="hint" role="status">
          Hiển thị {visible.length}/{all.length} vé
        </span>
      </div>
      {shows.error && (
        <div className="notice">
          <p>
            Chưa tải được thông tin lịch chiếu. Mã vé, tổng tiền và trạng thái
            booking vẫn hiển thị được.
          </p>
          <Button
            className="small"
            disabled={busyId !== null}
            onClick={shows.reload}
          >
            Tải lại thông tin lịch chiếu
          </Button>
        </div>
      )}
      {!visible.length ? (
        <Empty title="Không có vé phù hợp">
          <span>Thử đổi mã tìm kiếm hoặc bộ lọc.</span>
          <Button
            className="small pvt-clear-filter"
            onClick={() => {
              setStatus("");
              setQuery("");
            }}
          >
            Xóa bộ lọc
          </Button>
        </Empty>
      ) : (
        <div className="tickets-list">
          {visible.map((booking) => {
            const { show, title, receipt } = details(booking),
              opened = expanded === booking.bookingId;
            return (
              <article className="ticket-card" key={booking.bookingId}>
                <div className="ticket-card-main">
                  <span className="ticket-cut">
                    <Icon name="ticket" size={29} />
                  </span>
                  <div>
                    <span className="eyebrow">{booking.bookingCode}</span>
                    <h3>{title}</h3>
                    <p>
                      {show
                        ? `${show.cinemaName} · ${show.roomName} · ${readableDate(show.startTime)}`
                        : receipt
                          ? `${receipt.cinemaName} · ${receipt.roomName} · ${readableDate(receipt.startTime)}`
                          : `Suất chiếu #${booking.showtimeId}`}
                    </p>
                    <small>Đặt lúc {readableDate(booking.bookingTime)}</small>
                    {receipt && (
                      <div className="pvt-ticket-seats">
                        Ghế trên máy này:{" "}
                        <b>
                          {receipt.seats.map((seat) => seat.label).join(", ")}
                        </b>
                      </div>
                    )}
                  </div>
                  <div className="ticket-card-side">
                    <span
                      className={`badge ${String(booking.status).toLowerCase()}`}
                    >
                      {statusText(booking.status)}
                    </span>
                    <strong>{money(booking.totalAmount)}</strong>
                    <Button
                      className="small"
                      disabled={busyId !== null}
                      aria-expanded={opened}
                      aria-controls={`ticket-details-${booking.bookingId}`}
                      onClick={() =>
                        setExpanded(opened ? null : booking.bookingId)
                      }
                    >
                      {opened ? "Thu gọn" : "Chi tiết vé"}
                    </Button>
                  </div>
                </div>
                {opened && (
                  <div
                    id={`ticket-details-${booking.bookingId}`}
                    className="pvt-ticket-details"
                  >
                    <PaymentPanel
                      key={`${mode}-${booking.bookingCode}`}
                      api={api}
                      mode={mode}
                      booking={booking}
                      onUpdate={(latest) =>
                        setUpdates((current) => ({
                          ...current,
                          [latest.bookingId]: latest,
                        }))
                      }
                      onBusy={(active) => {
                        setBusyId(active ? booking.bookingId : null);
                        onBusy(active);
                      }}
                    />
                    <TicketReceipt mode={mode} booking={booking} />
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function Tickets({ api, mode, user, onUser, movies, revision }) {
  const [busy, setBusy] = useState(false);
  return (
    <section className="container page-section">
      <span className="eyebrow">NHỮNG CUỘC HẸN TRÊN MÀN ẢNH LỚN</span>
      <h1>Vé của tôi</h1>
      <p>Tra cứu và theo dõi trạng thái vé theo hồ sơ khách hàng.</p>
      <CustomerForm api={api} user={user} onUser={onUser} disabled={busy} />
      {user && (
        <TicketList
          key={`${mode}-${user.userId}`}
          api={api}
          mode={mode}
          user={user}
          movies={movies}
          revision={revision}
          onBusy={setBusy}
        />
      )}
    </section>
  );
}
