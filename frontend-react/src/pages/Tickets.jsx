import React, { useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { date, money, statusText, time } from "../lib/domain.js";
import { Button, Empty, ErrorBox, Icon, Loading } from "../components/UI.jsx";
import CustomerForm from "../components/CustomerForm.jsx";
import PaymentPanel from "../components/PaymentPanel.jsx";

function TicketList({ api, mode, user, movies, revision }) {
  const result = useLoad(
    (options) => api.bookings(user.userId, options),
    [api, user.userId, revision],
  );
  const shows = useLoad((options) => api.showtimes("", options), [api]);
  const [updates, setUpdates] = useState({}),
    [expanded, setExpanded] = useState(null);
  if (result.loading) return <Loading label="Đang tải vé của bạn…" />;
  if (result.error)
    return <ErrorBox message={result.error} onRetry={result.reload} />;
  if (!result.data.length)
    return (
      <Empty title="Bạn chưa có vé nào">
        Chọn một bộ phim và đặt chỗ cho cuộc hẹn tiếp theo.
      </Empty>
    );
  return (
    <>
      <div className="subheading">
        <h3>{result.data.length} vé đã đặt</h3>
        <Button
          className="small"
          onClick={() => {
            setUpdates({});
            result.reload();
          }}
        >
          <Icon name="refresh" size={15} />
          Tải lại danh sách
        </Button>
      </div>
      {shows.error && (
        <p className="hint">
          Chưa tải được thông tin phim/rạp của vé: {shows.error}
        </p>
      )}
      <div className="tickets-list">
        {[...result.data]
          .sort((a, b) => new Date(b.bookingTime) - new Date(a.bookingTime))
          .map((original) => {
            const booking = updates[original.bookingId] || original;
            const show = shows.data?.find(
                (show) => show.showtimeId === booking.showtimeId,
              ),
              movie = movies.find((movie) => movie.movieId === show?.movieId);
            return (
              <article className="ticket-card" key={booking.bookingId}>
                <div className="ticket-card-main">
                  <span className="ticket-cut">
                    <Icon name="ticket" size={29} />
                  </span>
                  <div>
                    <span className="eyebrow">{booking.bookingCode}</span>
                    <h3>
                      {movie?.title || `Suất chiếu #${booking.showtimeId}`}
                    </h3>
                    <p>
                      {show
                        ? `${show.cinemaName} · ${show.roomName} · ${time(show.startTime)} ${date(show.startTime)}`
                        : `Booking #${booking.bookingId}`}
                    </p>
                    <small>
                      Đặt ngày {date(booking.bookingTime)} ·{" "}
                      {time(booking.bookingTime)}
                    </small>
                  </div>
                  <div className="ticket-card-side">
                    <span className={`badge ${booking.status.toLowerCase()}`}>
                      {statusText(booking.status)}
                    </span>
                    <strong>{money(booking.totalAmount)}</strong>
                    <Button
                      className="small"
                      onClick={() =>
                        setExpanded(
                          expanded === booking.bookingId
                            ? null
                            : booking.bookingId,
                        )
                      }
                    >
                      {expanded === booking.bookingId
                        ? "Thu gọn"
                        : "Chi tiết vé"}
                    </Button>
                  </div>
                </div>
                {expanded === booking.bookingId && (
                  <PaymentPanel
                    key={booking.bookingId}
                    api={api}
                    mode={mode}
                    booking={booking}
                    onUpdate={(latest) =>
                      setUpdates((current) => ({
                        ...current,
                        [latest.bookingId]: latest,
                      }))
                    }
                  />
                )}
              </article>
            );
          })}
      </div>
    </>
  );
}

export default function Tickets({ api, mode, user, onUser, movies, revision }) {
  return (
    <section className="container page-section">
      <span className="eyebrow">NHỮNG CUỘC HẸN TRÊN MÀN ẢNH LỚN</span>
      <h1>Vé của tôi</h1>
      <p>Tra cứu và theo dõi trạng thái vé theo hồ sơ khách hàng.</p>
      <CustomerForm api={api} user={user} onUser={onUser} />
      {user && (
        <TicketList
          key={`${mode}-${user.userId}`}
          api={api}
          mode={mode}
          user={user}
          movies={movies}
          revision={revision}
        />
      )}
    </section>
  );
}
