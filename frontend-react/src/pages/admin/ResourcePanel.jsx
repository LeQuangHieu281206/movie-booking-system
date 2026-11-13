import React, { useMemo, useState } from "react";
import { useLoad } from "../../lib/useLoad.js";
import { columnText, filterRows, inDayRange } from "../../lib/admin.js";
import { cityList } from "../../lib/venues.js";
import { sameId } from "../../lib/domain.js";
import {
  Button,
  Empty,
  ErrorBox,
  Icon,
  Modal,
  SearchSelect,
} from "../../components/UI.jsx";
import { TableSkeleton } from "../../components/Skeleton.jsx";
import DateField from "./DateField.jsx";
import ResourceForm from "./ResourceForm.jsx";

function ConfirmDialog({ title, message, confirmLabel = "Xác nhận", onCancel, onConfirm }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function run() {
    setBusy(true);
    setError("");
    try {
      await onConfirm();
    } catch (cause) {
      setError(cause.message || "Không thực hiện được thao tác.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={title}
      onClose={onCancel}
      busy={busy}
      className="admin-dialog admin-confirm-dialog"
    >
      {error ? <ErrorBox message={error} /> : <p className="admin-confirm-text">{message}</p>}
      <div className="admin-form-actions">
        <Button onClick={onCancel} disabled={busy}>
          Để sau
        </Button>
        <Button primary onClick={run} disabled={busy}>
          {busy ? "Đang xử lý…" : confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}

export default function ResourcePanel({ api, resource, ctx, onRefsChanged }) {
  const [query, setQuery] = useState("");
  // Chọn theo cấp: phim → thành phố → rạp → phòng (mục nào cần thì hiện).
  const [state, setState] = useState({
    movieId: "",
    city: "",
    cinemaId: "",
    roomId: "",
    from: "",
    to: "",
  });
  const [form, setForm] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [notice, setNotice] = useState("");

  const cascading =
    resource.requiresRoom ||
    resource.requiresCinema ||
    resource.requiresCity ||
    resource.filtersCinema;
  const cities = cityList(ctx.cinemas, "city");
  const cityCinemas = state.city
    ? ctx.cinemas.filter((cinema) => cinema.city === state.city)
    : [];
  const cinemaRooms = state.cinemaId
    ? ctx.rooms.filter((room) => sameId(room.cinemaId, state.cinemaId))
    : [];
  // Danh sách chỉ hiện khi đã chọn đủ các cấp lọc của mục đang mở.
  const pending = [
    resource.requiresMovie && !state.movieId && "phim",
    cascading && !state.city && "thành phố",
    (resource.requiresRoom || resource.requiresCinema || resource.filtersCinema) &&
      !state.cinemaId &&
      "rạp chiếu",
    resource.requiresRoom && !state.roomId && "phòng chiếu",
    resource.filtersDate && (!state.from || !state.to) && "khoảng ngày",
  ].filter(Boolean);

  const listPath =
    typeof resource.listPath === "function"
      ? resource.listPath(state)
      : resource.listPath;
  const result = useLoad(
    (options) => (listPath ? api.admin.list(listPath, options) : Promise.resolve([])),
    [api, listPath],
  );
  const rows = useMemo(() => {
    let filtered = filterRows(result.data || [], query, resource, ctx);
    if (resource.filtersDate)
      filtered = filtered.filter((row) =>
        inDayRange(row.startTime, state.from, state.to),
      );
    // /getShowtimes chỉ lọc được theo phim, nên rạp/thành phố lọc tại đây.
    if (!resource.filtersCinema || (!state.city && !state.cinemaId))
      return filtered;
    const cinemaIds = new Set(
      state.cinemaId
        ? [String(state.cinemaId)]
        : ctx.cinemas
            .filter((cinema) => cinema.city === state.city)
            .map((cinema) => String(cinema.cinemaId)),
    );
    return filtered.filter((row) => cinemaIds.has(String(row.cinemaId)));
  }, [
    result.data,
    query,
    resource,
    ctx,
    state.cinemaId,
    state.city,
    state.from,
    state.to,
  ]);

  function changed(message) {
    setNotice(message);
    result.reload();
    onRefsChanged();
  }

  function remove(row) {
    setForm(null);
    setConfirm({
      title: `Xoá ${resource.label.toLowerCase()}?`,
      message: `Thao tác này không thể hoàn tác. Bản ghi #${row[resource.idKey]} sẽ bị xoá khỏi hệ thống.`,
      confirmLabel: "Xoá",
      run: async () => {
        await api.admin.remove(resource.removePath, row[resource.idKey]);
        setConfirm(null);
        changed("Đã xoá bản ghi.");
      },
    });
  }

  function quick(row, action) {
    setConfirm({
      title: `${action.label}?`,
      message: `Chuyển vé ${row.bookingCode} sang trạng thái ${action.status}.`,
      confirmLabel: action.label,
      run: async () => {
        await api.admin.update(resource.updatePath, row[resource.idKey], {
          status: action.status,
        });
        setConfirm(null);
        changed(`Đã cập nhật vé ${row.bookingCode}.`);
      },
    });
  }

  return (
    <section className="admin-panel fade-in" aria-labelledby="admin-title">
      <header className="admin-panel-head">
        <div>
          <h1 id="admin-title">{resource.title}</h1>
          <p>{resource.description}</p>
        </div>
        <div className="admin-panel-actions">
          <Button className="small" onClick={() => result.reload()} disabled={result.loading}>
            <Icon name="refresh" size={15} />
            Tải lại
          </Button>
          {resource.canCreate !== false && (
            <Button primary onClick={() => setForm({ row: null })}>
              <Icon name="plus" size={17} />
              Thêm mới
            </Button>
          )}
        </div>
      </header>

      <div className="admin-toolbar">
        {(resource.requiresMovie || cascading) && (
          <div className="admin-filters">
            {resource.requiresMovie && (
              <div className="admin-room-picker admin-movie-picker">
                Phim
                <SearchSelect
                  label="Tìm phim"
                  placeholder="Tìm phim…"
                  options={ctx.movies.map((movie) => ({
                    id: movie.movieId,
                    label: movie.title,
                  }))}
                  value={state.movieId}
                  onChange={(movieId) =>
                    setState((current) => ({
                      ...current,
                      movieId,
                      city: "",
                      cinemaId: "",
                      roomId: "",
                    }))
                  }
                />
              </div>
            )}
            {cascading && (
              <>
                <label className="admin-room-picker">
                  Thành phố
                  <select
                    value={state.city}
                    disabled={resource.requiresMovie && !state.movieId}
                    onChange={(event) =>
                      setState((current) => ({
                        ...current,
                        city: event.target.value,
                        cinemaId: "",
                        roomId: "",
                      }))
                    }
                  >
                    <option value="">— Chọn thành phố —</option>
                    {cities.map((city) => (
                      <option key={city}>{city}</option>
                    ))}
                  </select>
                </label>
                {(resource.requiresRoom ||
                  resource.requiresCinema ||
                  resource.filtersCinema) && (
                  <label className="admin-room-picker">
                    Rạp chiếu
                    <select
                      value={state.cinemaId}
                      disabled={!state.city}
                      onChange={(event) =>
                        setState((current) => ({
                          ...current,
                          cinemaId: event.target.value,
                          roomId: "",
                        }))
                      }
                    >
                      <option value="">— Chọn rạp —</option>
                      {cityCinemas.map((cinema) => (
                        <option key={cinema.cinemaId} value={cinema.cinemaId}>
                          {cinema.name}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
                {resource.requiresRoom && (
                  <label className="admin-room-picker">
                    Phòng chiếu
                    <select
                      value={state.roomId}
                      disabled={!state.cinemaId}
                      onChange={(event) =>
                        setState((current) => ({
                          ...current,
                          roomId: event.target.value,
                        }))
                      }
                    >
                      <option value="">— Chọn phòng —</option>
                      {cinemaRooms.map((room) => (
                        <option key={room.roomId} value={room.roomId}>
                          {room.name}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
              </>
            )}
          </div>
        )}
        <div className="admin-toolbar-search">
          {resource.filtersDate ? (
            <>
              <div className="admin-room-picker admin-date-picker">
                <span>Từ ngày</span>
                <DateField
                  label="Từ ngày"
                  value={state.from}
                  onChange={(from) =>
                    setState((current) => ({
                      ...current,
                      from,
                      // Giữ khoảng ngày luôn hợp lệ.
                      to: current.to && from && current.to < from ? from : current.to,
                    }))
                  }
                />
              </div>
              <div className="admin-room-picker admin-date-picker">
                <span>Đến ngày</span>
                <DateField
                  label="Đến ngày"
                  value={state.to}
                  onChange={(to) =>
                    setState((current) => ({
                      ...current,
                      to,
                      from:
                        current.from && to && current.from > to
                          ? to
                          : current.from,
                    }))
                  }
                />
              </div>
            </>
          ) : (
            <label className="admin-search">
              <Icon name="search" size={17} />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={`Tìm ${resource.label.toLowerCase()}…`}
                aria-label={`Tìm ${resource.label.toLowerCase()}`}
              />
            </label>
          )}
          {!result.loading && !result.error && !pending.length && (
            <span className="admin-count">
              {rows.length}/{result.data?.length || 0} bản ghi
            </span>
          )}
        </div>
      </div>

      {notice && (
        <div className="success-note" role="status">
          <Icon name="check" size={18} />
          {notice}
        </div>
      )}

      {pending.length ? (
        <Empty title={`Chọn ${pending[0]}`}>
          {pending.length > 1
            ? `Chọn ${pending.join(" → ")} để xem danh sách.`
            : `Chọn ${pending[0]} để xem ${resource.label.toLowerCase()}.`}
        </Empty>
      ) : result.loading ? (
        <TableSkeleton columns={resource.columns.length + 1} />
      ) : result.error ? (
        <ErrorBox message={result.error} onRetry={result.reload} />
      ) : rows.length ? (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                {resource.columns.map((column) => (
                  <th key={column.label} scope="col">
                    {column.label}
                  </th>
                ))}
                <th scope="col">
                  <span className="sr-only">Thao tác</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row[resource.idKey]}>
                  {resource.columns.map((column) => (
                    <td
                      key={column.label}
                      className={column.nowrap ? "is-nowrap" : undefined}
                    >
                      {column.badge ? (
                        <span
                          className={`admin-badge status-${String(
                            column.tone ? column.tone(row) : "",
                          ).toLowerCase()}`}
                        >
                          {columnText(row, column, ctx)}
                        </span>
                      ) : (
                        columnText(row, column, ctx) || "—"
                      )}
                    </td>
                  ))}
                  <td className="admin-row-actions">
                    {resource.quickActions
                      ?.filter((action) => action.when(row))
                      .map((action) => (
                        <Button
                          className="small"
                          key={action.label}
                          onClick={() => quick(row, action)}
                        >
                          {action.label}
                        </Button>
                      ))}
                    {resource.canEdit !== false && (
                      <button
                        type="button"
                        className="admin-icon-button"
                        aria-label={`Sửa ${resource.label.toLowerCase()}`}
                        title="Sửa"
                        onClick={() => setForm({ row })}
                      >
                        <Icon name="edit" size={16} />
                      </button>
                    )}
                    <button
                      type="button"
                      className="admin-icon-button danger"
                      aria-label={`Xoá ${resource.label.toLowerCase()}`}
                      title="Xoá"
                      onClick={() => remove(row)}
                    >
                      <Icon name="trash" size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <Empty
          title={
            query || state.from || state.to
              ? "Không tìm thấy bản ghi"
              : `Chưa có ${resource.label.toLowerCase()}`
          }
        >
          {query
            ? "Thử từ khoá khác hoặc xoá bộ lọc tìm kiếm."
            : state.from || state.to
              ? "Thử mở rộng khoảng ngày hoặc chọn phim/rạp khác."
              : "Bấm “Thêm mới” để tạo bản ghi đầu tiên."}
        </Empty>
      )}

      {form && (
        <ResourceForm
          key={form.row?.[resource.idKey] || "new"}
          api={api}
          resource={resource}
          row={form.row}
          ctx={ctx}
          state={state}
          onClose={() => setForm(null)}
          onSaved={(message) => {
            setForm(null);
            changed(message);
          }}
        />
      )}
      {confirm && (
        <ConfirmDialog
          title={confirm.title}
          message={confirm.message}
          confirmLabel={confirm.confirmLabel}
          onCancel={() => setConfirm(null)}
          onConfirm={confirm.run}
        />
      )}
    </section>
  );
}

