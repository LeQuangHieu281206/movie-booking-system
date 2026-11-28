import React, { useState } from "react";
import { hotMovies } from "../lib/domain.js";
import { Button, Empty, ErrorBox, Loading, Poster } from "../components/UI.jsx";

const MAX = 5;

export default function AdminHot({ api, user, catalog }) {
  // Giữ ở đây vì Picker bị gỡ ra trong lúc danh mục tải lại sau khi lưu.
  const [saved, setSaved] = useState(false);
  if (user?.role !== "ADMIN")
    return (
      <div className="container page-section">
        <Empty title="Chỉ dành cho quản trị viên">
          Hãy đăng nhập bằng tài khoản ADMIN để chọn phim hot.
        </Empty>
      </div>
    );
  return (
    <div className="container page-section">
      <h1>Chọn top {MAX} phim hot</h1>
      <p>Thứ tự ở đây là thứ tự hiển thị trên trang chủ.</p>
      {saved && <p role="status">Đã lưu danh sách phim hot.</p>}
      {catalog.loading ? (
        <Loading />
      ) : catalog.error ? (
        <ErrorBox message={catalog.error} onRetry={catalog.reload} />
      ) : (
        <Picker api={api} catalog={catalog} onSaved={setSaved} />
      )}
    </div>
  );
}

function Picker({ api, catalog, onSaved }) {
  const available = catalog.data.movies.filter((m) => m.status !== "ENDED");
  const [ids, setIds] = useState(() =>
    hotMovies(catalog.data.movies).map((m) => m.movieId),
  );
  const [saving, setSaving] = useState(false),
    [error, setError] = useState("");
  const selected = ids
    .map((id) => available.find((m) => m.movieId === id))
    .filter(Boolean);

  function change(next) {
    setIds(next);
    onSaved(false);
    setError("");
  }
  function move(index, step) {
    const next = [...ids];
    [next[index], next[index + step]] = [next[index + step], next[index]];
    change(next);
  }
  async function save() {
    setSaving(true);
    setError("");
    try {
      await api.setHotMovies(ids);
      onSaved(true);
      // Tải lại danh mục để trang chủ nhận thứ hạng mới.
      catalog.reload();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="hot-admin">
      <section>
        <h2>
          Đã chọn ({selected.length}/{MAX})
        </h2>
        {selected.length ? (
          <ol>
            {selected.map((movie, index) => (
              <li key={movie.movieId}>
                <span className="hot-rank">{index + 1}</span>
                <Poster movie={movie} />
                <span className="hot-title">{movie.title}</span>
                <Button
                  className="small"
                  aria-label={`Đưa ${movie.title} lên`}
                  disabled={index === 0}
                  onClick={() => move(index, -1)}
                >
                  ↑
                </Button>
                <Button
                  className="small"
                  aria-label={`Đưa ${movie.title} xuống`}
                  disabled={index === selected.length - 1}
                  onClick={() => move(index, 1)}
                >
                  ↓
                </Button>
                <Button
                  className="small"
                  onClick={() => change(ids.filter((id) => id !== movie.movieId))}
                >
                  Bỏ
                </Button>
              </li>
            ))}
          </ol>
        ) : (
          <p className="hint">
            Chưa chọn phim nào. Trang chủ sẽ hiện một phim đang chiếu.
          </p>
        )}
        {error && <ErrorBox message={error} />}
        <Button primary disabled={saving} onClick={save}>
          {saving ? "Đang lưu…" : "Lưu danh sách"}
        </Button>
      </section>
      <section>
        <h2>Phim có thể chọn</h2>
        <ul>
          {available
            .filter((movie) => !ids.includes(movie.movieId))
            .map((movie) => (
              <li key={movie.movieId}>
                <Poster movie={movie} />
                <span className="hot-title">
                  {movie.title}
                  <small>
                    {movie.status === "UPCOMING" ? "Sắp chiếu" : "Đang chiếu"}
                  </small>
                </span>
                <Button
                  className="small"
                  disabled={selected.length >= MAX}
                  onClick={() => change([...selected.map((m) => m.movieId), movie.movieId])}
                >
                  Thêm
                </Button>
              </li>
            ))}
        </ul>
      </section>
    </div>
  );
}
