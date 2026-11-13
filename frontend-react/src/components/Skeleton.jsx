import React from "react";

const times = (count) => Array.from({ length: count }, (_, index) => index);

export function Skeleton({ className = "", style }) {
  return (
    <span className={`skeleton ${className}`} style={style} aria-hidden="true" />
  );
}

function Placeholder({ label, children }) {
  return (
    <div className="skeleton-block" role="status">
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}

export function MovieGridSkeleton({ count = 6 }) {
  return (
    <Placeholder label="Đang tải danh sách phim…">
      <div className="movie-grid" aria-hidden="true">
        {times(count).map((index) => (
          <div className="skeleton-card" key={index}>
            <Skeleton className="skeleton-poster" />
            <div className="skeleton-meta">
              <Skeleton className="skeleton-line" style={{ width: "46%" }} />
              <Skeleton className="skeleton-line" style={{ width: "22%" }} />
            </div>
            <Skeleton className="skeleton-line skeleton-title" />
            <Skeleton className="skeleton-line skeleton-footer" />
          </div>
        ))}
      </div>
    </Placeholder>
  );
}

export function CinemaGridSkeleton({ count = 3 }) {
  return (
    <Placeholder label="Đang tải danh sách rạp…">
      <div className="cinema-grid" aria-hidden="true">
        {times(count).map((index) => (
          <div className="skeleton-panel" key={index}>
            <Skeleton className="skeleton-cinema-art" />
            <div className="skeleton-panel-body">
              <Skeleton className="skeleton-line skeleton-title" />
              <Skeleton className="skeleton-line" />
              <Skeleton className="skeleton-line" style={{ width: "72%" }} />
              <Skeleton className="skeleton-button" />
            </div>
          </div>
        ))}
      </div>
    </Placeholder>
  );
}

export function TicketListSkeleton({ count = 3 }) {
  return (
    <Placeholder label="Đang tải vé của bạn…">
      <div className="tickets-list" aria-hidden="true">
        {times(count).map((index) => (
          <div className="skeleton-panel skeleton-ticket" key={index}>
            <Skeleton className="skeleton-ticket-cut" />
            <div className="skeleton-ticket-body">
              <Skeleton className="skeleton-line" style={{ width: "30%" }} />
              <Skeleton className="skeleton-line skeleton-title" />
              <Skeleton className="skeleton-line" style={{ width: "65%" }} />
            </div>
            <div className="skeleton-ticket-side">
              <Skeleton className="skeleton-pill" />
              <Skeleton className="skeleton-line" style={{ width: "70px" }} />
              <Skeleton className="skeleton-button" />
            </div>
          </div>
        ))}
      </div>
    </Placeholder>
  );
}

export function SeatMapSkeleton() {
  return (
    <Placeholder label="Đang tải sơ đồ ghế…">
      <div className="skeleton-seat-map" aria-hidden="true">
        <Skeleton className="skeleton-screen" />
        {times(7).map((row) => (
          <div className="skeleton-seat-row" key={row}>
            {times(10).map((seat) => (
              <Skeleton className="skeleton-seat" key={seat} />
            ))}
          </div>
        ))}
      </div>
    </Placeholder>
  );
}

export function ScheduleSkeleton({ groups = 2 }) {
  return (
    <Placeholder label="Đang tìm suất chiếu…">
      <div className="schedule-board" aria-hidden="true">
        <div className="skeleton-date-tabs">
          {times(5).map((index) => (
            <Skeleton className="skeleton-tab" key={index} />
          ))}
        </div>
        {times(groups).map((group) => (
          <section className="skeleton-panel skeleton-schedule" key={group}>
            <div className="skeleton-schedule-head">
              <Skeleton className="skeleton-avatar" />
              <div>
                <Skeleton className="skeleton-line skeleton-title" />
                <Skeleton className="skeleton-line" style={{ width: "45%" }} />
              </div>
            </div>
            <div className="showtime-list">
              {times(4).map((index) => (
                <Skeleton className="skeleton-showtime" key={index} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </Placeholder>
  );
}

export function TableSkeleton({ columns = 5, count = 7 }) {
  return (
    <Placeholder label="Đang tải dữ liệu quản trị…">
      <div className="admin-table-wrap" aria-hidden="true">
        <div className="skeleton-table">
          <div className="skeleton-table-row skeleton-table-head">
            {times(columns).map((column) => (
              <Skeleton className="skeleton-line" key={column} />
            ))}
          </div>
          {times(count).map((row) => (
            <div className="skeleton-table-row" key={row}>
              {times(columns).map((column) => (
                <Skeleton className="skeleton-line" key={column} />
              ))}
            </div>
          ))}
        </div>
      </div>
    </Placeholder>
  );
}
