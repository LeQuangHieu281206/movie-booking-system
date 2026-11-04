import React, { useEffect, useMemo, useState } from "react";
import { useLoad } from "../lib/useLoad.js";
import {
  ADMIN_RESOURCES,
  byId,
  columnText,
  filterRows,
} from "../lib/admin.js";
import { money } from "../lib/domain.js";
import { Button, Empty, ErrorBox, Icon, Loading, Modal } from "../components/UI.jsx";

function fieldOptions(field, ctx) {
  if (field.options) return field.options;
  return (ctx[field.optionsKey] || []).map((item) => ({
    value: item[field.optionValue],
    label:
      field.optionsKey === "rooms"
        ? `${item.name} — ${byId(ctx.cinemas, "cinemaId", "name", item.cinemaId)}`
        : item[field.optionLabel],
  }));
}

function AccessCard({ title, children, action, onAction }) {
  return (
    <div className="container page-section">
      <div className="admin-access">
        <span className="admin-access-icon">
          <Icon name="dashboard" size={28} />
        </span>
        <span className="eyebrow">CINEROOM / QUẢN TRỊ</span>
        <h1>{title}</h1>
        <p>{children}</p>
        <Button primary onClick={onAction}>
          {action}
          <Icon name="arrow" size={16} />
        </Button>
      </div>
    </div>
  );
}

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

function ResourceForm({ api, resource, row, ctx, state, onClose, onSaved }) {
  const editing = Boolean(row);
  const [values, setValues] = useState(() =>
    resource.toForm(row || null, ctx, state),
  );
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function update(name, value) {
    setValues((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const payload = resource.toPayload(values, editing);
      if (editing)
        await api.admin.update(resource.updatePath, row[resource.idKey], payload);
      else await api.admin.create(resource.createPath, payload);
      onSaved(editing ? "Đã cập nhật thành công." : "Đã thêm mới thành công.");
    } catch (cause) {
      setError(cause.message || "Không lưu được dữ liệu.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      title={`${editing ? "Sửa" : "Thêm"} ${resource.label.toLowerCase()}`}
      onClose={onClose}
      busy={busy}
      wide
      className="admin-dialog"
    >
      <form className="admin-form" onSubmit={submit}>
        {error && <ErrorBox message={error} />}
        <div className="admin-form-grid">
          {resource.fields.map((field) => {
            const wide = field.wide || field.type === "textarea" || field.type === "genres";
            const readOnly = editing && field.readOnlyOnEdit;
            const required = field.required && !(editing && field.optionalOnEdit);
            const label = (
              <>
                {field.label}
                {required && <span aria-hidden="true"> *</span>}
              </>
            );
            if (field.type === "genres")
              return (
                <fieldset className="admin-field admin-field-wide" key={field.name}>
                  <legend>{label}</legend>
                  <div className="admin-check-list">
                    {fieldOptions(field, ctx).map((option) => {
                      const checked = (values[field.name] || [])
                        .map(String)
                        .includes(String(option.value));
                      return (
                        <label className="admin-check" key={option.value}>
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() =>
                              update(
                                field.name,
                                checked
                                  ? values[field.name].filter(
                                      (value) => String(value) !== String(option.value),
                                    )
                                  : [...(values[field.name] || []), option.value],
                              )
                            }
                          />
                          <span>{option.label}</span>
                        </label>
                      );
                    })}
                  </div>
                </fieldset>
              );
            if (field.type === "select")
              return (
                <label className={`admin-field ${wide ? "admin-field-wide" : ""}`} key={field.name}>
                  {label}
                  <select
                    value={values[field.name] ?? ""}
                    required={required}
                    onChange={(event) => update(field.name, event.target.value)}
                  >
                    <option value="">— Chọn —</option>
                    {fieldOptions(field, ctx).map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  {field.help && <small>{field.help}</small>}
                </label>
              );
            if (field.type === "textarea")
              return (
                <label className="admin-field admin-field-wide" key={field.name}>
                  {label}
                  <textarea
                    value={values[field.name] ?? ""}
                    required={required}
                    maxLength={field.maxLength}
                    rows={4}
                    onChange={(event) => update(field.name, event.target.value)}
                  />
                  {field.help && <small>{field.help}</small>}
                </label>
              );
            return (
              <label className={`admin-field ${wide ? "admin-field-wide" : ""}`} key={field.name}>
                {label}
                <input
                  type={field.type || "text"}
                  value={values[field.name] ?? ""}
                  required={required}
                  disabled={readOnly}
                  readOnly={readOnly}
                  min={field.min}
                  max={field.max}
                  minLength={field.minLength}
                  maxLength={field.maxLength}
                  step={field.step}
                  pattern={field.pattern}
                  title={field.title}
                  inputMode={field.inputMode}
                  placeholder={
                    editing && field.optionalOnEdit ? "Để trống nếu không đổi" : undefined
                  }
                  onChange={(event) => update(field.name, event.target.value)}
                />
                {field.help && <small>{field.help}</small>}
              </label>
            );
          })}
        </div>
        <div className="admin-form-actions">
          <Button type="button" onClick={onClose} disabled={busy}>
            Huỷ
          </Button>
          <Button primary type="submit" disabled={busy}>
            {busy ? "Đang lưu…" : editing ? "Lưu thay đổi" : "Thêm mới"}
            <Icon name="check" size={16} />
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function ResourcePanel({ api, resource, ctx, onRefsChanged }) {
  const [query, setQuery] = useState("");
  const [state, setState] = useState({ roomId: "" });
  const [form, setForm] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!resource.requiresRoom || state.roomId) return;
    const first = ctx.rooms[0]?.roomId;
    if (first) setState({ roomId: first });
  }, [resource.requiresRoom, state.roomId, ctx.rooms]);

  const listPath =
    typeof resource.listPath === "function"
      ? resource.listPath(state)
      : resource.listPath;
  const result = useLoad(
    (options) => (listPath ? api.admin.list(listPath, options) : Promise.resolve([])),
    [api, listPath],
  );
  const rows = useMemo(
    () => filterRows(result.data || [], query, resource, ctx),
    [result.data, query, resource, ctx],
  );

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
    <section className="admin-panel" aria-labelledby="admin-title">
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
        {resource.requiresRoom && (
          <label className="admin-room-picker">
            Phòng chiếu
            <select
              value={state.roomId}
              onChange={(event) => setState({ roomId: event.target.value })}
            >
              <option value="">— Chọn phòng —</option>
              {ctx.rooms.map((room) => (
                <option key={room.roomId} value={room.roomId}>
                  {room.name} — {byId(ctx.cinemas, "cinemaId", "name", room.cinemaId)}
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="admin-search">
          <Icon name="search" size={17} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={`Tìm ${resource.label.toLowerCase()}…`}
            aria-label={`Tìm ${resource.label.toLowerCase()}`}
          />
        </label>
        {!result.loading && !result.error && (
          <span className="admin-count">
            {rows.length}/{result.data?.length || 0} bản ghi
          </span>
        )}
      </div>

      {notice && (
        <div className="success-note" role="status">
          <Icon name="check" size={18} />
          {notice}
        </div>
      )}

      {resource.requiresRoom && !state.roomId ? (
        <Empty title="Chọn phòng chiếu">
          Danh sách ghế được quản lý theo từng phòng. Chọn một phòng để tiếp tục.
        </Empty>
      ) : result.loading ? (
        <Loading label="Đang tải dữ liệu quản trị…" />
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
        <Empty title={query ? "Không tìm thấy bản ghi" : `Chưa có ${resource.label.toLowerCase()}`}>
          {query
            ? "Thử từ khoá khác hoặc xoá bộ lọc tìm kiếm."
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

const lastSevenDays = () => {
  const format = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  const now = Date.now();
  return Array.from({ length: 7 }, (_, index) => ({
    date: format.format(new Date(now - (6 - index) * 86400000)),
    revenue: 0,
    paidBookings: 0,
  }));
};

const compactMoney = (value) =>
  new Intl.NumberFormat("vi-VN", {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(Number(value) || 0);
const dayLabel = (value) => `${value.slice(8, 10)}/${value.slice(5, 7)}`;

function RevenueChart({ data = [] }) {
  const max = Math.max(...data.map((day) => Number(day.revenue) || 0), 1);
  return (
    <section className="admin-chart" aria-labelledby="admin-chart-title">
      <header className="admin-chart-head">
        <h2 id="admin-chart-title">Doanh thu 7 ngày gần nhất</h2>
        <p>Chỉ tính các đơn đã thanh toán, theo giờ Việt Nam.</p>
      </header>
      <div
        className="admin-chart-bars"
        role="img"
        aria-label="Biểu đồ doanh thu 7 ngày gần nhất"
      >
        {data.map((day) => {
          const revenue = Number(day.revenue) || 0;
          return (
            <div
              className="admin-chart-bar"
              key={day.date}
              title={`${dayLabel(day.date)}: ${money(revenue)} · ${day.paidBookings} đơn`}
            >
              <span className="admin-chart-value">
                {revenue ? compactMoney(revenue) : "0"}
              </span>
              <div className="admin-chart-track">
                <div
                  className="admin-chart-fill"
                  style={{ height: `${Math.max(3, (revenue / max) * 100)}%` }}
                />
              </div>
              <span className="admin-chart-label">{dayLabel(day.date)}</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function Overview({ api }) {
  const stats = useLoad(
    (options) => api.admin.get("/getBookingStats", options),
    [api],
  );
  const cards = [
    {
      label: "Doanh số",
      value: Number(stats.data?.paidBookings || 0).toLocaleString("vi-VN"),
      note: "đơn đã thanh toán",
      icon: "chart",
    },
    {
      label: "Số vé bán được",
      value: Number(stats.data?.ticketsSold || 0).toLocaleString("vi-VN"),
      note: "vé đã thanh toán",
      icon: "ticket",
    },
    {
      label: "Doanh thu",
      value: money(stats.data?.revenue),
      note: "tổng tiền đã thu",
      icon: "dashboard",
    },
  ];

  return (
    <section className="admin-panel" aria-labelledby="admin-title">
      <header className="admin-panel-head">
        <div>
          <h1 id="admin-title">Tổng quan</h1>
          <p>Doanh số, vé bán và doanh thu từ các đơn đã thanh toán.</p>
        </div>
        <Button
          className="small"
          onClick={() => stats.reload()}
          disabled={stats.loading}
        >
          <Icon name="refresh" size={15} />
          Tải lại
        </Button>
      </header>
      {stats.loading && !stats.data ? (
        <Loading label="Đang tải thống kê…" />
      ) : stats.error ? (
        <ErrorBox message={stats.error} onRetry={stats.reload} />
      ) : (
        <>
          <div className="admin-stats">
            {cards.map((card) => (
              <article className="admin-stat-card" key={card.label}>
                <span className="admin-stat-icon">
                  <Icon name={card.icon} size={18} />
                </span>
                <span className="admin-stat-label">{card.label}</span>
                <strong className="admin-stat-value">{card.value}</strong>
                <small>{card.note}</small>
              </article>
            ))}
          </div>
          <RevenueChart
            data={
              stats.data?.dailyRevenue?.length
                ? stats.data.dailyRevenue
                : lastSevenDays()
            }
          />
        </>
      )}
    </section>
  );
}

function AdminConsole({ api, user }) {
  const [activeId, setActiveId] = useState("overview");
  const [lastCtx, setLastCtx] = useState(null);
  const refs = useLoad(
    async (options) => {
      const [movies, genres, cinemas, rooms] = await Promise.all([
        api.admin.list("/getMovies", options),
        api.admin.list("/getGenres", options),
        api.admin.list("/getCinemas", options),
        api.admin.list("/getRooms", options),
      ]);
      return { movies, genres, cinemas, rooms };
    },
    [api],
  );
  useEffect(() => {
    if (refs.data) setLastCtx(refs.data);
  }, [refs.data]);
  const ctx = useMemo(
    () =>
      refs.data ||
      lastCtx || { movies: [], genres: [], cinemas: [], rooms: [] },
    [refs.data, lastCtx],
  );
  const active =
    ADMIN_RESOURCES.find((resource) => resource.id === activeId) ||
    ADMIN_RESOURCES[0];

  return (
    <div className="container page-section admin-page">
      <span className="eyebrow">CINEROOM / QUẢN TRỊ HỆ THỐNG</span>
      <div className="admin-shell">
        <aside className="admin-sidebar" aria-label="Khu vực quản trị">
          <div className="admin-sidebar-head">
            <span className="admin-sidebar-avatar" aria-hidden="true">
              {user.fullName?.charAt(0) || "A"}
            </span>
            <div>
              <strong>{user.fullName}</strong>
              <small>{user.email}</small>
            </div>
          </div>
          <nav className="admin-nav" aria-label="Danh mục quản trị">
            <span className="admin-nav-group">Tổng quan</span>
            <button
              type="button"
              className={activeId === "overview" ? "active" : undefined}
              aria-current={activeId === "overview" ? "page" : undefined}
              onClick={() => setActiveId("overview")}
            >
              <Icon name="dashboard" size={17} />
              Tổng quan
            </button>
            {ADMIN_RESOURCES.map((resource, index) => {
              const firstOfGroup =
                index === 0 ||
                ADMIN_RESOURCES[index - 1].group !== resource.group;
              return (
                <React.Fragment key={resource.id}>
                  {firstOfGroup && (
                    <span className="admin-nav-group">{resource.group}</span>
                  )}
                  <button
                    type="button"
                    className={
                      activeId === resource.id ? "active" : undefined
                    }
                    aria-current={activeId === resource.id ? "page" : undefined}
                    onClick={() => setActiveId(resource.id)}
                  >
                    <Icon name={resource.icon} size={17} />
                    {resource.label}
                  </button>
                </React.Fragment>
              );
            })}
          </nav>
        </aside>

        <div className="admin-content">
          {activeId === "overview" ? (
            <Overview api={api} />
          ) : refs.loading && !lastCtx ? (
            <Loading label="Đang tải dữ liệu quản trị…" />
          ) : refs.error && !lastCtx ? (
            <ErrorBox message={refs.error} onRetry={refs.reload} />
          ) : (
            <ResourcePanel
              key={active.id}
              api={api}
              resource={active}
              ctx={ctx}
              onRefsChanged={refs.reload}
            />
          )}
        </div>
      </div>
    </div>
  );
}

export default function Admin({ api, user, onLogin, onHome }) {
  if (!user)
    return (
      <AccessCard
        title="Đăng nhập quản trị"
        action="Đi tới đăng nhập"
        onAction={onLogin}
      >
        Dùng tài khoản quản trị viên đã được cấp quyền. Sau khi đăng nhập, mục
        Quản trị sẽ xuất hiện trên thanh điều hướng.
      </AccessCard>
    );
  if (user.role !== "ADMIN")
    return (
      <AccessCard
        title="Không có quyền truy cập"
        action="Về trang chủ"
        onAction={onHome}
      >
        Tài khoản {user.email} không phải quản trị viên. Liên hệ người phụ
        trách hệ thống nếu bạn cần quyền này.
      </AccessCard>
    );
  return <AdminConsole api={api} user={user} />;
}
