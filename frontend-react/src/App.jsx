import React, { useEffect, useMemo, useState } from "react";
import { createApi, createHttpClient } from "./services/api.js";
import { createAuthService } from "./services/auth.js";
import { useLoad } from "./lib/useLoad.js";
import { ErrorBox, Icon } from "./components/UI.jsx";
import { Catalog, Home, MovieDetail } from "./pages/Catalog.jsx";
import { Cinemas, Showtimes } from "./pages/Venues.jsx";
import Booking from "./pages/Booking.jsx";
import Tickets from "./pages/Tickets.jsx";
import Auth from "./pages/Auth.jsx";

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

function Workspace() {
  const api = useMemo(() => createApi(), []);
  const auth = useMemo(() => createAuthService(createHttpClient()), []);
  const [route, setRoute] = useState(readRoute),
    [movie, setMovie] = useState(null),
    [showtime, setShowtime] = useState(null);
  const [user, setUser] = useState(() => auth.restore()?.user || null),
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
  // Xác thực lại phiên đã lưu bằng JWT; token hết hạn được tự đổi bằng refresh token.
  useEffect(() => {
    if (!auth.restore()) return;
    let active = true;
    const controller = new AbortController();
    api
      .me({ signal: controller.signal })
      .then((profile) => active && setUser(profile))
      .catch((error) => {
        if (!active || error.name === "AbortError") return;
        if (error.status === 401) {
          auth.logout();
          setUser(null);
          setUserError("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        } else setUserError(`Chưa tải lại được hồ sơ: ${error.message}`);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [api, auth]);
  function setProfile(customer) {
    setUser(customer);
    setUserError("");
  }
  function logout() {
    auth.logout();
    setUser(null);
    setUserError("");
    navigate("home");
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
  function goLogin() {
    setShowtime(null);
    navigate("auth");
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
            {user ? (
              <>
                <button
                  className="profile-button"
                  aria-label={`Vé của ${user.fullName}`}
                  onClick={() => navigate("tickets")}
                >
                  <Icon name="user" size={18} />
                  <span>{user.fullName.split(" ").at(-1)}</span>
                </button>
                <button
                  className="profile-button"
                  aria-label="Đăng xuất"
                  onClick={logout}
                >
                  <Icon name="close" size={16} />
                  <span>Đăng xuất</span>
                </button>
              </>
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
            user={user}
            onUser={setProfile}
            onDone={() => navigate("home")}
          />
        )}
        {route === "tickets" && (
          <Tickets
            api={api}
            user={user}
            onLogin={goLogin}
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
          key={showtime.showtimeId}
          api={api}
          showtimeId={showtime.showtimeId}
          movie={movies.find((movie) => movie.movieId === showtime.movieId)}
          user={user}
          onLogin={goLogin}
          onClose={closeBooking}
          onTickets={() => {
            closeBooking();
            navigate("tickets");
          }}
        />
      )}
    </>
  );
}

export default function App() {
  return <Workspace />;
}
