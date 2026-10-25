function defaultStorage() {
  try {
    return globalThis.localStorage;
  } catch {
    return null;
  }
}
export function createBookingStore(storage = defaultStorage()) {
  const memory = new Map();
  function read(key) {
    try {
      const raw = storage?.getItem(key);
      return raw ? JSON.parse(raw) : memory.get(key) || null;
    } catch {
      return memory.get(key) || null;
    }
  }
  function write(key, value) {
    memory.set(key, value);
    try {
      storage?.setItem(key, JSON.stringify(value));
      return Boolean(storage);
    } catch {
      return false;
    }
  }
  function remove(key) {
    memory.delete(key);
    try {
      storage?.removeItem(key);
    } catch {
      /* In-memory state still works. */
    }
  }
  const identity = (mode, booking) =>
    [mode, booking.userId, booking.bookingId, booking.bookingCode]
      .map(encodeURIComponent)
      .join(":");
  const paymentKey = (mode, booking) =>
    `cineroom-pvt-action-v1:${identity(mode, booking)}`;
  return {
    action: (mode, booking) => read(paymentKey(mode, booking)),
    markAction: (mode, booking, kind) =>
      write(paymentKey(mode, booking), { kind, at: Date.now() }),
    clearAction: (mode, booking) => remove(paymentKey(mode, booking)),
    saveReceipt(mode, booking, show, movie, seats) {
      return write(`cineroom-pvt-receipt-v1:${identity(mode, booking)}`, {
        bookingId: booking.bookingId,
        bookingCode: booking.bookingCode,
        userId: booking.userId,
        showtimeId: booking.showtimeId,
        title: movie?.title || `Phim #${show.movieId}`,
        cinemaName: show.cinemaName || "",
        roomName: show.roomName || "",
        startTime: show.startTime,
        seats: seats.map((seat) => ({
          showtimeSeatId: seat.showtimeSeatId,
          label: seat.label,
        })),
        savedAt: Date.now(),
      });
    },
    receipt(mode, booking) {
      const value = read(`cineroom-pvt-receipt-v1:${identity(mode, booking)}`);
      return value &&
        String(value.showtimeId) === String(booking.showtimeId) &&
        Array.isArray(value.seats)
        ? value
        : null;
    },
    creation(mode, userId, showtimeId) {
      return read(`cineroom-pvt-create-v1:${mode}:${userId}:${showtimeId}`);
    },
    markCreation(mode, userId, showtimeId) {
      return write(`cineroom-pvt-create-v1:${mode}:${userId}:${showtimeId}`, {
        at: Date.now(),
        showtimeId,
      });
    },
    clearCreation(mode, userId, showtimeId) {
      remove(`cineroom-pvt-create-v1:${mode}:${userId}:${showtimeId}`);
    },
  };
}
export const bookingStore = createBookingStore();

const actionLocks = new Set();
export function claimAction(mode, booking) {
  const key = `${mode}:${booking.userId}:${booking.bookingId}:${booking.bookingCode}`;
  if (actionLocks.has(key)) return null;
  actionLocks.add(key);
  return () => actionLocks.delete(key);
}
