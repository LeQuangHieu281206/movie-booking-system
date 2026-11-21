# Đối chiếu báo cáo “Nhóm 8 – TTCSN” với mã nguồn dự án

Tài liệu này liệt kê những chỗ báo cáo **không khớp** với mã nguồn hiện tại của
`movie-booking-system`, kèm nội dung viết lại để dán trực tiếp vào báo cáo.
Số dòng là vị trí trong file `Bản sao của Nhóm 8 - TTCSN(1).md` tại thời điểm
đối chiếu; nếu bạn sửa file thì số dòng sẽ dịch, hãy tìm theo trích dẫn.

Nguồn đối chiếu: `README.md`, `pom.xml`, `api-gateway/`, `*/src/main/java`,
`database/migrations/`, `frontend-react/`.

---

## A. Danh sách nội dung lệch

### A1. Công nghệ và kiến trúc

| # | Vị trí | Báo cáo đang ghi | Dự án thực tế |
| - | ------ | ---------------- | ------------- |
| 1 | 1.5, dòng 369 | Giao diện “HTML, CSS, JavaScript và Bootstrap” | React 19.3 + Vite 8.3, JSX, CSS thuần theo design token; không có Bootstrap (`frontend-react/package.json`) |
| 2 | 1.5, dòng 370 | “Sử dụng cơ sở dữ liệu MySQL” | PostgreSQL trên Supabase (`spring.datasource.url=jdbc:postgresql://…supabase.com:6543/postgres`) |
| 3 | 1.6.2 (2), dòng 403 | “Microsoft SQL Server được đề xuất…” | PostgreSQL/Supabase; toàn bộ migration viết cho PostgreSQL (`database/migrations/*.sql`) |
| 4 | 1.6.2 (3) | Chỉ HTML/CSS/JavaScript thủ công | React component + Vite, `@react-oauth/google` cho đăng nhập Google |
| 5 | 3.1, dòng 633–639 | “Hệ thống quản lý và chia sẻ bài viết trực tuyến **Edushare**… kiến trúc 3 lớp… tìm kiếm khóa học, xem bài học” | CineRoom – website bán vé phim, kiến trúc **SOA**: frontend → API Gateway → 6 service nghiệp vụ + Eureka, đồng bộ OpenFeign, bất đồng bộ RabbitMQ |
| 6 | Toàn chương 3 | Không nhắc Gateway/Eureka/Feign/RabbitMQ/microservice | Chương 4 của chính báo cáo lại mô tả các service và endpoint nội bộ → chương 3 và chương 4 đang mâu thuẫn nhau |

### A2. Chức năng (chương 1–2)

| # | Vị trí | Báo cáo đang ghi | Dự án thực tế |
| - | ------ | ---------------- | ------------- |
| 7 | 1.4, dòng 329 | Cập nhật “họ tên, email, số điện thoại và đổi mật khẩu” | `PUT /api/users/me` chỉ nhận `fullName`, `phoneNumber`; không có API nào đổi email sau khi tạo, cũng không có API đổi mật khẩu tự phục vụ (chỉ Admin đặt lại qua `updateUser`) |
| 8 | 1.4, dòng 331–332 | Admin “khóa/mở khóa” tài khoản | Không có cột trạng thái/khóa; Admin chỉ tạo, sửa (kể cả `role`, `password`), xóa user |
| 9 | 1.4, dòng 339 | Rạp có “trạng thái hoạt động” | Bảng `cinemas` chỉ có `cinema_id, name, address, city, phone_number, created_at, updated_at` |
| 10 | 1.4, dòng 340 | Phòng chiếu “thiết lập sơ đồ ghế” | Không có công cụ vẽ sơ đồ ghế; Admin nhập `totalSeats` rồi tạo từng ghế (`roomId, seatRow, seatNumber, seatType`) |
| 11 | 1.4, dòng 352 | Vé điện tử có “mã QR/mã xác nhận” | Vé chỉ có `bookingCode` dạng chữ; QR chỉ dùng cho **thanh toán chuyển khoản**, không phải QR soát vé |
| 12 | 1.4, dòng 353 | Trạng thái vé “đã đặt, đã thanh toán, đã sử dụng, đã hủy…” | Chỉ có `PENDING, PAID, CANCELLED, EXPIRED`; không có trạng thái “đã sử dụng” |
| 13 | 1.4, mục Thanh toán + 1.3 | Khảo sát ưu tiên ví điện tử/thẻ, gợi ý mở rộng | Chỉ hỗ trợ `CASH` và `BANK_TRANSFER` (SePay VietQR); `CARD`, `E_WALLET` đã bị migration `002` loại bỏ |
| 14 | 1.4 / mục Quản lý vé | “mã QR sau khi thanh toán”, gửi vé qua email | `notification-service` chỉ ghi log “Đã gửi Email vé…”, chưa gửi email thật; vé không có QR |
| 15 | 2.3.1, dòng 487 | Đăng nhập bằng “email/tên đăng nhập”, “tạo phiên đăng nhập” | Chỉ đăng nhập bằng email; xác thực **JWT stateless** (access + refresh), không tạo session phía server |
| 16 | 2.3.1, bước A4 | “Nếu tài khoản bị khóa hoặc vô hiệu hóa…” | Không tồn tại trạng thái khóa tài khoản |
| 17 | 2.3.3, bước 5 | Sửa “họ tên, số điện thoại, email, ngày sinh…” | Chỉ `fullName` và `phoneNumber`; email read-only, không có ngày sinh |
| 18 | 2.3.5 / Bảng 2.6 | Use case “Quản lý danh mục phim” nhưng tác nhân là **Khách hàng**, mô tả lại là đặt vé/thanh toán | Phải là tác nhân **Quản trị viên**, nghiệp vụ thêm/sửa/xóa phim, thể loại, trạng thái phim |
| 19 | 1.4 | Không nhắc đăng nhập Google, xác nhận tiền mặt tại quầy | Đã có `POST /api/auth/google` và `POST /api/confirmCashPayment` (Admin) |
| 20 | 3.1.3 | “Giao diện danh sách phim” là màn hình riêng | Đã bỏ trang “Phim”; danh sách phim nằm ngay trang chủ, menu chỉ còn Trang chủ / Lịch chiếu / Rạp chiếu / Vé của tôi / Tài khoản |

### A3. API (chương 4)

| # | Vị trí | Báo cáo đang ghi | Dự án thực tế |
| - | ------ | ---------------- | ------------- |
| 21 | 4.2.4, dòng 809 | `GET /api/get/cities` | `GET /api/getCities` (`CinemaController`) |
| 22 | 4.2.4, dòng 811 | Body tạo rạp: `phonenumber?` | `phoneNumber` (`CinemaRequest`) |
| 23 | 4.2.4 | `POST /api/createRoom` để trống cột Response | Trả `201` + `RoomResponse` |
| 24 | 4.2.3, dòng 789 | `GET /api/getShowtimeSeats?ShowtimeId=` | Tham số là `showtimeId` (chữ thường) |
| 25 | 4.2.5, dòng 835 | `GET /api/getBookingsStat` → `{revenue, paidBookings, ticketsSold, dailyRevenue[7]}` | `GET /api/getBookingStats?period=day|month|year` → `{revenue, paidBookings, ticketsSold, period, revenueSeries[{key, revenue, paidBookings}]}` |
| 26 | 4.2.5 | Không có xác nhận tiền mặt | Thiếu `POST /api/confirmCashPayment?id=` (ADMIN, chỉ booking `CASH` + `PENDING`) |
| 27 | 4.2.1 | Không có đăng nhập Google | Thiếu `POST /api/auth/google` (body `{idToken}` → `AuthResponse`) |
| 28 | 4.1 | “204 No Content (xoá)” cho mọi lệnh xóa | Chỉ movie/genre/cinema/room/seat/showtime trả 204; `deleteBooking`, `deleteUser` trả `200` + chuỗi thông báo |
| 29 | 4.1, bảng mã lỗi | Truy cập tài nguyên của người khác → 404 | Ném `ForbiddenException` → **403** |
| 30 | 4.1, bảng mã lỗi | “path không khớp rule gateway → 404” | Gateway mặc định coi path lạ là `ADMIN`, người không phải Admin nhận **403** |
| 31 | 4.2.7 | Có `GET /internal/showtimes/{showtimeId}/exists` | Không tồn tại; booking đọc suất chiếu qua `MovieClient` gọi `GET /api/getShowtimeById` |
| 32 | 4.2.7 | Thiếu endpoint nội bộ của ghế | Có `POST /internal/seats/by-ids` (venue) do booking dùng khi tạo booking |
| 33 | 2.3.2 / 4.2.5 | `updateBooking` nhận `{status}` chung chung | Chỉ `PENDING → CANCELLED|EXPIRED`; gửi `PAID` bị từ chối `409` (PAID chỉ do listener thanh toán đặt) |
| 34 | 2.3 “Bất đồng bô” | payment → notification “gửi thông báo” | `notification-service` chỉ log, chưa tích hợp SMTP; cần ghi rõ là mô phỏng |

### A4. Cơ sở dữ liệu (chương 5)

| # | Vị trí | Báo cáo đang ghi | Dự án thực tế |
| - | ------ | ---------------- | ------------- |
| 35 | 5.2.2, dòng 927 | `users.password_hash` **Not Null** | Cho phép NULL (tài khoản Google) – migration `004` |
| 36 | 5.2.2 Users | Thiếu cột | Thiếu `auth_provider` (`LOCAL`/`GOOGLE`, NOT NULL) và `google_id` (unique, nullable) |
| 37 | 5.2.2, dòng 1048 | `showtime_seats.hold_by INT8`, FK → users | Thực tế là `TEXT`, **không có FK** (đúng như mục 5.2.3 quan hệ 12 đã ghi) |
| 38 | 5.2.2, dòng 1066 | `bookings.payment_method` kiểu `payment_method_enum`, giá trị gồm `CARD, E_WALLET` | Vẫn là enum `payment_method_enum` nhưng chỉ còn `CASH`, `BANK_TRANSFER` (migration `002_remove_card_ewallet_payment_methods`) |
| 39 | 5.2.2, dòng 1067 | `bookings.booking_time` “Có thể Null” | `NOT NULL DEFAULT now()` (migration `001`) |
| 40 | 5.2.2, dòng 1096 | `payments.status` kiểu `payment_status_enum` | Đúng là enum `payment_status_enum`, nhưng có thêm `CANCELLED`, `EXPIRED` (5 giá trị) |
| 41 | 5.2.2, dòng 1097 | `payments.provider` “Có thể Null” | `NOT NULL DEFAULT 'SEPAY'` |
| 42 | 5.2.2 Payments | `currency TEXT` | `varchar(8) NOT NULL DEFAULT 'VND'` |
| 43 | 5.2.2 Payments | `provider_txn_id` unique | Unique theo cặp `(provider, provider_txn_id)`; thêm partial unique index: mỗi booking tối đa 1 payment `PENDING` |
| 44 | 5.2.2, dòng 1122–1123 | Bảng enum có `payment_method_enum`, `payment_status_enum` | 10 enum native, gồm cả `payment_status_enum` (5 giá trị) |
| 45 | 5.2.3, dòng 1168 | `booking_details` có **composite FK** `(showtime_seat_id, seat_id)` bảo đảm khớp nhau | Không có composite FK; chỉ có 2 FK độc lập: `seat_id → seats`, `showtime_seat_id → showtime_seats` |
| 46 | 5.3 | “Các khóa ngoại, composite FK…” | Bỏ “composite FK”, thay bằng “hai khóa ngoại độc lập + ràng buộc NOT NULL” |

### A5. Trình bày, đánh số chương/hình/bảng

| # | Vị trí | Vấn đề |
| - | ------ | ------ |
| 47 | Mục lục, dòng 97 | Mục lục gọi chương CSDL là **CHƯƠNG 4**, nhưng thân bài là **CHƯƠNG 5**; mục lục thiếu hẳn chương API (thân bài là CHƯƠNG 4) |
| 48 | Mục lục, dòng 97 | Link `#chương-5-…` trỏ sai số chương so với tiêu đề “CHƯƠNG 4” |
| 49 | Chương 4 API | Hình/bảng đánh số 4.x; chương CSDL cũng đánh 4.x → trùng `Hình 4.1`, `Bảng 4.1` |
| 50 | Chương 4 | Nhiều bảng bị đặt nhãn “Hình” (Hình 4.1 Quy ước chung, Hình 4.2 mã lỗi, Hình 4.10 danh sách api authentication) |
| 51 | 2.3.5 / danh mục bảng | Hai bảng cùng số **Bảng 2.5**, bảng sau lẽ ra là 2.6 |
| 52 | 3.1.7 | Tiêu đề “chi tiết **khim**”, sai chính tả |
| 53 | Mục lục / 2.5 / 3.1.4 / 4.2.4 | “đặt vé xem **phm**”, “Use case **uho** phép”, “địa **chí** rạp”, “**RooomRequeest**” – lỗi gõ |
| 54 | 3.1.3 / 3.1.1 | Ảnh chụp giao diện cũ (còn trang Phim, còn select mặc định, chưa có wizard 5 bước) không còn khớp UI hiện tại |

---

## B. Nội dung viết lại (dán thay thế)

### B1. 1.5 – Yêu cầu phi chức năng, phần “Yêu cầu kỹ thuật”

> + Giao diện xây dựng bằng **React 19 + Vite 8** (JavaScript/JSX, CSS thuần theo
> design token), chạy ở cổng 5173 và proxy `/api/**` về API Gateway.
> + Nghiệp vụ xây dựng bằng **Java 21, Spring Boot 3.5.6, Spring Cloud 2025.0.0**
> (Spring Web MVC, Spring Data JPA, OpenFeign, Spring AMQP).
> + Hệ thống tổ chức theo **kiến trúc hướng dịch vụ (SOA)**: một API Gateway
> (cổng 8080) điều hướng tới các service độc lập đăng ký với Eureka; giao tiếp
> đồng bộ qua OpenFeign và bất đồng bộ qua RabbitMQ.
> + Cơ sở dữ liệu **PostgreSQL trên Supabase**, dùng chung cho các service.
> + Xác thực bằng **JWT stateless (HS256)** gồm access token và refresh token;
> mật khẩu băm BCrypt; Gateway kiểm tra token và gắn `X-User-Id`, `X-User-Role`,
> `X-Internal-Token` trước khi chuyển tiếp.
> + Hoạt động ổn định trên Google Chrome, Microsoft Edge và Mozilla Firefox.

### B2. 1.6.2 – Công cụ sử dụng, mục (2) và (3)

**(2) Hệ quản trị cơ sở dữ liệu**

> Hệ thống dùng **PostgreSQL** triển khai trên **Supabase**. Toàn bộ bảng, kiểu
> ENUM, khóa ngoại và index được tạo bằng script SQL trong thư mục
> `database/migrations/`; dữ liệu dùng chung cho tất cả service nên các ràng buộc
> toàn vẹn được giữ ở mức cơ sở dữ liệu. Supabase cung cấp sẵn kết nối TLS qua
> pooler (cổng 6543), phù hợp với môi trường chạy thử.
>
> - Dữ liệu danh mục: người dùng, phim, thể loại, cụm rạp, phòng chiếu, ghế,
>   suất chiếu.
> - Dữ liệu giao dịch: đơn đặt vé, chi tiết đơn, trạng thái ghế theo suất chiếu,
>   thanh toán.
> - Ràng buộc: `showtime_seats` unique theo `(showtime_id, seat_id)`;
>   `booking_details` tham chiếu cả `seats` và `showtime_seats`; mỗi booking chỉ
>   có tối đa một payment `PENDING`.

**(3) Công nghệ giao diện**

> Giao diện là ứng dụng **React 19** tạo bằng **Vite**, chia thành các trang
> Trang chủ, Lịch chiếu, Rạp chiếu, Vé của tôi, Tài khoản và khu Quản trị. Luồng
> đặt vé được tổ chức thành 5 bước (phim → thành phố → rạp → ngày → suất chiếu)
> rồi mở hộp thoại chọn ghế. Trạng thái đăng nhập lưu ở `localStorage`
> (`cineroom-auth`), giao diện hỗ trợ sáng/tối (`cineroom-theme`) và đăng nhập
> Google qua `@react-oauth/google`.

### B3. 3.1 – Kiến trúc phần mềm (thay toàn bộ đoạn Edushare)

> Hệ thống bán vé xem phim CineRoom được triển khai theo **kiến trúc hướng dịch
> vụ (SOA)**. Về mặt logic vẫn có ba tầng: tầng giao diện, tầng nghiệp vụ và
> tầng dữ liệu; nhưng tầng nghiệp vụ không phải một khối duy nhất mà được chia
> thành các service độc lập, mỗi service sở hữu một nhóm nghiệp vụ.
>
> - **Tầng giao diện:** ứng dụng React/Vite chạy cổng 5173, gọi API qua
>   `/api/**`; Vite proxy các request này sang API Gateway.
> - **Tầng Gateway:** `api-gateway` (8080) là điểm vào duy nhất, xác thực JWT,
>   gắn thông tin người dùng vào header nội bộ, kiểm tra quyền theo
>   phương thức + đường dẫn rồi định tuyến tới service qua Eureka.
> - **Tầng nghiệp vụ:** `venue-service` (8083) quản lý rạp/phòng/ghế;
>   `movie-service` (8081) quản lý phim/thể loại/suất chiếu/trạng thái ghế;
>   `booking-service` (8082) quản lý đơn đặt vé và giữ ghế;
>   `user-service` (8084) quản lý tài khoản và phát hành JWT;
>   `payment-service` (8086) tạo QR SePay và xử lý webhook;
>   `notification-service` (8087) nhận sự kiện thanh toán.
>   `eureka-server` (8761) làm dịch vụ đăng ký và phát hiện service.
> - **Tầng dữ liệu:** một cơ sở dữ liệu **PostgreSQL trên Supabase** dùng chung.
>
> Giao tiếp giữa các service gồm hai kiểu: **đồng bộ** bằng OpenFeign khi cần
> phản hồi ngay (kiểm tra người dùng, đọc suất chiếu, giữ/nhả ghế, đọc đơn để
> đối soát) và **bất đồng bộ** bằng RabbitMQ khi thanh toán hoàn tất: payment
> publish routing key `payment.completed` lên topic exchange
> `mb.topic.exchange`, booking và notification mỗi bên có queue riêng nhận sự
> kiện này. Nhờ vậy, việc cập nhật trạng thái vé và gửi thông báo không làm chậm
> phản hồi webhook.
>
> Các endpoint `/internal/**` không được mở ở Gateway và chỉ dùng cho gọi
> service-to-service; các service nghiệp vụ bind `127.0.0.1` nên không truy cập
> được từ ngoài máy chủ.

### B4. 3.1.3 – Thiết kế giao diện danh sách phim

> Danh sách phim được hiển thị ngay trên **trang chủ**, không còn trang “Phim”
> riêng. Mỗi thẻ phim gồm poster, tên phim, thể loại và thời lượng; phía trên có
> ô tìm kiếm theo tên và bộ lọc thể loại. Người dùng bấm vào thẻ để mở trang chi
> tiết phim hoặc chuyển thẳng sang luồng chọn suất chiếu.

### B5. 3.1.7 – Sửa tiêu đề

> **3.1.7. Thiết kế giao diện chi tiết phim**

### B6. Đánh số chương, hình, bảng

- Giữ nguyên thân bài: **CHƯƠNG 4 – THIẾT KẾ API VÀ GIAO TIẾP DỊCH VỤ**,
  **CHƯƠNG 5 – THIẾT KẾ VÀ CÀI ĐẶT CƠ SỞ DỮ LIỆU**.
- Sửa mục lục: thêm mục “CHƯƠNG 4: THIẾT KẾ API VÀ GIAO TIẾP DỊCH VỤ”, đổi mục
  CSDL thành “CHƯƠNG 5”, sửa anchor `#chương-5-…` cho khớp.
- Chương 4: giữ `Hình 4.1…4.10` nhưng đổi các bảng sang `Bảng 4.1…4.x`.
- Chương 5: đổi `Hình 4.1` → `Hình 5.1`; `Bảng 4.1…4.13` → `Bảng 5.1…5.13`.
- Bảng use case quản lý danh mục phim: `Bảng 2.6` (bảng đặt vé giữ `Bảng 2.5`).
- Sửa chính tả: “chi tiết phim”, “đặt vé xem phim”, “dự án phần mềm”,
  “use case cho phép”.

### B7. 4.1 – Quy ước chung (sửa 3 dòng)

> | Mã thành công | 200 OK (đọc/cập nhật/xóa user, xóa booking); 201 Created (tạo mới); 204 No Content (xóa movie, genre, cinema, room, seat, showtime) |
>
> | 403 | Đã xác thực nhưng sai role; truy cập tài nguyên của người khác; đường dẫn không khớp rule nào của Gateway (mặc định chỉ ADMIN) |
>
> | 404 | Không tìm thấy tài nguyên (id không tồn tại) |

### B8. 4.2.1 – Bảng API xác thực (thêm Google)

Thêm ngay sau dòng `POST /api/auth/login`:

> | POST | /api/auth/google | Public | {idToken} | 200 {accessToken, refreshToken, tokenType, expiresIn, user}; tự tạo tài khoản `auth_provider=GOOGLE` nếu email chưa tồn tại |

Và sửa dòng `PUT /api/users/me`:

> | PUT | /api/users/me | CUSTOMER/ADMIN | {fullName, phoneNumber?} | 200 hồ sơ sau cập nhật (email và mật khẩu không đổi được ở đây) |

### B9. 4.2.3 – Sửa tham số

> | GET | /api/getShowtimeSeats?showtimeId= | Public | | 200 ShowtimeSeatView[] |

### B10. 4.2.4 – Sửa 3 dòng

> | GET | /api/getCities | Public | | 200 danh sách thành phố |
>
> | POST | /api/createCinema | ADMIN | {name, address, city, phoneNumber?} | 201 rạp |
>
> | POST | /api/createRoom | ADMIN | {cinemaId, name, totalSeats (>0), roomType} | 201 phòng |

### B11. 4.2.5 – Bảng API đặt vé (thay 2 dòng, thêm 1 dòng)

Thay dòng thống kê bằng:

> | GET | /api/getBookingStats?period= | ADMIN | period = day (mặc định) \| month \| year | 200 {revenue, paidBookings, ticketsSold, period, revenueSeries:[{key, revenue, paidBookings}]} |

Thêm sau `PUT /api/updateBooking`:

> | POST | /api/confirmCashPayment?id= | ADMIN | | 200 BookingResponse (PENDING → PAID) cho vé `CASH` đã thu tiền tại quầy |

Ghi chú dưới bảng:

> `updateBooking` chỉ chấp nhận `PENDING → CANCELLED` (khách hoặc Admin) và
> `PENDING → EXPIRED` (Admin). Trạng thái `PAID` chỉ do listener
> `payment.completed` đặt; gửi `PAID` qua API trả `409`.
> Booking `CASH` không bị scheduler cho hết hạn để khách kịp ra quầy; vé chuyển
> `PAID` khi Admin bấm “Xác nhận tiền mặt”.

### B12. 4.2.6 – Thanh toán

> Hệ thống hỗ trợ đúng hai phương thức: `CASH` (trả tại quầy) và
> `BANK_TRANSFER` (quét VietQR qua SePay). Không hỗ trợ thẻ và ví điện tử.
> Với `BANK_TRANSFER`, booking chỉ chuyển `PAID` sau khi webhook SePay xác nhận
> đúng nội dung và số tiền.

### B13. 4.2.7 – API nội bộ (bảng đúng)

> | GET | /internal/users/{userId}/exists | user | booking kiểm tra user tồn tại |
> | POST | /internal/showtime-seats/hold | movie | giữ ghế (body `SeatIdsRequest`) |
> | POST | /internal/showtime-seats/release | movie | nhả ghế khi hủy/hết hạn |
> | GET | /internal/rooms/{roomId} | venue | lấy phòng chiếu |
> | GET | /internal/rooms/{roomId}/seats | venue | lấy ghế của phòng |
> | GET | /internal/rooms/by-ids?ids= | venue | lấy nhiều phòng |
> | POST | /internal/seats/by-ids | venue | lấy nhiều ghế theo id |
> | GET | /internal/bookings/{bookingId} | booking | payment đọc booking để đối soát |

> Không có endpoint `/internal/showtimes/{id}/exists`: booking-service đọc suất
> chiếu qua `MovieClient` gọi `GET /api/getShowtimeById`. Các service nghiệp vụ
> bind `127.0.0.1` và các đường dẫn `/internal/**` không có route tại Gateway.

### B14. 2.3 – Bảng giao tiếp giữa các service (bảng đúng)

> | Đồng bộ | OpenFeign | booking → user | kiểm tra user tồn tại |
> | Đồng bộ | OpenFeign | booking → movie | đọc suất chiếu, giữ/nhả ghế nội bộ |
> | Đồng bộ | OpenFeign | booking → venue | kiểm tra phòng và lấy ghế theo id |
> | Đồng bộ | OpenFeign | movie → venue | lấy phòng/ghế khi tạo suất chiếu và trả sơ đồ ghế |
> | Đồng bộ | OpenFeign | payment → booking | đọc booking để kiểm tra quyền và đối soát |
> | Bất đồng bộ | RabbitMQ topic `mb.topic.exchange` | payment → booking | routing key `payment.completed`: đặt `PAID`/`CANCELLED`, trả ghế |
> | Bất đồng bộ | RabbitMQ topic `mb.topic.exchange` | payment → notification | cùng routing key: ghi nhận và (dự kiến) gửi vé; hiện chỉ ghi log |

### B15. 5.2.2 – Users (thay bảng)

| Tên thuộc tính | Kiểu dữ liệu | Mô tả | Ràng buộc |
| --- | --- | --- | --- |
| user_id | INT8 | Mã người dùng | PK, Identity |
| full_name | TEXT | Họ tên | Not Null |
| email | TEXT | Email đăng nhập | Not Null, Unique |
| password_hash | TEXT | Mật khẩu băm BCrypt | **Có thể Null** (tài khoản Google) |
| auth_provider | VARCHAR(20) | Nguồn tạo tài khoản | Not Null, Default 'LOCAL', CHECK IN ('LOCAL','GOOGLE') |
| google_id | VARCHAR(255) | Mã tài khoản Google | Có thể Null, Unique |
| phone_number | VARCHAR(12) | Số điện thoại | Có thể Null |
| role | user_role_enum | CUSTOMER, ADMIN | Not Null, Default 'CUSTOMER' |
| created_at / updated_at | TIMESTAMPTZ | Thời điểm tạo/cập nhật | Not Null, Default now() |

### B16. 5.2.2 – Các dòng cần sửa

- `showtime_seats.hold_by`: kiểu **TEXT**, có thể Null, **không có khóa ngoại**;
  service tự kiểm tra và đặt NULL khi nhả ghế.
- `bookings.payment_method`: enum **`payment_method_enum`** chỉ gồm
  `CASH`, `BANK_TRANSFER`, có thể Null.
- `bookings.booking_time`: **Not Null, Default now()**.
- `payments.status`: enum **`payment_status_enum`** gồm
  `PENDING, SUCCESS, FAILED, CANCELLED, EXPIRED`, Not Null, Default 'PENDING'.
- `payments.currency`: **VARCHAR(8)**, Not Null, Default 'VND'.
- `payments.provider`: **VARCHAR(32)**, Not Null, Default 'SEPAY'.
- `payments.provider_txn_id`: unique theo cặp `(provider, provider_txn_id)`.
  Ngoài ra có partial unique index bảo đảm mỗi `booking_id` chỉ có một payment
  `PENDING`.
- `booking_details.price`: NUMERIC, Not Null.
- `booking_details` có **hai khóa ngoại độc lập**:
  `seat_id → seats(seat_id)` và
  `showtime_seat_id → showtime_seats(showtime_seat_id)`; không có composite FK.

### B17. 5.2.2 (m) – Bảng enum đúng

| Kiểu ENUM | Giá trị | Dùng ở |
| --- | --- | --- |
| user_role_enum | CUSTOMER, ADMIN | users.role |
| age_restriction_enum | P, K, T13, T16, T18 | movies.age_restriction |
| movie_status_enum | UPCOMING, SHOWING, ENDED | movies.status |
| room_type_enum | 2D, 3D, IMAX, 4DX | rooms.room_type |
| seat_type_enum | NORMAL, VIP, COUPLE | seats.seat_type |
| showtime_status_enum | SCHEDULED, ONGOING, ENDED, CANCELLED | showtimes.status |
| seat_status_enum | AVAILABLE, HOLDING, BOOKED | showtime_seats.status |
| booking_status_enum | PENDING, PAID, CANCELLED, EXPIRED | bookings.status |
| payment_method_enum | CASH, BANK_TRANSFER | bookings.payment_method |
| payment_status_enum | PENDING, SUCCESS, FAILED, CANCELLED, EXPIRED | payments.status |

> `payments.status` là enum thật; `CANCELLED`/`EXPIRED` do job đối soát
> `PaymentReconciliationJob` ghi khi booking đã huỷ/hết hạn. `bookings.payment_method`
> chỉ còn `CASH` và `BANK_TRANSFER` sau migration
> `002_remove_card_ewallet_payment_methods`.

### B18. 5.2.3 – Quan hệ 11 (viết lại)

> 11. Quan hệ Ghế vật lý – Chi tiết đơn đặt vé (1 – n) và Trạng thái ghế theo
> suất – Chi tiết đơn đặt vé (1 – n)
> - Mô tả: mỗi dòng chi tiết lưu đồng thời `seat_id` (ghế vật lý) và
>   `showtime_seat_id` (dòng trạng thái ghế theo suất) để truy vết và trả ghế.
>   Hai cột này do service ghi theo cùng một ghế; cơ sở dữ liệu chỉ bảo đảm mỗi
>   giá trị tồn tại ở bảng tương ứng bằng hai khóa ngoại độc lập, **không dùng
>   composite FK**.
> - Liên kết: `seats.seat_id (1) ⟷ booking_details.seat_id (n)`;
>   `showtime_seats.showtime_seat_id (1) ⟷ booking_details.showtime_seat_id (n)`.
> - Ràng buộc xóa: NO ACTION.

### B19. 5.3 – Kết luận chương (viết lại đoạn cuối)

> Chương 5 đã trình bày thiết kế logic và cài đặt vật lý cơ sở dữ liệu cho hệ
> thống đặt vé xem phim trên PostgreSQL (Supabase). Mô hình 12 bảng đáp ứng
> nghiệp vụ quản lý phim, rạp, phòng chiếu, ghế, suất chiếu, giữ ghế, đặt vé và
> thanh toán. Các khóa ngoại, ràng buộc unique, CHECK và index được thiết kế để
> bảo đảm toàn vẹn dữ liệu, chống đặt trùng ghế (unique
> `(showtime_id, seat_id)`), chống ghi trùng webhook (unique
> `(provider, provider_txn_id)`) và hỗ trợ các truy vấn chính của hệ thống.

---

## C. Những chỗ cần chụp lại ảnh giao diện

Ảnh 3.2 – 3.11 hiện là giao diện cũ. Cần chụp lại sau khi đã có:

1. Trang chủ (sáng/tối) – không còn mục “Phim” trên menu.
2. Đăng nhập/Đăng ký – có nút Google, không còn khoảng trống.
3. Lịch chiếu – wizard 5 bước (phim → thành phố → rạp → ngày → suất chiếu).
4. Rạp chiếu – bộ lọc thành phố dạng dropdown tuỳ biến.
5. Tài khoản – chỉ sửa họ tên và số điện thoại.
6. Chi tiết phim.
7. Hộp thoại chọn ghế và bước thanh toán (Tiền mặt / Chuyển khoản).
8. Vé của tôi – trạng thái Chờ thanh toán / Đã thanh toán.
9. Khu Quản trị – bảng đặt vé có nút “Xác nhận tiền mặt”, dashboard doanh thu.

---

## D. Việc còn thiếu trong code (nếu muốn báo cáo đúng như đang viết)

Báo cáo đang hứa nhiều hơn sản phẩm. Chọn một trong hai hướng:

- **Sửa báo cáo theo code** (khuyến nghị, đã làm ở mục B): bỏ QR vé, email vé,
  ví điện tử, khóa tài khoản, sơ đồ ghế, trạng thái “đã sử dụng”.
- **Bổ sung code theo báo cáo**: QR soát vé, gửi email thật ở
  `notification-service`, đổi mật khẩu/email tự phục vụ, khóa tài khoản, trạng
  thái hoạt động của rạp, công cụ xếp sơ đồ ghế.

Nếu chỉ có thời gian cho một việc, nên làm **QR soát vé**: vé đã có
`bookingCode`, chỉ cần sinh QR từ mã này ở trang Vé của tôi và thêm màn hình
kiểm tra cho Admin – khi đó mục “vé điện tử có mã QR” trong 1.4 mới đúng.
