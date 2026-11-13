// Cấu hình kỳ xem doanh số: mỗi kỳ là MỘT đơn vị thời gian đang diễn ra
// (hôm nay / tháng này / năm nay). Biểu đồ chia nhỏ đơn vị đó thành các mốc:
// ngày -> 24 giờ, tháng -> từng ngày, năm -> 12 tháng.
export const PERIODS = {
  day: {
    tab: "Ngày",
    chart: "Doanh thu hôm nay",
    compare: "hôm qua",
    note: "hôm nay",
    unit: "giờ",
  },
  month: {
    tab: "Tháng",
    chart: "Doanh thu tháng này",
    compare: "tháng trước",
    note: "tháng này",
    unit: "ngày",
  },
  year: {
    tab: "Năm",
    chart: "Doanh thu năm nay",
    compare: "năm trước",
    note: "năm nay",
    unit: "tháng",
  },
};

export const PERIOD_IDS = Object.keys(PERIODS);

export const periodOf = (value) => PERIODS[value] || PERIODS.day;

const pad = (value) => String(value).padStart(2, "0");

/**
 * Nhãn ngắn cho một mốc: 09h (giờ), 06/11 (ngày), 11/26 (tháng).
 * Key do backend trả: "YYYY-MM-DD HH" / YYYY-MM-DD / YYYY-MM.
 */
export function pointLabel(key) {
  if (!key) return "";
  if (key.length === 13) return `${key.slice(11, 13)}h`;
  if (key.length === 10) return `${key.slice(8, 10)}/${key.slice(5, 7)}`;
  if (key.length === 7) return `${key.slice(5, 7)}/${key.slice(2, 4)}`;
  return key;
}

/** Dải thời gian dưới tiêu đề; không có mốc nào thì dùng mô tả mặc định. */
export function periodRange(period, keys) {
  const first = keys[0];
  if (!first) return periodOf(period).note;
  if (period === "day")
    return `Hôm nay, ${first.slice(8, 10)}/${first.slice(5, 7)}/${first.slice(0, 4)}`;
  if (period === "month")
    return `Tháng ${pad(first.slice(5, 7))}/${first.slice(0, 4)}`;
  return `Năm ${first.slice(0, 4)}`;
}

/** Dải quá dày thì giãn nhãn trục: giữ tối đa ~6 nhãn trên một hàng. */
export const labelStep = (count) => (count > 7 ? Math.ceil(count / 6) : 1);
