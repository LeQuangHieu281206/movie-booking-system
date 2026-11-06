import {
  bookingPayload,
  isBookable,
  normalizeText,
  sameId,
} from "../lib/domain.js";

export { sameId };

export function reconcileSeats(ids, seats, showtimeId) {
  const available = new Set(
    seats
      .filter(
        (seat) =>
          seat.status === "AVAILABLE" && sameId(seat.showtimeId, showtimeId),
      )
      .map((seat) => String(seat.seatId)),
  );
  return [...new Set(ids)].filter((id) => available.has(String(id)));
}
export function changedShow(previous, latest) {
  return (
    ["showtimeId", "roomId", "movieId"].some(
      (key) => !sameId(previous[key], latest[key]),
    ) ||
    Number(previous.basePrice) !== Number(latest.basePrice) ||
    new Date(previous.startTime).getTime() !==
      new Date(latest.startTime).getTime()
  );
}
export async function readInventory(api, showtimeId, options) {
  const show = await api.showtime(showtimeId, options);
  if (!sameId(show?.showtimeId, showtimeId))
    throw new Error("API trả về sai suất chiếu. Vui lòng tải lại.");
  const seats = await api.seats(show, options);
  if (
    !Array.isArray(seats) ||
    seats.some((seat) => !sameId(seat.showtimeId, showtimeId))
  )
    throw new Error("Dữ liệu ghế không thuộc suất chiếu đang chọn.");
  return { show, seats, loadedAt: Date.now() };
}

// A changed inventory returns a review result, never sends createBooking.
export async function prepareBooking(
  api,
  { show, seats: previousSeats, ids, paymentMethod, reviewOnly = false },
) {
  const inventory = await readInventory(api, show.showtimeId);
  const validIds = reconcileSeats(
    ids,
    inventory.seats,
    inventory.show.showtimeId,
  );
  const removed = ids
    .filter((id) => !validIds.some((value) => sameId(value, id)))
    .map(
      (id) =>
        previousSeats.find((seat) => sameId(seat.seatId, id))?.label ||
        `#${id}`,
    );
  if (!isBookable(inventory.show))
    return {
      inventory,
      ids: validIds,
      review: "closed",
      message:
        "Suất chiếu đã bắt đầu hoặc ngừng nhận đặt vé. Hãy chọn suất khác.",
    };
  if (removed.length)
    return {
      inventory,
      ids: validIds,
      review: "seats",
      message: `Ghế ${removed.join(", ")} không còn trống. Đã bỏ các ghế này; vui lòng kiểm tra lại lựa chọn.`,
    };
  if (changedShow(show, inventory.show))
    return {
      inventory,
      ids: validIds,
      review: "show",
      message:
        "Giá vé hoặc thông tin suất chiếu vừa thay đổi. Đã cập nhật thông tin mới; vui lòng xác nhận lại.",
    };
  return {
    inventory,
    ids: validIds,
    payload: reviewOnly
      ? null
      : bookingPayload(
          inventory.show,
          inventory.seats,
          validIds,
          paymentMethod,
        ),
  };
}

const BOOKING_STATUSES = ["PENDING", "PAID", "CANCELLED", "EXPIRED"];

function assertBookingShape(value) {
  if (
    !value ||
    !BOOKING_STATUSES.includes(value.status) ||
    !Number.isFinite(Number(value.totalAmount)) ||
    Number(value.totalAmount) <= 0
  ) {
    throw new Error(
      "Thông tin booking trả về không khớp. Đã dừng thao tác để tránh tác động nhầm vé.",
    );
  }
}

/** expected chỉ kiểm tra các trường được truyền (undefined = bỏ qua). */
const differs = (actual, expected) =>
  expected !== undefined && expected !== null && !sameId(actual, expected);

export function assertBooking(value, expected) {
  assertBookingShape(value);
  if (
    differs(value.bookingId, expected.bookingId) ||
    differs(value.userId, expected.userId) ||
    differs(value.showtimeId, expected.showtimeId) ||
    (expected.bookingCode !== undefined &&
      value.bookingCode !== expected.bookingCode)
  ) {
    throw new Error(
      "Thông tin booking trả về không khớp. Đã dừng thao tác để tránh tác động nhầm vé.",
    );
  }
  return value;
}

export function assertCreatedBooking(value, payload) {
  assertBookingShape(value);
  if (
    !Number.isSafeInteger(Number(value.bookingId)) ||
    Number(value.bookingId) <= 0 ||
    !value.bookingCode ||
    differs(value.userId, payload.userId) ||
    differs(value.showtimeId, payload.showtimeId)
  )
    throw new Error(
      "Chưa xác nhận được phản hồi tạo booking. Hãy kiểm tra Vé của tôi trước khi thử lại.",
    );
  return value;
}
export function assertUserBookings(items, userId) {
  if (
    !Array.isArray(items) ||
    items.some((item) => !sameId(item?.userId, userId))
  ) {
    throw new Error(
      "Danh sách vé trả về không khớp hồ sơ khách hàng. Vui lòng tải lại.",
    );
  }
  for (const item of items) assertBooking(item, item);
  return items;
}
export function filterTickets(
  bookings,
  { query = "", status = "", order = "newest", titleFor = () => "" } = {},
) {
  const keyword = normalizeText(query.trim());
  return bookings
    .filter(
      (booking) =>
        (!status || booking.status === status) &&
        normalizeText(
          `${booking.bookingCode} ${booking.bookingId} ${titleFor(booking)}`,
        ).includes(keyword),
    )
    .sort((a, b) => {
      const aTime = new Date(a.bookingTime).getTime() || 0,
        bTime = new Date(b.bookingTime).getTime() || 0;
      return order === "oldest" ? aTime - bTime : bTime - aTime;
    });
}
