import React, { useEffect, useRef, useId } from "react";

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
}) {
  const ref = useRef(null),
    previous = useRef(null),
    titleId = useId();
  useEffect(() => {
    if (!previous.current) previous.current = document.activeElement;
    ref.current?.showModal();
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = overflow;
      queueMicrotask(() => {
        if (!ref.current?.open) previous.current?.focus();
      });
    };
  }, []);
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
      className={`modal ${wide ? "wide" : ""}`}
      aria-labelledby={titleId}
      onKeyDown={trap}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
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
          onClick={onClose}
        >
          <Icon name="close" />
        </button>
      </div>
      {children}
    </dialog>
  );
}
