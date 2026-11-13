import React, { useId } from "react";
import { Icon } from "../UI.jsx";
import { Skeleton } from "../Skeleton.jsx";
import { money } from "../../lib/domain.js";
import {
  PERIODS,
  PERIOD_IDS,
  labelStep,
  periodOf,
  periodRange,
  pointLabel,
} from "../../lib/period.js";
import "./sales-dashboard.css";

const count = (value) => Number(value || 0).toLocaleString("vi-VN");
const shortMoney = (value) => {
  const amount = Number(value) || 0;
  const oneDecimal = (n) => n.toFixed(1).replace(".", ",").replace(",0", "");
  if (amount >= 1e9) return `${oneDecimal(amount / 1e9)} tỷ ₫`;
  if (amount >= 1e6) return `${oneDecimal(amount / 1e6)} tr ₫`;
  if (amount >= 1e3) return `${Math.round(amount / 1e3)}k ₫`;
  return `${amount} ₫`;
};

function trendOf(values) {
  if (values.length < 2) return null;
  const last = values.at(-1);
  const previous = values.at(-2);
  if (!previous) return null;
  return Math.round(((last - previous) / previous) * 100);
}

function niceCeil(value) {
  const step = value <= 10 ? 2 : value <= 40 ? 10 : value <= 120 ? 20 : 50;
  return Math.max(step, Math.ceil(value / step) * step);
}

// Quadratic smoothing through segment midpoints keeps the line soft without overshoot.
function Sparkline({ values, good = true }) {
  const id = useId().replace(/[^a-zA-Z0-9_-]/g, "");
  const width = 132;
  const height = 34;
  if (values.length < 2) return null;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const points = values.map((value, index) => [
    (index / (values.length - 1)) * width,
    height - 3 - ((value - min) / (max - min || 1)) * (height - 8),
  ]);
  let line = `M ${points[0][0].toFixed(1)} ${points[0][1].toFixed(1)}`;
  for (let index = 1; index < points.length - 1; index++) {
    const mx = (points[index][0] + points[index + 1][0]) / 2;
    const my = (points[index][1] + points[index + 1][1]) / 2;
    line += ` Q ${points[index][0].toFixed(1)} ${points[index][1].toFixed(1)} ${mx.toFixed(1)} ${my.toFixed(1)}`;
  }
  const last = points.at(-1);
  line += ` T ${last[0].toFixed(1)} ${last[1].toFixed(1)}`;
  const area = `${line} L ${width} ${height} L 0 ${height} Z`;

  return (
    <svg
      className="sd-spark"
      data-good={good}
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id={`sd-g${id}`} x1="0" x2="0" y1="0" y2="1">
          <stop offset="0" stopColor="currentColor" stopOpacity="0.28" />
          <stop offset="1" stopColor="currentColor" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#sd-g${id})`} />
      <path
        d={line}
        fill="none"
        stroke="currentColor"
        strokeWidth={1.6}
        vectorEffect="non-scaling-stroke"
      />
      <circle cx={last[0]} cy={last[1]} r={2.6} fill="currentColor" />
    </svg>
  );
}

const SKELETON_BARS = { day: 24, month: 30, year: 12 };
const SKELETON_HEIGHTS = [42, 68, 55, 84, 62, 92, 74, 58, 80, 48, 66, 88];

export function SalesDashboardSkeleton({ period = "day" }) {
  const bars = SKELETON_BARS[period] || SKELETON_BARS.day;
  return (
    <section className="sales-dashboard" role="status">
      <span className="sr-only">Đang tải thống kê…</span>
      <header className="sd-head" aria-hidden="true">
        <div>
          <Skeleton className="sd-skel sd-skel-eyebrow" />
          <Skeleton className="sd-skel sd-skel-title" />
          <Skeleton className="sd-skel sd-skel-sub" />
        </div>
        <div className="sd-actions">
          <Skeleton className="sd-skel sd-skel-range" />
          <Skeleton className="sd-skel sd-skel-refresh" />
        </div>
      </header>
      <ul className="sd-stats" aria-hidden="true">
        {[0, 1, 2].map((index) => (
          <li className="sd-stat" key={index}>
            <Skeleton className="sd-skel sd-skel-label" />
            <Skeleton className="sd-skel sd-skel-value" />
            <Skeleton className="sd-skel sd-skel-spark" />
          </li>
        ))}
      </ul>
      <section className="sd-card" aria-hidden="true">
        <header className="sd-card-head">
          <Skeleton className="sd-skel sd-skel-card-title" />
          <Skeleton className="sd-skel sd-skel-legend" />
        </header>
        {/* Số cột khớp với kỳ sắp hiện để khung tải không nhảy khi có dữ liệu. */}
        <div className="sd-skel-chart" style={{ "--cols": bars }}>
          {Array.from({ length: bars }, (_, index) => (
            <Skeleton
              className="sd-skel sd-skel-bar"
              key={index}
              style={{ height: `${SKELETON_HEIGHTS[index % SKELETON_HEIGHTS.length]}%` }}
            />
          ))}
        </div>
      </section>
    </section>
  );
}

export function SalesDashboard({ data, loading = false, onReload, period = "day", onPeriodChange }) {
  // Dùng period của chính dữ liệu để số liệu cũ (đang tải kỳ mới) vẫn được gắn nhãn đúng.
  const shownPeriod = data?.period || period;
  const labels = periodOf(shownPeriod);
  const points = data?.revenueSeries || [];
  const revenue = points.map((point) => Number(point.revenue) || 0);
  const bookings = points.map((point) => Number(point.paidBookings) || 0);
  const totalRevenue = revenue.reduce((sum, value) => sum + value, 0);
  const totalBookings = bookings.reduce((sum, value) => sum + value, 0);
  const tickets = Number(data?.ticketsSold) || 0;
  // Tổng của 3 card lấy từ chính chuỗi mốc để luôn khớp với biểu đồ.
  const paidRevenue = totalRevenue;
  const paidBookings = totalBookings;
  const keys = points.map((point) => point.key);
  const range = periodRange(shownPeriod, keys);
  // Dải 12 tháng thì cách quãng nhãn trục; nhãn cách quãng chỉ ẩn ở màn hình hẹp.
  const step = labelStep(points.length);

  const stats = [
    {
      id: "revenue",
      label: "Doanh thu",
      icon: "dashboard",
      value: shortMoney(paidRevenue),
      title: money(paidRevenue),
      spark: revenue,
      delta: trendOf(revenue),
    },
    {
      id: "bookings",
      label: "Doanh số",
      icon: "chart",
      value: `${count(paidBookings)} đơn`,
      spark: bookings,
      delta: trendOf(bookings),
    },
    {
      id: "tickets",
      label: "Vé bán",
      icon: "ticket",
      value: `${count(tickets)} vé`,
      note: "vé đã bán trong kỳ",
    },
  ];

  const ceil = niceCeil(Math.max(0, ...revenue));
  const summary = points
    .map((point, index) => `${pointLabel(point.key)} ${money(revenue[index])}`)
    .join(", ");

  return (
    <section
      className="sales-dashboard"
      aria-labelledby="sales-title"
      aria-busy={loading || undefined}
    >
      <header className="sd-head sd-enter" style={{ "--i": 0 }}>
        <div>
          <span className="sd-eyebrow">
            <Icon name="dashboard" size={14} />
            CINEROOM / TỔNG QUAN
          </span>
          <h1 className="sd-title" id="sales-title">
            Doanh số
          </h1>
          <p className="sd-sub">{range} · Chỉ tính các đơn đã thanh toán</p>
        </div>
        <div className="sd-actions">
          <div
            className="sd-range"
            role="group"
            aria-label="Xem doanh số theo"
            style={{ "--sd-index": Math.max(0, PERIOD_IDS.indexOf(period)) }}
          >
            {PERIOD_IDS.map((id) => (
              <button
                key={id}
                type="button"
                aria-pressed={id === period}
                onClick={() => onPeriodChange?.(id)}
              >
                {PERIODS[id].tab}
              </button>
            ))}
          </div>
          <button
            type="button"
            className={`sd-refresh ${loading ? "is-loading" : ""}`}
            onClick={onReload}
            disabled={loading}
            aria-label="Tải lại số liệu"
            title="Tải lại số liệu"
          >
            <Icon name="refresh" size={16} />
          </button>
        </div>
      </header>

      <ul className="sd-stats" key={`stats-${shownPeriod}`}>
        {stats.map((stat, index) => (
          <li
            className="sd-stat sd-enter"
            key={stat.id}
            style={{ "--i": index + 1 }}
          >
            <span className="sd-stat-label">
              <Icon name={stat.icon} size={16} />
              {stat.label}
            </span>
            <div className="sd-stat-row">
              <strong className="sd-stat-value" title={stat.title}>
                {stat.value}
              </strong>
              {stat.delta !== null && stat.delta !== undefined && (
                <span
                  className="sd-delta"
                  data-good={stat.delta >= 0}
                  title={`So với ${labels.compare}`}
                >
                  <Icon
                    name="arrow"
                    size={13}
                    style={{
                      transform: `rotate(${stat.delta < 0 ? 90 : -90}deg)`,
                    }}
                  />
                  {stat.delta < 0 ? "−" : "+"}
                  {Math.abs(stat.delta)}%
                </span>
              )}
            </div>
            {stat.spark ? (
              <Sparkline
                values={stat.spark}
                good={stat.delta === null || stat.delta >= 0}
              />
            ) : (
              <span className="sd-stat-note">{stat.note}</span>
            )}
          </li>
        ))}
      </ul>

      <section
        className="sd-card sd-enter"
        key={`chart-${shownPeriod}`}
        style={{ "--i": 4 }}
      >
        <header className="sd-card-head">
          <h2 className="sd-card-title">{labels.chart}</h2>
          <ul className="sd-legend" aria-label="Chú thích">
            <li>
              <i aria-hidden="true" />
              Doanh thu
            </li>
          </ul>
        </header>
        <figure
          className="sd-chart"
          role="img"
          aria-label={`Doanh thu theo ${labels.unit}: ${summary}`}
        >
          <div className="sd-grid" aria-hidden="true">
            {[1, 0.5, 0].map((fraction) => (
              <span key={fraction} data-value={shortMoney(ceil * fraction)} />
            ))}
          </div>
          <ol
            className={`sd-cols${points.length > 12 ? " is-dense" : ""}`}
            aria-hidden="true"
            style={{ "--cols": Math.max(1, points.length) }}
          >
            {points.map((point, index) => (
              <li
                className="sd-col"
                key={point.key}
                style={{ "--col": index }}
              >
                <div
                  className="sd-bar-wrap has-tip tip-top"
                  data-tip={`${pointLabel(point.key)} · ${money(revenue[index])} · ${bookings[index]} đơn`}
                  style={{
                    height: `${(revenue[index] / ceil) * 100}%`,
                  }}
                >
                  <div className="sd-bar">
                    <span className="sd-bar-seg" />
                  </div>
                </div>
                <span
                  className={`sd-col-label${index % step ? " is-alt" : ""}`}
                >
                  {pointLabel(point.key)}
                </span>
              </li>
            ))}
          </ol>
        </figure>
      </section>
    </section>
  );
}

export default SalesDashboard;
