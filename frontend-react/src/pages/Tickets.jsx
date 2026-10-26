import React, { useEffect, useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import { date, money, statusText, time } from "../lib/domain.js";
import { Button, Empty, ErrorBox, Icon, Loading } from "../components/UI.jsx";
import PaymentPanel from "../components/PaymentPanel.jsx";

function TicketList({ api, user, movies, revision }) {
  const result = useLoad(
    (options) => api.bookings(user.userId, options),
    [api, user.userId, revision],
  );
  // Chỉ tải suất chiếu của chính các vé này: /getShowtimes không lọc trả về
  // toàn bộ lịch của rạp (dữ liệu thật rất lớn).
  const [shows, setShows] = useState({});
  const [showsError, setShowsError] = useState("");
  useEffect(() => {
    const ids = [
      ...new Set((result.data || []).map((booking) => booking.showtimeId)),
    ].filter((id) => id !== null && id !== undefined);
    if (!ids.length) return;
    let active = true;
    const controller = new AbortController();
    Promise.allSettled(
      ids.map((id) =>
        api
          .showtime(id, { signal: controller.signal })
          .then((show) => [id, show]),
      ),
    ).then((results) => {
      if (!active) return;
      const map = {};
      let failed = false;
      for (const item of results) {
        if (item.status === "fulfilled") map[item.value[0]] = item.value[1];
        else if (item.reason?.name !== "AbortError") failed = true;
      }
      setShows(map);
      setShowsError(
        failed ? "Một số suất chiếu của vé không tải được." : "",
      );
    });
    return () => {
      active = false;
      controller.abort();
    };
  }, [api, result.data]);
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
      {showsError && <p className="hint">{showsError}</p>}
      <div className="tickets-list">
        {[...result.data]
          .sort((a, b) => new Date(b.bookingTime) - new Date(a.bookingTime))
          .map((original) => {
            const booking = updates[original.bookingId] || original;
            const show = shows[booking.showtimeId],
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

export default function Tickets({ api, user, onLogin, movies, revision }) {
  return (
    <section className="container page-section">
      <span className="eyebrow">NHỮNG CUỘC HẸN TRÊN MÀN ẢNH LỚN</span>
      <h1>Vé của tôi</h1>
      <p>Tra cứu và theo dõi trạng thái vé của tài khoản đang đăng nhập.</p>
      {user ? (
        <TicketList
          key={user.userId}
          api={api}
          user={user}
          movies={movies}
          revision={revision}
        />
      ) : (
        <>
          <Empty title="Đăng nhập để xem vé">
            Vé được gắn với tài khoản đã đặt, không tra cứu được bằng mã khách
            hàng.
          </Empty>
          <div className="booking-actions">
            <Button primary onClick={onLogin}>
              Đăng nhập / Đăng ký
              <Icon name="arrow" size={16} />
            </Button>
          </div>
        </>
      )}
    </section>
  );
}
