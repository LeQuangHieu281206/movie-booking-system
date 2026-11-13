import React, {
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useLoad } from "../lib/useLoad.js";
import { ADMIN_RESOURCES } from "../lib/admin.js";
import { isAdmin } from "../lib/domain.js";
import { Button, ErrorBox, Icon } from "../components/UI.jsx";
import { TableSkeleton } from "../components/Skeleton.jsx";
import ResourcePanel from "./admin/ResourcePanel.jsx";
import {
  SalesDashboard,
  SalesDashboardSkeleton,
} from "../components/dashboard/SalesDashboard.jsx";

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

function Overview({ api }) {
  const [period, setPeriod] = useState("day");
  // Giữ số liệu lần trước trong lúc tải để biểu đồ không nhấp nháy khi đổi kỳ.
  const [last, setLast] = useState(null);
  const stats = useLoad(
    (options) => api.admin.get(`/getBookingStats?period=${period}`, options),
    [api, period],
  );
  useEffect(() => {
    if (stats.data) setLast(stats.data);
  }, [stats.data]);
  const shown = stats.data || last;
  if (!shown && stats.loading) return <SalesDashboardSkeleton period={period} />;
  if (stats.error) return <ErrorBox message={stats.error} onRetry={stats.reload} />;
  return (
    <SalesDashboard
      data={shown}
      period={period}
      onPeriodChange={setPeriod}
      loading={stats.loading}
      onReload={stats.reload}
    />
  );
}

function AdminConsole({ api, user }) {
  const [activeId, setActiveId] = useState("overview");
  const [lastCtx, setLastCtx] = useState(null);
  const navRef = useRef(null);
  const pillRef = useRef(null);
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

  // Thanh sáng trượt theo mục đang chọn; lần đầu và khi đổi bề rộng thì đặt
  // thẳng để không chạy từ vị trí 0.
  useLayoutEffect(() => {
    const nav = navRef.current,
      pill = pillRef.current;
    if (!nav || !pill) return;
    const place = (animate) => {
      const current = nav.querySelector("button.active");
      if (!current) return;
      if (!animate) pill.style.transition = "none";
      pill.style.transform = `translate(${current.offsetLeft}px, ${current.offsetTop}px)`;
      pill.style.width = `${current.offsetWidth}px`;
      pill.style.height = `${current.offsetHeight}px`;
      if (!animate) {
        void pill.offsetWidth;
        pill.style.transition = "";
      }
      pill.dataset.ready = "true";
    };
    place(pill.dataset.ready === "true");
    const resize = () => place(false);
    window.addEventListener("resize", resize);
    return () => window.removeEventListener("resize", resize);
  }, [activeId]);

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
          <nav
            className="admin-nav"
            aria-label="Danh mục quản trị"
            ref={navRef}
          >
            <span
              className="admin-nav-pill"
              aria-hidden="true"
              ref={pillRef}
            />
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
            <TableSkeleton />
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
  if (!isAdmin(user.role))
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
