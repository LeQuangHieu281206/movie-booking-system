import React, { useEffect, useMemo, useState } from "react";
import { createApi, createHttpClient } from "./services/api.js";
import { useLoad } from "./lib/useLoad.js";
import { Button, ErrorBox, Icon, Modal } from "./components/UI.jsx";
import CustomerForm from "./components/CustomerForm.jsx";
import { Catalog, Home, MovieDetail } from "./pages/Catalog.jsx";
import { Cinemas, Showtimes } from "./pages/Venues.jsx";
import Booking from "./pages/Booking.jsx";
import Tickets from "./pages/Tickets.jsx";
import Auth from "./pages/Auth.jsx";
import {
  createAuthService,
  createDemoAuthService,
} from "./services/auth.js";

const nav = [
  ["home", "Trang chủ"],
  ["movies", "Phim"],
  ["showtimes", "Lịch chiếu"],
  ["cinemas", "Rạp chiếu"],
  ["tickets", "Vé của tôi"],
  ["auth", "Tài khoản"],
];
const readRoute = () =>
  nav.some(([key]) => key === location.hash.slice(1))
    ? location.hash.slice(1)
    : "home";
function safeGet(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}
function save(key, value) {
  try {
    value === null
      ? localStorage.removeItem(key)
      : localStorage.setItem(key, value);
  } catch {
    /* App remains usable without persistence. */
  }
}

function Workspace({ mode, onMode }) {
  const api = useMemo(() => createApi(mode), [mode]);
  const auth = useMemo(
    () =>
      mode === "demo"
        ? createDemoAuthService()
        : createAuthService(createHttpClient()),
    [mode],
  );
  const [route, setRoute] = useState(readRoute),
    [movie, setMovie] = useState(null),
    [showtime, setShowtime] = useState(null),
    [profileOpen, setProfileOpen] = useState(false);
  const [user, setUser] = useState(null),
    [userError, setUserError] = useState(""),
    [revision, setRevision] = useState(0);
  const [schedule, setSchedule] = useState({
    movieId: "",
    cinemaId: "",
    key: 0,
  });
  const catalog = useLoad(
    async (options) => {
      const [movies, genres] = await Promise.all([
        api.movies(options),
        api.genres(options),
      ]);
      return { movies, genres };
    },
    [api],
  );
  const movies = catalog.data?.movies || [];
  useEffect(() => {
    const change = () => {
      setRoute(readRoute());
      window.scrollTo({ top: 0 });
    };
    window.addEventListener("hashchange", change);
    return () => window.removeEventListener("hashchange", change);
  }, []);
  useEffect(() => {
    const session = auth.restore();
    if (session?.user) {
      setUser(session.user);
      save(`cineroom-user:${mode}`, String(session.user.userId));
    }
  }, [auth, mode]);
  useEffect(() => {
    const storedId = safeGet(`cineroom-user:${mode}`);
    if (!storedId) return;
    let active = true;
    const controller = new AbortController();
    api
      .user(storedId, { signal: controller.signal })
      .then((user) => {
        if (active) setUser(user);
      })
      .catch((error) => {
        if (active && error.name !== "AbortError")
          setUserError(
            "Chưa tải lại được hồ sơ khách hàng. Bạn có thể mở Hồ sơ và nhập lại mã khách hàng.",
          );
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [api, mode]);
  function remember(customer) {
    setUser(customer);
    setUserError("");
    save(`cineroom-user:${mode}`, customer ? String(customer.userId) : null);
  }
  function navigate(next) {
    setRoute(next);
    location.hash = next;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function openSchedule(movieId = "", cinemaId = "") {
    setSchedule((value) => ({ movieId, cinemaId, key: value.key + 1 }));
    navigate("showtimes");
  }
  function selectShowtime(show) {
    setMovie(null);
    setShowtime(show);
  }
  function closeBooking() {
    setShowtime(null);
    setRevision((value) => value + 1);
  }
  return (
    <>
      <a
        className="skip-link"
        href="#main-content"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById("main-content")?.focus();
        }}
      >
        Bỏ qua điều hướng
      </a>
      <header className="site-header">
        <div className="header-inner">
          <a className="brand" href="#home" aria-label="CineRoom trang chủ">
            CINE<span>ROOM</span>
            <Icon name="ticket" size={23} />
          </a>
          <nav aria-label="Điều hướng chính">
            {nav.map(([key, label]) => (
              <a
                key={key}
                href={`#${key}`}
                aria-current={route === key ? "page" : undefined}
                className={route === key ? "active" : ""}
                onClick={() =>
                  key === "showtimes" &&
                  setSchedule((value) => ({
                    movieId: "",
                    cinemaId: "",
                    key: value.key + 1,
                  }))
                }
              >
                {label}
              </a>
            ))}
          </nav>
          <div className="header-actions">
            <button
              className={`mode-pill ${mode}`}
              onClick={() => onMode(mode === "demo" ? "live" : "demo")}
            >
              <span />
              {mode === "demo" ? "Dữ liệu mẫu" : "Backend thật"}
            </button>
            <button
              className="profile-button"
              aria-label={user ? `Hồ sơ ${user.fullName}` : "Hồ sơ khách hàng"}
              onClick={() => setProfileOpen(true)}
            >
              <Icon name="user" size={18} />
              <span>{user ? user.fullName.split(" ").at(-1) : "Hồ sơ"}</span>
            </button>
            {user ? (
              <button
                className="profile-button"
                aria-label="Đăng xuất"
                onClick={() => {
                  auth.logout();
                  setUser(null);
                  save(`cineroom-user:${mode}`, null);
                  navigate("home");
                }}
              >
                <Icon name="close" size={16} />
                <span>Đăng xuất</span>
              </button>
            ) : (
              <button
                className="profile-button"
                aria-label="Đăng nhập hoặc đăng ký"
                onClick={() => navigate("auth")}
              >
                <Icon name="user" size={16} />
                <span>Đăng nhập</span>
              </button>
            )}
          </div>
        </div>
      </header>
      <div className={`mode-banner ${mode}`}>
        <span>
          {mode === "demo"
            ? "Bạn đang trải nghiệm dữ liệu mẫu. Vé chỉ được lưu trong trình duyệt, không gửi đến backend."
            : "Đang sử dụng API backend. Thanh toán của dự án là mô phỏng, không thu tiền thật."}
        </span>
        <button onClick={() => onMode(mode === "demo" ? "live" : "demo")}>
          {mode === "demo" ? "Chuyển sang backend" : "Thử dữ liệu mẫu"}
          <Icon name="arrow" size={13} />
        </button>
      </div>
      {userError && (
        <div className="container">
          <ErrorBox message={userError} />
        </div>
      )}
      <main id="main-content" tabIndex={-1}>
        {route === "home" && (
          <Home
            catalog={catalog}
            onMovie={setMovie}
            onNavigate={navigate}
            onSchedule={openSchedule}
          />
        )}
        {route === "movies" && (
          <div className="container">
            <Catalog {...catalog} onMovie={setMovie} />
          </div>
        )}
        {route === "showtimes" && (
          <Showtimes
            key={schedule.key}
            api={api}
            movies={movies}
            initialMovieId={schedule.movieId}
            initialCinemaId={schedule.cinemaId}
            onSelect={selectShowtime}
          />
        )}
        {route === "cinemas" && <Cinemas api={api} onSchedule={openSchedule} />}
        {route === "auth" && (
          <Auth
            auth={auth}
            mode={mode}
            user={user}
            onUser={remember}
            onDone={() => navigate("home")}
          />
        )}
        {route === "tickets" && (
          <Tickets
            api={api}
            mode={mode}
            user={user}
            onUser={remember}
            movies={movies}
            revision={revision}
          />
        )}
      </main>
      <footer>
        <div className="container footer-inner">
          <div>
            <a className="brand" href="#home">
              CINE<span>ROOM</span>
              <Icon name="ticket" size={22} />
            </a>
            <p>Hẹn bạn ở rạp.</p>
          </div>
          <div className="footer-links">
            <a href="#movies">Khám phá phim</a>
            <a href="#cinemas">Hệ thống rạp</a>
            <a href="#tickets">Vé của tôi</a>
          </div>
          <div className="footer-note">
            <span>ĐỒ ÁN WEBSITE ĐẶT VÉ XEM PHIM</span>
            <p>React · Java Spring Boot · SOA</p>
          </div>
        </div>
        <div className="container footer-bottom">
          <span>© 2026 CineRoom · Dự án học tập</span>
          <span>Để mỗi bộ phim là một trải nghiệm.</span>
        </div>
      </footer>
      {movie && (
        <MovieDetail
          movie={movie}
          api={api}
          onClose={() => setMovie(null)}
          onSelect={selectShowtime}
        />
      )}
      {showtime && (
        <Booking
          key={`${mode}-${showtime.showtimeId}`}
          api={api}
          mode={mode}
          showtimeId={showtime.showtimeId}
          movie={movies.find((movie) => movie.movieId === showtime.movieId)}
          user={user}
          onUser={remember}
          onClose={closeBooking}
          onTickets={() => {
            closeBooking();
            navigate("tickets");
          }}
        />
      )}
      {profileOpen && (
        <Modal title="Hồ sơ khách hàng" onClose={() => setProfileOpen(false)}>
          <CustomerForm api={api} user={user} onUser={remember} />
          {user && (
            <Button
              className="full"
              onClick={() => {
                setProfileOpen(false);
                navigate("tickets");
              }}
            >
              Xem vé đã đặt
              <Icon name="arrow" size={16} />
            </Button>
          )}
        </Modal>
      )}
    </>
  );
}

export default function App() {
  const [mode, setMode] = useState(() =>
    new URLSearchParams(location.search).get("mode") === "demo"
      ? "demo"
      : safeGet("cineroom-react-mode") === "demo"
        ? "demo"
        : "live",
  );
  function changeMode(next) {
    save("cineroom-react-mode", next);
    const url = new URL(location.href);
    url.searchParams.delete("mode");
    history.replaceState(null, "", url);
    setMode(next);
  }
  return <Workspace key={mode} mode={mode} onMode={changeMode} />;
}
