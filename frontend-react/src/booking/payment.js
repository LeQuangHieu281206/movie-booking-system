import { assertBooking } from "./flow.js";
import { claimAction } from "./storage.js";

export function abortError() {
  return Object.assign(new Error("Đã dừng cập nhật."), { name: "AbortError" });
}
export function delay(ms, signal) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(abortError());
    const abort = () => {
      clearTimeout(timer);
      reject(abortError());
    };
    const timer = setTimeout(() => {
      signal?.removeEventListener("abort", abort);
      resolve();
    }, ms);
    signal?.addEventListener("abort", abort, { once: true });
  });
}
export async function runBookingAction({
  api,
  mode,
  booking,
  kind,
  store,
  signal,
  onUpdate = () => {},
  attempts = 8,
  wait = delay,
}) {
  if (!["cancel", "pay", "fail"].includes(kind))
    throw new Error("Thao tác không hợp lệ.");
  const release = claimAction(mode, booking);
  if (!release)
    throw new Error(
      "Vé này đang được xử lý. Vui lòng chờ và cập nhật trạng thái.",
    );
  try {
    if (signal?.aborted) throw abortError();
    const current = assertBooking(
      await api.booking(booking.bookingId, { signal }),
      booking,
    );
    if (signal?.aborted) throw abortError();
    onUpdate(current);
    if (current.status !== "PENDING") {
      store.clearAction(mode, current);
      return current;
    }
    if (store.action(mode, current))
      throw new Error(
        "Đã có yêu cầu thanh toán hoặc hủy cho vé này. Hãy cập nhật trạng thái, không gửi lặp lại.",
      );
    store.markAction(mode, current, kind);
    // Never abort/retry an outgoing mutation automatically; its outcome may be unknown.
    if (kind === "cancel") {
      const cancelled = assertBooking(
        await api.cancel(current.bookingId),
        current,
      );
      if (!signal?.aborted) onUpdate(cancelled);
      if (cancelled.status !== "PENDING") store.clearAction(mode, cancelled);
      return cancelled;
    }
    await api.pay(current, kind === "fail");
    let latest = current;
    for (let index = 0; index < attempts; index++) {
      if (signal?.aborted) throw abortError();
      latest = assertBooking(
        await api.booking(current.bookingId, { signal }),
        current,
      );
      if (signal?.aborted) throw abortError();
      onUpdate(latest);
      if (latest.status !== "PENDING") {
        store.clearAction(mode, latest);
        return latest;
      }
      if (index + 1 < attempts) await wait(1000, signal);
    }
    return latest;
  } finally {
    release();
  }
}
