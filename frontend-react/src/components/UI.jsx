import React, { useEffect, useRef, useId, useState } from "react";

export function Icon({ name, size = 20, ...props }) {
  const paths = {
    ticket: (
      <>
        <path d="M3 7h18v4a2 2 0 0 0 0 4v3H3v-3a2 2 0 0 0 0-4V7Z" />
        <path d="M16 7v2m0 3v2m0 2v2" />
      </>
    ),
    arrow: (
      <>
        <path d="M4 12h16m-6-6 6 6-6 6" />
      </>
    ),
    search: (
      <>
        <circle cx="10.5" cy="10.5" r="6.5" />
        <path d="m16 16 4 4" />
      </>
    ),
    pin: (
      <>
        <path d="M20 10c0 6-8 11-8 11S4 16 4 10a8 8 0 1 1 16 0Z" />
        <circle cx="12" cy="10" r="2.5" />
      </>
    ),
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 6v6l4 2" />
      </>
    ),
    play: <path d="m9 5 11 7-11 7V5Z" />,
    sun: (
      <>
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32 1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
      </>
    ),
    moon: <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" />,
    user: (
      <>
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21v-2a8 8 0 0 1 16 0v2" />
      </>
    ),
    close: <path d="m6 6 12 12M6 18 18 6" />,
    check: <path d="m5 12 4 4L19 6" />,
    refresh: (
      <>
        <path d="M20 7v5h-5M4 17v-5h5" />
        <path d="M6 7a7 7 0 0 1 12-1l2 6M4 12l2 6a7 7 0 0 0 12-1" />
      </>
    ),
    seat: (
      <>
        <path d="M6 13V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8M4 11v7h16v-7M6 21v-3m12 3v-3M4 14h16" />
      </>
    ),
    dashboard: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="3" width="7" height="7" rx="1.5" />
        <rect x="3" y="14" width="7" height="7" rx="1.5" />
        <rect x="14" y="14" width="7" height="7" rx="1.5" />
      </>
    ),
    chart: (
      <>
        <path d="M4 20V4M4 20h16" />
        <path d="m7 15 4-4 3 3 5-6" />
      </>
    ),
    plus: <path d="M12 5v14M5 12h14" />,
    edit: (
      <>
        <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Z" />
        <path d="m14.5 5.5 3 3" />
      </>
    ),
    trash: (
      <>
        <path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14" />
        <path d="M10 11v6m4-6v6" />
      </>
    ),
    film: (
      <>
        <rect x="3" y="4" width="18" height="16" rx="2" />
        <path d="M7 4v16M17 4v16M3 9h4m10 0h4M3 15h4m10 0h4" />
      </>
    ),
    tag: (
      <>
        <path d="M20 13 11 4H4v7l9 9a2 2 0 0 0 3 0l4-4a2 2 0 0 0 0-3Z" />
        <circle cx="7.5" cy="7.5" r="1" />
      </>
    ),
    calendar: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="2" />
        <path d="M8 3v4m8-4v4M3 11h18" />
      </>
    ),
    building: (
      <>
        <path d="M4 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16M16 9h3a1 1 0 0 1 1 1v11M8 7h4M8 11h4M8 15h4M2 21h20" />
      </>
    ),
    room: (
      <>
        <rect x="3" y="4" width="18" height="13" rx="2" />
        <path d="M7 21h10M12 17v4" />
      </>
    ),
  };
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {paths[name] || paths.ticket}
    </svg>
  );
}
export function Button({
  children,
  primary = false,
  className = "",
  ...props
}) {
  return (
    <button
      className={`button ${primary ? "primary" : ""} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
export function Empty({ title = "Chưa có dữ liệu", children }) {
  return (
    <div className="empty">
      <Icon name="ticket" size={36} />
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}
export function ErrorBox({ message, onRetry }) {
  return (
    <div className="error-box" role="alert">
      <p>{message}</p>
      {onRetry && (
        <Button onClick={onRetry} className="small">
          <Icon name="refresh" size={16} />
          Thử lại
        </Button>
      )}
    </div>
  );
}
export function Loading({ label = "Đang tải dữ liệu…" }) {
  return (
    <div className="loading" role="status">
      <span className="spinner" />
      {label}
    </div>
  );
}
export function Poster({ movie, className = "" }) {
  return (
    <div className={`poster ${className}`}>
      <div className="poster-fallback">
        <Icon name="play" size={42} />
        <span>{movie.title}</span>
      </div>
      {movie.posterUrl && /^https?:\/\/|^\/assets\//.test(movie.posterUrl) && (
        <img
          src={movie.posterUrl}
          alt={`Áp phích ${movie.title}`}
          loading="lazy"
          onError={(event) => {
            event.currentTarget.style.display = "none";
          }}
        />
      )}
      <span className="age">{movie.ageRestriction || "P"}</span>
    </div>
  );
}
export function Modal({
  title,
  children,
  onClose,
  wide = false,
  busy = false,
  className = "",
}) {
  const ref = useRef(null),
    previous = useRef(null),
    titleId = useId(),
    timer = useRef(null);
  const [closing, setClosing] = useState(false);
  useEffect(() => {
    if (!previous.current) previous.current = document.activeElement;
    if (ref.current && !ref.current.open) ref.current.showModal();
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      clearTimeout(timer.current);
      document.body.style.overflow = overflow;
      queueMicrotask(() => {
        if (!ref.current?.open) previous.current?.focus();
      });
    };
  }, []);
  function requestClose() {
    if (busy || closing) return;
    setClosing(true);
    const delay = window.matchMedia?.("(prefers-reduced-motion: reduce)")
      .matches
      ? 0
      : 150;
    timer.current = setTimeout(() => {
      ref.current?.close();
      onClose();
    }, delay);
  }
  function trap(event) {
    if (event.key !== "Tab") return;
    const focusable = [
      ...ref.current.querySelectorAll(
        'button:not(:disabled), a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex="0"]',
      ),
    ];
    const first = focusable[0],
      last = focusable.at(-1);
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    }
    if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  }
  return (
    <dialog
      ref={ref}
      className={`modal ${wide ? "wide" : ""} ${closing ? "is-closing" : ""} ${className}`}
      aria-labelledby={titleId}
      onKeyDown={trap}
      onClick={(event) => {
        if (event.target !== ref.current) return;
        const rect = ref.current.getBoundingClientRect();
        const outside =
          event.clientX < rect.left ||
          event.clientX > rect.right ||
          event.clientY < rect.top ||
          event.clientY > rect.bottom;
        if (outside) requestClose();
      }}
      onCancel={(event) => {
        event.preventDefault();
        requestClose();
      }}
    >
      <div className="modal-heading">
        <div>
          <span className="eyebrow">CINEROOM / TRẢI NGHIỆM ĐIỆN ẢNH</span>
          <h2 id={titleId}>{title}</h2>
        </div>
        <button
          disabled={busy}
          className="icon-button"
          aria-label="Đóng"
          onClick={requestClose}
        >
          <Icon name="close" />
        </button>
      </div>
      {children}
    </dialog>
  );
}
