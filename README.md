# Movie Ticket Booking System (SOA)

Hệ thống đặt vé xem phim trực tuyến được phát triển trong khuôn khổ đồ án môn học của **Nhóm 8**, kiến trúc theo hướng dịch vụ: một **API Gateway** duy nhất tiếp nhận request từ frontend và điều hướng đến **7 service backend độc lập**, liên lạc với nhau qua **REST đồng bộ (OpenFeign)** và **bất đồng bộ (RabbitMQ)**, lưu dữ liệu trên **Supabase PostgreSQL**. Frontend **CineRoom** dùng HTML/CSS/JavaScript thuần chạy trên server Node proxy về gateway.

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Các service](#các-service)
- [Luồng nghiệp vụ Saga-lite (đặt vé – thanh toán)](#luồng-nghiệp-vụ-saga-lite-đặt-vé--thanh-toán)
- [Cấu trúc repository](#cấu-trúc-repository)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cài đặt và chạy dự án](#cài-đặt-và-chạy-dự-án)
- [Kiểm thử bằng Postman](#kiểm-thử-bằng-postman)
- [Phạm vi chức năng](#phạm-vi-chức-năng)
- [Cơ sở dữ liệu và biến môi trường](#cơ-sở-dữ-liệu-và-biến-môi-trường)
- [Ghi chú Lombok / NetBeans](#ghi-chú-lombok--netbeans)
- [Nhóm thực hiện](#nhóm-thực-hiện)

## Giới thiệu

Hệ thống hỗ trợ tra cứu phim, xem lịch chiếu, chọn ghế theo sơ đồ từng suất chiếu, đặt vé và thanh toán (mô phỏng), kèm phân tách nghiệp vụ thành các bounded context:

- **venue** — rạp (Cinema), phòng chiếu (Room), ghế vật lý (Seat)
- **movie** — phim (Movie), thể loại (Genre), suất chiếu (Showtime) và **ghế-theo-suất** (ShowtimeSeat — seat inventory)
- **user** — tài khoản người dùng
- **booking** — đơn đặt vé, chi tiết vé
- **payment** — cổng thanh toán mô phỏng
- **notification** — nhận event thanh toán, gửi thông báo/email
- **api-gateway** — điểm vào duy nhất cho frontend, router + CORS
- **frontend/cine-ticket-frontend** — giao diện web **CineRoom** (vanilla JS) chạy server Node :5173, proxy `/api` về gateway

Dự án được tổ chức theo mô hình **monorepo**, chứa toàn bộ backend, frontend, hạ tầng Docker và tài liệu trong cùng một repository.

Repository: [lequanghieu-haui/movie-booking-system](https://github.com/lequanghieu-haui/movie-booking-system)

## Công nghệ sử dụng

### Backend

- **Java 21**, **Maven multi-module**
- **Spring Boot 3.5.x** — Web MVC cho API, Validation, Data JPA/Hibernate
- **Spring Cloud OpenFeign** — giao tiếp đồng bộ giữa các service
- **Spring AMQP (RabbitMQ)** — giao tiếp bất đồng bộ (Saga choreography)
- **PostgreSQL** qua JDBC — **một database Supabase Postgres dùng chung cho các service** (quyết định của nhóm; tham chiếu giữa service là ID thuần, tính toàn vẹn đảm bảo ở tầng ứng dụng qua Feign)
- **Lombok**
- **common**

### Frontend (CineRoom — `frontend/cine-ticket-frontend`)

- **HTML/CSS/JavaScript thuần**

### Hạ tầng

- **Docker Compose** — RabbitMQ
- **RabbitMQ 3.13** — topic exchange `mb.topic.exchange` cho các event Saga

## Kiến trúc tổng quan

```
Trình duyệt / Postman
        |
        v
CineRoom frontend :5173  (Node server.mjs — file tĩnh + proxy /api/** về gateway)
        |
        v
API Gateway :8080  (routes /api/** theo service, CORS cho frontend)
        |
        +-> venue-service :8083   (Cinema / Room / Seat)
        |     ^  Feign /internal/rooms/{id}, /internal/rooms/{id}/seats
        +-> movie-service :8081   (Movie / Genre / Showtime / ShowtimeSeat)
        |     ^  Feign /internal/showtimes/{id}/exists
        |     v  Feign hold/release/confirm /internal/showtime-seats/**
        +-> booking-service :8082 (Booking / BookingDetail)
        |     ^  Feign user-exists, showtime-by-id, room-by-id
        +-> user-service :8084    (User / Auth sau này)
        +-> payment-service :8086 (cổng thanh toán mô phỏng) --> RabbitMQ
        +-> notification-service :8087 --> RabbitMQ (q.payment-completed.notification)

        RabbitMQ  mb.topic.exchange
            "payment.completed" --> booking-service (PAID / CANCELLED + trả ghế)
                                  --> notification-service (gửi vé)
```

Quy ước API: các endpoint dạng **verb-path** dưới gateway `/api/**`, ví dụ `POST /api/createCinema`, `GET /api/getShowtimes?movieId=...`. Endpoint nội bộ `/internal/**` chỉ dành cho giao tiếp giữa các service (không route qua gateway), và exception chung trả về theo `ErrorResponse` (404/409/400/500).

## Các service

| Service | Port | Domain | Trách nhiệm chính |
| --- | ---: | --- | --- |
| api-gateway | 8080 | — | Router `/api/**` đến từng service, CORS cho `http://localhost:*` |
| venue-service | 8083 | Cinema, Room, Seat | CRUD rạp/phòng/ghế; expose `/internal/rooms/**` cho movie/booking |
| movie-service | 8081 | Movie, Genre, Showtime, ShowtimeSeat | CRUD phim/thể loại/suất chiếu; **tồn kho ghế phát sinh theo tương tác** — tạo suất không chèn ghế, giữ/nhả/chốt mới sinh bản ghi `showtime_seats` (native SQL 1 lượt/truy vấn); giữ/trả/chốt ghế cho booking |
| booking-service | 8082 | Booking, BookingDetail | Saga orchestrator-lite: kiểm user/suất/phòng, hold ghế, tạo Booking PENDING + chi tiết; consume `payment.completed` |
| user-service | 8084 | User | CRUD người dùng; expose `/internal/users/{id}/exists` |
| payment-service | 8086 | Payment (mô phỏng) | Ghi nhận thanh toán, publish `PaymentCompletedEvent` (`payment.completed`) lên RabbitMQ |
| notification-service | 8087 | Notification | Consume `payment.completed`, gửi email vé (hiện log — TODO SMTP) |
| common | — | thư viện chung | DTO event exception dùng chung, không chạy riêng |

## Luồng nghiệp vụ Saga-lite (đặt vé – thanh toán)

1. `POST /api/createBooking` (booking-service): kiểm tra **user** (user-service) → kiểm tra **suất chiếu** (movie-service) → kiểm tra **phòng** (venue-service) → **chốt ghế** (movie-service upsert `showtime_seats` sang `BOOKED` bằng 1 truy vấn; ghế đang `HOLDING` của cùng khách tự chuyển thành `BOOKED`) → tạo **Booking PENDING** + BookingDetail
2. `POST /api/createPayment` (payment-service): xử lý mô phỏng → publish **`payment.completed`** (SUCCESS/FAILED)
3. booking-service nhận event:
    - **SUCCESS** → Booking `PENDING → PAID`
    - **FAILED** → Booking `PENDING → CANCELLED` **+ trả ghế về AVAILABLE** (bù trừ)
    - Event trễ/FAILED khi booking đã PAID được bỏ qua (idempotent)
4. notification-service nhận event SUCCESS → gửi vé (hiện log)

**Mô hình ghế-theo-suất (lazy inventory)**: tạo suất chiếu **không** sinh sẵn ghế — bảng `showtime_seats` chỉ sinh bản ghi khi có thao tác: khách bấm chọn ghế → `HOLDING` với `hold_until` = +10 phút (`POST /api/holdShowtimeSeat`); bỏ chọn → xoá bản ghi (`POST /api/releaseShowtimeSeat`); chốt vé → `BOOKED`. Ghế giữ quá hạn được lọc coi như `AVAILABLE` ngay tại truy vấn — không cần worker hay Redis. Ghế trống của một suất bằng tổng ghế vật lý của phòng (truy vấn venue-service) trừ các bản ghi còn hiệu lực.

Bù trừ trực tiếp: `PUT /api/updateBooking` chuyển `CANCELLED`/`EXPIRED` cũng trả ghế về `AVAILABLE`. Đặt trùng ghế đang `BOOKED` trả về **409 CONFLICT**.

## Cấu trúc repository

```
movie-booking-system/
├── api-gateway/            # Gateway :8080 (Spring Cloud Gateway, WebFlux)
├── booking-service/        # Booking + Saga-lite listener :8082
├── venue-service/          # Cinema/Room/Seat :8083
├── movie-service/          # Movie/Genre/Showtime/ShowtimeSeat :8081
├── user-service/           # User :8084
├── payment-service/        # Payment mô phỏng :8086 (publish RabbitMQ)
├── notification-service/   # Notification :8087 (consume RabbitMQ)
├── common/                 # DTO/event/exception dùng chung (com.ttcsn.common)
├── frontend/                   # Giao diện web
│   └── cine-ticket-frontend/   # "CineRoom" — vanilla JS + Node server.mjs :5173 (không cần npm install)
├── frontend-/                  # Bản mẫu React/Vite cũ từ ZIP ban đầu — chỉ để tham khảo, không dùng để chạy
├── logs/                   # Log khi chạy bằng run-services.sh (bỏ qua commit)
├── docker-compose.yml      # RabbitMQ (dùng cho Saga) + Redis (chỉ dự phòng, không dùng)
├── run-services.sh         # Chạy 7 service Spring Boot (mvn spring-boot:run background)
├── stop-services.sh        # Dừng toàn bộ service
├── postman_collection_v3.json # Bộ assert E2E toàn luồng (chạy bằng Collection Runner qua gateway :8080)
├── SOA_REFACTOR_PLAN.md    # Lộ trình tách service chi tiết
├── pom.xml                 # Parent pom (movie-booking-parent)
└── README.md
```

Cấu trúc bên trong một service (ví dụ `movie-service`):

```
movie-service/src/main/java/com/ttcsn/movie/
├── client/        # Feign client (VenueClient)
├── controller/    # MovieController, GenreController
├── dto/           # request/ response/
├── entity/        # Movie, Genre (+ enums)
├── exception/     # NotFound tùy domain kế thừa common.NotFoundException
├── repository/    # Spring Data JPA
├── showtime/      # bounded context Showtime: controller/entity/service/repository
├── service/       # interface + impl/
└── specifications # JPA Specification cho tìm kiếm phim
```

## Yêu cầu môi trường

- **JDK 21** và **Maven** để build và chạy backend
- **Node.js ≥ 18** cho frontend
- **Docker** chỉ cần để bật RabbitMQ (`docker compose up -d rabbitmq`)
- **Database PostgreSQL** — người mới có thể dùng ngay Supabase chung với thông tin kết nối đã cấu hình sẵn (xem mục [Cơ sở dữ liệu](#cơ-sở-dữ-liệu-và-biến-môi-trường))
- Optional: **Postman** để chạy bộ kiểm thử E2E

## Cài đặt và chạy dự án

### 1. Chuẩn bị hạ tầng

```bash
docker compose up -d rabbitmq   # RabbitMQ :5672 (UI :15672)
```

`docker-compose.yml` hiện chỉ còn **RabbitMQ** (dùng cho sự kiện Saga thanh toán) và **Redis** (chỉ là dự phòng, không service nào dùng — TTL giữ ghế làm bằng cột `hold_until` trên Postgres). Database PostgreSQL cục bộ đã bị xoá khỏi compose vì nhóm dùng chung Supabase (xem mục [Cơ sở dữ liệu](#cơ-sở-dữ-liệu-và-biến-môi-trường)).

### 2. Cấu hình kết nối Supabase

Mỗi service có file cấu hình riêng:

- `venue-service`, `movie-service`, `user-service`: `src/main/resources/application.properties`
- `booking-service`, `payment-service`, `notification-service`, `api-gateway`: `src/main/resources/application.yml`

**Hiện trạng**: các file trên đã chứa thông tin kết nối tới **Supabase chung của nhóm** — người mới chạy theo các bước trên là thấy ngay dữ liệu demo (khoảng 50 phim, 12.000 ghế vật lý, 84.600 suất chiếu; `showtime_seats` sẽ sinh dần theo thao tác). Khuyến nghị lâu dài là chuyển sang biến môi trường:

```
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Nếu dùng **database riêng** (Postgres local hoặc Supabase dự án khác): sửa thẳng `spring.datasource.url/username/password` trong 5 file trên — ví dụ Postgres local `docker run -d --name movie-db -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:16-alpine` với URL `jdbc:postgresql://localhost:5432/postgres?sslmode=disable`. Schema tự sinh nhờ Hibernate `ddl-auto=update` — **không cần chạy SQL seed**; tạo dữ liệu bằng bộ Postman ở mục trên, sau đó giao diện CineRoom phát sinh ghế khi khách thao tác. **Không commit mật khẩu/token thật.**

### 3. Build và chạy backend

```
mvn -DskipTests install     # build toàn bộ 8 module
./run-services.sh           # chạy 7 service nền, log tại logs/<service>.log
./stop-services.sh          # dừng toàn bộ service (giữ Docker)
```

Nếu chạy từng service thủ công:

```
mvn -q -pl movie-service -DskipTests spring-boot:run
```

Kiểm tra trạng thái port (8080–8087) ví dụ bằng `ss -ltn` sau khi chạy \~45 giây.

### 4. Chạy frontend (CineRoom)

```
cd frontend/cine-ticket-frontend
node server.mjs       # Windows: bấm đúp CHAY-FRONTEND.bat
```

Mở **http://localhost:5173** — phải chạy backend trước, vì server này chỉ phục vụ giao diện và proxy `/api/**` về gateway. Biến môi trường tùy chọn: `BACKEND_URL` (mặc định `http://localhost:8080`) và `PORT` (mặc định 5173). Chi tiết đầy đủ ở `frontend/cine-ticket-frontend/HUONG-DAN.md`.

Chạy thành công khi: trang phim hiển thị phim đang chiếu (có poster), bấm vào phim thấy lịch chiếu, chọn suất thấy sơ đồ ghế.

## Kiểm thử bằng Postman

Import `postman_collection_v3.json` (base url biến `{{base}}` = `http://localhost:8080`). Bộ collection bao phủ toàn luồng qua gateway, chạy bằng **Collection Runner** để giữ đúng thứ tự assertion:

1. **venue** — tạo rạp → phòng → ghế → truy vấn theo thành phố/id
2. **movie** — thể loại (tự tái sử dụng genre trùng thay vì báo 400) → phim → suất chiếu → sơ đồ ghế (bản ghi `showtime_seats` chỉ xuất hiện khi đã có thao tác giữ/đặt)
3. **user** — tạo user
4. **booking** — 2 lần đặt **trùng ghế** (lần 2 kỳ vọng 409) → huỷ booking (trả ghế)
5. **payment** — thanh toán SUCCESS/FAILED qua RabbitMQ → verify `getBookingById` trạng thái `PAID`/`CANCELLED`

Collection tự nối các id động (`cinemaId`, `roomId`, `seatId`, `showtimeId`, `userId`, `bookingId`, `seatIds`) giữa các request qua collection variables, chạy lại được nhiều lần mà không cần sửa tay.

## Phạm vi chức năng

- [x] CRUD rạp/phòng/ghế + truy vấn theo thành phố
- [x] CRUD phim/thể loại + tìm kiếm/lọc phim (status, genre, từ khoá)
- [x] Tạo suất chiếu (tồn kho ghế phát sinh theo tương tác — tạo suất không chèn sẵn ghế)
- [x] Sơ đồ ghế theo suất: `AVAILABLE` suy ra từ ghế vật lý + `HOLDING` (giữ 10 phút qua `hold_until`) + `BOOKED`
- [x] Đặt vé Saga-lite: hold ghế, Booking PENDING, chi tiết vé
- [x] Thanh toán mô phỏng (REST + publish event qua RabbitMQ)
- [x] Cập nhật trạng thái vé theo event (PAID/CANCELLED) + trả ghế cho bù trừ
- [x] Thông báo nhận event (log — phát triển tiếp SMTP)
- [x] Giao diện CineRoom: duyệt phim, lịch chiếu, chọn ghế, đặt vé, vé của tôi (`frontend/cine-ticket-frontend`)
- [ ] Transactional outbox + idempotency chi tiết cho event Saga (vẫn trên DB Supabase dùng chung)
- [ ] Xác thực/phân quyền (Spring Security/JWT)
- [x] TTL giữ ghế 10 phút (cột `hold_until`; hết hạn tự coi như trống tại truy vấn — không cần Redis/worker)
- [ ] Cổng thanh toán thật (VNPay/MoMo/Stripe)
- [ ] Kiểm thử tự động + tài liệu OpenAPI

## Cơ sở dữ liệu và biến môi trường

**Quyết định của nhóm: tất cả service dùng chung một database Supabase PostgreSQL** (không tách DB-per-service). Lý do: dữ liệu demo đã có sẵn tập trung trên Supabase, mọi thành viên nhóm và máy demo đều nhìn thấy cùng một dữ liệu khi trình bày — không phải seed dữ liệu riêng trên từng máy cá nhân. Mỗi service chỉ "sở hữu" bảng của mình (`users` — user-service; `movies`, `genres`, `movie_genres`, `showtimes`, `showtime_seats` — movie-service; `cinemas`, `rooms`, `seats` — venue-service; `bookings`, `booking_details` — booking-service), schema do Hibernate `ddl-auto=update` quản lý. Tham chiếu giữa các service là **ID thuần**, tính toàn vẹn đảm bảo ở tầng ứng dụng qua Feign — vì vậy việc dùng chung DB không phá kiến trúc SOA.

Lưu ý với DB dùng chung: xoá rạp (`deleteCinema`) sẽ cascade xoá phòng/ghế ở DB — các `showtimes` đang tham chiếu `room_id` đã xoá sẽ trở thành dữ liệu treo, hãy xoá suất chiếu liên quan trước (hoặc chỉ xoá dữ liệu test qua mục `99 · deleteCinema (cleanup)` trong collection Postman).

| Biến | Ý nghĩa |
| --- | --- |
| `DB_URL` | JDBC URL PostgreSQL (Supabase pooler hoặc Postgres local) |
| `DB_USERNAME` | Username PostgreSQL |
| `DB_PASSWORD` | Mật khẩu PostgreSQL |
| `BACKEND_URL` | Gateway cho server frontend Node (mặc định `http://localhost:8080`) |
| `PORT` | Cổng server frontend (mặc định `5173`) |

Lưu ý: xoá rạp (`deleteCinema`) hiện tại sẽ cascade xoá phòng/ghế tương ứng ở DB, và các pha xóa dọn dữ liệu test được đặt cuối collection Postman (mục `99 · deleteCinema (cleanup)`).

## Ghi chú Lombok / NetBeans

- Lombok được khai báo ở từng service và pin ở pom cha qua `<lombok.version>`; `annotationProcessorPaths` đã cấu hình trong `<plugins>` của pom cha để **Maven CLI lẫn NetBeans** đều nhận diện được processor.
- Khi mở bằng NetBeans 26+, sau khi clone/nhận pom mới hãy **Close/Reload Project** để IDE re-enroll Lombok; nếu vẫn còn gạch đỏ hãy chạy lại `mvn clean install` rồi Clean and Build trong IDE.

## Nhóm thực hiện

**Nhóm 8** — đồ án môn học.

| STT | Họ và tên | Vai trò |
| ---: | --- | --- |
| 1 | Phạm Văn Thăng | Nhóm trưởng |
| 2 | Lê Quang Hiếu | Thành viên |
| 3 | Vi Tiến Thành | Thành viên |
| 4 | Hoàng Phúc Giáp | Thành viên |
| 5 | Nguyễn Đình Phú | Thành viên |

Repository: [lequanghieu-haui/movie-booking-system](https://github.com/lequanghieu-haui/movie-booking-system)

---

> README này được viết dựa trên cấu hình có thể kiểm tra trong mã nguồn tại thời điểm soạn (Spring Boot 3.5.x, Spring Cloud 2025.0.0, các port/endpoint ở trên). Khi thêm service, đổi port hoặc thêm endpoint mới, hãy cập nhật cả bảng [Các service](#các-service), sơ đồ kiến trúc và danh sách endpoint của gateway trong `api-gateway/src/main/resources/application.yml`.
