import { date, money, statusText, time } from "./domain.js";

const labels = {
  P: "P",
  K: "K",
  T13: "T13",
  T16: "T16",
  T18: "T18",
  UPCOMING: "Sắp chiếu",
  SHOWING: "Đang chiếu",
  ENDED: "Đã kết thúc",
  SCHEDULED: "Đã lên lịch",
  ONGOING: "Đang chiếu",
  CANCELLED: "Đã huỷ",
  NORMAL: "Thường",
  VIP: "VIP",
  COUPLE: "Đôi",
  CASH: "Tiền mặt",
  BANK_TRANSFER: "Chuyển khoản",
  ADMIN: "Quản trị viên",
  CUSTOMER: "Khách hàng",
};

export const labelOf = (value) => statusText(value) === value ? labels[value] || value || "—" : statusText(value);

const options = (values) => values.map((value) => ({ value, label: labelOf(value) }));

const MOVIE_STATUS = options(["UPCOMING", "SHOWING", "ENDED"]);
const AGE = options(["P", "K", "T13", "T16", "T18"]);
const SHOWTIME_STATUS = options(["SCHEDULED", "ONGOING", "ENDED", "CANCELLED"]);
const ROOM_TYPE = ["2D", "3D", "IMAX", "4DX"].map((value) => ({ value, label: value }));
const SEAT_TYPE = options(["NORMAL", "VIP", "COUPLE"]);
const ROLE = options(["CUSTOMER", "ADMIN"]);

const text = (value) => (value == null ? "" : String(value));
const blankToNull = (value) => {
  const next = typeof value === "string" ? value.trim() : value;
  return next === "" || next == null ? null : next;
};
const numberOrNull = (value) =>
  value === "" || value == null ? null : Number(value);

export function toDateTimeInput(value) {
  if (!value) return "";
  const moment = new Date(value);
  if (Number.isNaN(moment.getTime())) return "";
  const pad = (part) => String(part).padStart(2, "0");
  return `${moment.getFullYear()}-${pad(moment.getMonth() + 1)}-${pad(moment.getDate())}T${pad(moment.getHours())}:${pad(moment.getMinutes())}`;
}

export function fromDateTimeInput(value) {
  return value ? new Date(value).toISOString() : null;
}

export function byId(list, key, labelKey, id) {
  const row = (list || []).find((item) => item[key] === id);
  return row?.[labelKey] || `#${id}`;
}

const bookingTime = (value) => (value ? `${time(value)} ${date(value)}` : "—");

export const ADMIN_RESOURCES = [
  {
    id: "movies",
    label: "Phim",
    icon: "film",
    group: "Nội dung",
    title: "Quản lý phim",
    description: "Thông tin phim, thể loại, độ tuổi và trạng thái phát hành.",
    listPath: "/getMovies",
    createPath: "/createMovie",
    updatePath: "/updateMovie",
    removePath: "/deleteMovie",
    idKey: "movieId",
    columns: [
      { label: "Tên phim", value: (row) => row.title },
      {
        label: "Thể loại",
        value: (row) =>
          (row.genres || []).map((genre) => genre.name).join(", ") || "—",
      },
      {
        label: "Thời lượng",
        value: (row) =>
          row.durationMinutes ? `${row.durationMinutes} phút` : "—",
      },
      { label: "Khởi chiếu", value: (row) => row.releaseDate || "—", nowrap: true },
      {
        label: "Trạng thái",
        value: (row) => labelOf(row.status),
        badge: true,
        tone: (row) => row.status,
      },
    ],
    fields: [
      { name: "title", label: "Tên phim", required: true, maxLength: 255, wide: true },
      { name: "description", label: "Mô tả", type: "textarea", wide: true },
      { name: "durationMinutes", label: "Thời lượng (phút)", type: "number", min: 1, step: 1 },
      { name: "language", label: "Ngôn ngữ" },
      { name: "releaseDate", label: "Ngày khởi chiếu", type: "date" },
      { name: "ageRestriction", label: "Giới hạn độ tuổi", type: "select", options: AGE },
      { name: "status", label: "Trạng thái", type: "select", options: MOVIE_STATUS, required: true },
      { name: "director", label: "Đạo diễn" },
      { name: "actors", label: "Diễn viên" },
      { name: "posterUrl", label: "URL áp phích", wide: true },
      { name: "trailerUrl", label: "URL trailer", wide: true },
      {
        name: "genreIds",
        label: "Thể loại",
        type: "genres",
        optionsKey: "genres",
        optionValue: "genreId",
        optionLabel: "name",
        wide: true,
      },
    ],
    toForm: (row) =>
      row
        ? {
            ...row,
            durationMinutes: row.durationMinutes ?? "",
            releaseDate: row.releaseDate || "",
            genreIds: (row.genres || []).map((genre) => genre.genreId),
          }
        : {
            title: "",
            description: "",
            durationMinutes: "",
            language: "",
            releaseDate: "",
            ageRestriction: "P",
            status: "SHOWING",
            director: "",
            actors: "",
            posterUrl: "",
            trailerUrl: "",
            genreIds: [],
          },
    toPayload: (values) => ({
      title: values.title.trim(),
      description: blankToNull(values.description),
      durationMinutes: numberOrNull(values.durationMinutes),
      language: blankToNull(values.language),
      releaseDate: blankToNull(values.releaseDate),
      posterUrl: blankToNull(values.posterUrl),
      trailerUrl: blankToNull(values.trailerUrl),
      ageRestriction: blankToNull(values.ageRestriction),
      status: blankToNull(values.status),
      director: blankToNull(values.director),
      actors: blankToNull(values.actors),
      genreIds: (values.genreIds || []).map(Number),
    }),
  },
  {
    id: "genres",
    label: "Thể loại",
    icon: "tag",
    group: "Nội dung",
    title: "Quản lý thể loại",
    description: "Danh mục thể loại dùng khi tạo và lọc phim.",
    listPath: "/getGenres",
    createPath: "/createGenre",
    updatePath: "/updateGenre",
    removePath: "/deleteGenre",
    idKey: "genreId",
    columns: [
      { label: "Mã", value: (row) => `#${row.genreId}` },
      { label: "Tên thể loại", value: (row) => row.name },
    ],
    fields: [
      { name: "name", label: "Tên thể loại", required: true, wide: true },
    ],
    toForm: (row) => ({ name: row?.name || "" }),
    toPayload: (values) => ({ name: values.name.trim() }),
  },
  {
    id: "showtimes",
    label: "Suất chiếu",
    icon: "calendar",
    group: "Nội dung",
    title: "Quản lý suất chiếu",
    description: "Lên lịch chiếu, phòng chiếu, giá vé và trạng thái suất.",
    listPath: "/getShowtimes",
    createPath: "/createShowtime",
    updatePath: "/updateShowtime",
    removePath: "/deleteShowtime",
    idKey: "showtimeId",
    columns: [
      {
        label: "Phim",
        value: (row, ctx) => byId(ctx.movies, "movieId", "title", row.movieId),
      },
      {
        label: "Rạp / phòng",
        value: (row, ctx) =>
          `${row.cinemaName || byId(ctx.cinemas, "cinemaId", "name", row.cinemaId)} · ${row.roomName || byId(ctx.rooms, "roomId", "name", row.roomId)}`,
      },
      {
        label: "Bắt đầu",
        value: (row) => bookingTime(row.startTime),
        nowrap: true,
      },
      { label: "Giá vé", value: (row) => money(row.basePrice) },
      {
        label: "Trạng thái",
        value: (row) => labelOf(row.status),
        badge: true,
        tone: (row) => row.status,
      },
    ],
    fields: [
      {
        name: "movieId",
        label: "Phim",
        type: "select",
        required: true,
        optionsKey: "movies",
        optionValue: "movieId",
        optionLabel: "title",
        wide: true,
      },
      {
        name: "roomId",
        label: "Phòng chiếu",
        type: "select",
        required: true,
        optionsKey: "rooms",
        optionValue: "roomId",
        optionLabel: "name",
      },
      {
        name: "status",
        label: "Trạng thái",
        type: "select",
        required: true,
        options: SHOWTIME_STATUS,
      },
      { name: "startTime", label: "Bắt đầu", type: "datetime-local", required: true },
      { name: "endTime", label: "Kết thúc", type: "datetime-local", required: true },
      { name: "basePrice", label: "Giá vé (VND)", type: "number", required: true, min: 0, step: 1000 },
    ],
    toForm: (row) =>
      row
        ? {
            movieId: row.movieId,
            roomId: row.roomId,
            startTime: toDateTimeInput(row.startTime),
            endTime: toDateTimeInput(row.endTime),
            basePrice: row.basePrice ?? "",
            status: row.status || "SCHEDULED",
          }
        : {
            movieId: "",
            roomId: "",
            startTime: "",
            endTime: "",
            basePrice: "",
            status: "SCHEDULED",
          },
    toPayload: (values) => ({
      movieId: Number(values.movieId),
      roomId: Number(values.roomId),
      startTime: fromDateTimeInput(values.startTime),
      endTime: fromDateTimeInput(values.endTime),
      basePrice: Number(values.basePrice),
      status: values.status,
    }),
  },
  {
    id: "cinemas",
    label: "Rạp",
    icon: "building",
    group: "Rạp chiếu",
    title: "Quản lý rạp",
    description: "Thông tin rạp, địa chỉ và số điện thoại liên hệ.",
    listPath: "/getCinemas",
    createPath: "/createCinema",
    updatePath: "/updateCinema",
    removePath: "/deleteCinema",
    idKey: "cinemaId",
    columns: [
      { label: "Tên rạp", value: (row) => row.name },
      { label: "Thành phố", value: (row) => row.city },
      { label: "Địa chỉ", value: (row) => row.address },
      { label: "Điện thoại", value: (row) => row.phoneNumber || "—" },
    ],
    fields: [
      { name: "name", label: "Tên rạp", required: true, wide: true },
      { name: "address", label: "Địa chỉ", required: true, wide: true },
      { name: "city", label: "Thành phố", required: true },
      {
        name: "phoneNumber",
        label: "Số điện thoại",
        maxLength: 12,
        pattern: "[0-9]{8,11}|[+]84[0-9]{9}",
        title: "Số điện thoại gồm 8-11 chữ số hoặc +84 theo sau 9 chữ số",
        inputMode: "tel",
      },
    ],
    toForm: (row) => ({
      name: row?.name || "",
      address: row?.address || "",
      city: row?.city || "",
      phoneNumber: row?.phoneNumber || "",
    }),
    toPayload: (values) => ({
      name: values.name.trim(),
      address: values.address.trim(),
      city: values.city.trim(),
      phoneNumber: blankToNull(values.phoneNumber),
    }),
  },
  {
    id: "rooms",
    label: "Phòng chiếu",
    icon: "room",
    group: "Rạp chiếu",
    title: "Quản lý phòng chiếu",
    description: "Phòng thuộc rạp, sức chứa và loại phòng.",
    listPath: "/getRooms",
    createPath: "/createRoom",
    updatePath: "/updateRoom",
    removePath: "/deleteRoom",
    idKey: "roomId",
    columns: [
      {
        label: "Rạp",
        value: (row, ctx) => byId(ctx.cinemas, "cinemaId", "name", row.cinemaId),
      },
      { label: "Phòng", value: (row) => row.name },
      { label: "Số ghế", value: (row) => row.totalSeats ?? "—" },
      { label: "Loại phòng", value: (row) => row.roomType || "—", badge: true },
    ],
    fields: [
      {
        name: "cinemaId",
        label: "Rạp",
        type: "select",
        required: true,
        optionsKey: "cinemas",
        optionValue: "cinemaId",
        optionLabel: "name",
        wide: true,
      },
      { name: "name", label: "Tên phòng", required: true },
      {
        name: "roomType",
        label: "Loại phòng",
        type: "select",
        required: true,
        options: ROOM_TYPE,
      },
      { name: "totalSeats", label: "Tổng số ghế", type: "number", required: true, min: 1, step: 1 },
    ],
    toForm: (row) =>
      row
        ? { ...row, totalSeats: row.totalSeats ?? "" }
        : { cinemaId: "", name: "", totalSeats: "", roomType: "2D" },
    toPayload: (values) => ({
      cinemaId: Number(values.cinemaId),
      name: values.name.trim(),
      totalSeats: numberOrNull(values.totalSeats),
      roomType: values.roomType,
    }),
  },
  {
    id: "seats",
    label: "Ghế",
    icon: "seat",
    group: "Rạp chiếu",
    title: "Quản lý ghế",
    description: "Ghế vật lý của từng phòng chiếu. Chọn phòng để xem danh sách.",
    listPath: (state) =>
      state.roomId ? `/getSeatsByRoomId?roomId=${state.roomId}` : null,
    createPath: "/createSeat",
    updatePath: "/updateSeat",
    removePath: "/deleteSeat",
    idKey: "id",
    requiresRoom: true,
    columns: [
      { label: "Mã ghế", value: (row) => `#${row.id}` },
      {
        label: "Vị trí",
        value: (row) => `${row.seatRow}${row.seatNumber}`,
      },
      {
        label: "Loại ghế",
        value: (row) => labelOf(row.seatType),
        badge: true,
        tone: (row) => row.seatType,
      },
    ],
    fields: [
      {
        name: "roomId",
        label: "Phòng chiếu",
        type: "select",
        required: true,
        optionsKey: "rooms",
        optionValue: "roomId",
        optionLabel: "name",
        wide: true,
      },
      { name: "seatRow", label: "Hàng ghế", required: true, maxLength: 5 },
      { name: "seatNumber", label: "Số ghế", type: "number", required: true, min: 1, step: 1 },
      { name: "seatType", label: "Loại ghế", type: "select", required: true, options: SEAT_TYPE },
    ],
    toForm: (row, ctx, state) =>
      row
        ? { ...row, roomId: row.roomId || state.roomId }
        : { roomId: state.roomId || "", seatRow: "", seatNumber: "", seatType: "NORMAL" },
    toPayload: (values) => ({
      roomId: Number(values.roomId),
      seatRow: values.seatRow.trim().toUpperCase(),
      seatNumber: numberOrNull(values.seatNumber),
      seatType: values.seatType,
    }),
  },
  {
    id: "users",
    label: "Người dùng",
    icon: "user",
    group: "Vận hành",
    title: "Quản lý người dùng",
    description: "Tài khoản khách hàng và quản trị viên, gán vai trò truy cập.",
    listPath: "/getUsers",
    createPath: "/createUser",
    updatePath: "/updateUser",
    removePath: "/deleteUser",
    idKey: "userId",
    columns: [
      { label: "Họ tên", value: (row) => row.fullName },
      { label: "Email", value: (row) => row.email },
      { label: "Điện thoại", value: (row) => row.phoneNumber || "—" },
      {
        label: "Vai trò",
        value: (row) => labelOf(row.role),
        badge: true,
        tone: (row) => row.role,
      },
    ],
    fields: [
      { name: "fullName", label: "Họ và tên", required: true, minLength: 2, maxLength: 100 },
      { name: "email", label: "Email", type: "email", required: true, maxLength: 150, readOnlyOnEdit: true },
      {
        name: "password",
        label: "Mật khẩu",
        type: "password",
        required: true,
        optionalOnEdit: true,
        minLength: 8,
        help: "Tối thiểu 8 ký tự. Khi sửa, để trống nếu không đổi mật khẩu.",
      },
      {
        name: "phoneNumber",
        label: "Số điện thoại",
        maxLength: 12,
        pattern: "[0-9]{8,11}|[+]84[0-9]{9}",
        title: "Số điện thoại gồm 8-11 chữ số hoặc +84 theo sau 9 chữ số",
        inputMode: "tel",
      },
      { name: "role", label: "Vai trò", type: "select", required: true, options: ROLE },
    ],
    toForm: (row) => ({
      fullName: row?.fullName || "",
      email: row?.email || "",
      password: "",
      phoneNumber: row?.phoneNumber || "",
      role: row?.role || "CUSTOMER",
    }),
    toPayload: (values, editing) => ({
      fullName: values.fullName.trim(),
      password: blankToNull(values.password),
      phoneNumber: blankToNull(values.phoneNumber),
      role: values.role,
      ...(editing ? {} : { email: values.email.trim() }),
    }),
  },
  {
    id: "bookings",
    label: "Đặt vé",
    icon: "ticket",
    group: "Vận hành",
    title: "Quản lý đặt vé",
    description: "Theo dõi giao dịch, huỷ hoặc đánh dấu hết hạn vé chờ thanh toán.",
    listPath: "/getBookings",
    updatePath: "/updateBooking",
    removePath: "/deleteBooking",
    idKey: "bookingId",
    canCreate: false,
    canEdit: false,
    columns: [
      { label: "Mã vé", value: (row) => row.bookingCode },
      { label: "Người dùng", value: (row) => `#${row.userId}` },
      { label: "Suất chiếu", value: (row) => `#${row.showtimeId}` },
      { label: "Tổng tiền", value: (row) => money(row.totalAmount) },
      {
        label: "Thanh toán",
        value: (row) => labelOf(row.paymentMethod),
      },
      {
        label: "Trạng thái",
        value: (row) => labelOf(row.status),
        badge: true,
        tone: (row) => row.status,
      },
      { label: "Thời gian", value: (row) => bookingTime(row.bookingTime), nowrap: true },
    ],
    fields: [],
    toForm: (row) => row || {},
    toPayload: (values) => ({ status: values.status }),
    quickActions: [
      { label: "Huỷ vé", status: "CANCELLED", when: (row) => row.status === "PENDING" },
      { label: "Hết hạn", status: "EXPIRED", when: (row) => row.status === "PENDING" },
    ],
    rowText: (row) =>
      `${row.bookingCode} #${row.userId} #${row.showtimeId} ${row.status}`,
  },
];

export const filterRows = (rows, query, resource, ctx) => {
  const needle = query.trim().toLowerCase();
  if (!needle) return rows;
  return rows.filter((row) =>
    (resource.rowText
      ? resource.rowText(row, ctx)
      : resource.columns
          .map((column) => column.value(row, ctx))
          .join(" ")
    )
      .toString()
      .toLowerCase()
      .includes(needle),
  );
};

export const columnText = (row, column, ctx) => text(column.value(row, ctx));
