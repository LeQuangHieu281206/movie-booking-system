# Movie Ticket Booking System (SOA)

Hệ thống đặt vé xem phim trực tuyến được phát triển trong khuôn khổ đồ án môn học của **Nhóm 8**, kiến trúc theo hướng dịch vụ (**SOA** — Service-Oriented Architecture): một **API Gateway** duy nhất tiếp nhận request từ frontend và điều hướng đến **7 service backend độc lập** (Spring Boot), liên lạc với nhau qua **REST đồng bộ (OpenFeign)** và **bất đồng bộ (RabbitMQ)**, lưu dữ liệu trên **Supabase PostgreSQL**. Frontend dùng **React + Vite**.

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

### Frontend

- **React 19** + **Vite 8**
- **React Router** để định tuyến
- **Axios** để gọi API (qua tầng `src/services/api.js`, base URL mặc định `http://localhost:8080`)
- **Lucide React** cho icon giao diện

### Hạ tầng

- **Docker Compose** — RabbitMQ (bắt buộc cho event Saga); Redis giữ trong compose làm dự phòng cho TTL hold ghế (Step 5, mặc định tắt)
- **RabbitMQ 3.13** — topic exchange `mb.topic.exchange` cho các event Saga

## Kiến trúc tổng quan

```
Người dùng / Postman
        |
        v
API Gateway :8080  (routes /api/** theo service, CORS frontend)
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
| movie-service | 8081 | Movie, Genre, Showtime, ShowtimeSeat | CRUD phim/thể loại/suất chiếu; tự sinh `showtime_seats` từ venue qua Feign khi tạo suất; giữ/trả/chốt ghế cho booking |
| booking-service | 8082 | Booking, BookingDetail | Saga orchestrator-lite: kiểm user/suất/phòng, hold ghế, tạo Booking PENDING + chi tiết; consume `payment.completed` |
| user-service | 8084 | User | CRUD người dùng; expose `/internal/users/{id}/exists` |
| payment-service | 8086 | Payment (mô phỏng) | Ghi nhận thanh toán, publish `PaymentCompletedEvent` (`payment.completed`) lên RabbitMQ |
| notification-service | 8087 | Notification | Consume `payment.completed`, gửi email vé (hiện log — TODO SMTP) |
| common | — | thư viện chung | DTO event exception dùng chung, không chạy riêng |

## Luồng nghiệp vụ Saga-lite (đặt vé – thanh toán)

1. `POST /api/createBooking` (booking-service): kiểm tra **user** (user-service) → kiểm tra **suất chiếu** (movie-service) → kiểm tra **phòng** (venue-service) → **hold ghế** (movie-service đặt các `showtime_seat` từ `AVAILABLE` sang `BOOKED`) → tạo **Booking PENDING** + BookingDetail
2. `POST /api/createPayment` (payment-service): xử lý mô phỏng → publish **`payment.completed`** (SUCCESS/FAILED)
3. booking-service nhận event:
    - **SUCCESS** → Booking `PENDING → PAID`
    - **FAILED** → Booking `PENDING → CANCELLED` **+ trả ghế về AVAILABLE** (bù trừ)
    - Event trễ/FAILED khi booking đã PAID được bỏ qua (idempotent)
4. notification-service nhận event SUCCESS → gửi vé (hiện log)

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
├── frontend/               # React 19 + Vite 8 (Step 0)
├── logs/                   # Log khi chạy bằng run-services.sh (bỏ qua commit)
├── docker-compose.yml      # RabbitMQ (dùng thật) + Redis (dự phòng TTL hold — Step 5)
├── run-services.sh         # Chạy 7 service Spring Boot (mvn spring-boot:run background)
├── stop-services.sh        # Dừng toàn bộ service
├── postman_collection.json # Smoke-test toàn luồng qua gateway :8080
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

- **JDK 21** và **Maven** (hoặc dùng Maven cài sẵn trên máy)
- **Docker + Docker Compose** (RabbitMQ/Redis/Postgres local)
- **Node.js + npm** tương thích `frontend/package.json` (Vite 8)
- Tài khoản **Supabase** (hoặc PostgreSQL local) cho database dùng chung
- Optional: **Postman** để chạy bộ kiểm thử

## Cài đặt và chạy dự án

### 1. Chuẩn bị hạ tầng

```bash
docker compose up -d rabbitmq   # RabbitMQ :5672 (UI :15672)
```

`docker-compose.yml` hiện chỉ còn **RabbitMQ** (bắt buộc) và **Redis** (dự phòng cho TTL hold ghế ở Step 5 — mặc định tắt, cần thì `docker compose up -d redis`). Database PostgreSQL cục bộ đã bị xoá khỏi compose vì nhóm dùng chung Supabase (xem mục [Cơ sở dữ liệu](#cơ-sở-dữ-liệu-và-biến-môi-trường)).

### 2. Cấu hình kết nối Supabase

Mỗi service có file cấu hình riêng:

- `venue-service`, `movie-service`, `user-service`: `src/main/resources/application.properties`
- `booking-service`: `src/main/resources/application.yml`

Cấu hình khuyến nghị là chuyển thông tin kết nối sang biến môi trường thay vì lưu mật khẩu trong repository:

```
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Với Supabase, lấy host/port/user/database và tham số SSL từ dashboard (direct connection hoặc connection pooler). **Không commit mật khẩu/token thật.**

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

### 4. Chạy frontend

```
cd frontend
npm install
npm run dev
```

Frontend mặc định gọi gateway `http://localhost:8080` (override bằng `VITE_API_BASE_URL` trong `frontend/.env` nếu cần).

## Kiểm thử bằng Postman

Import `postman_collection.json` (base url biến `{{base}}` = `http://localhost:8080`). Bộ collection bao phủ toàn luồng qua gateway:

1. **venue** — tạo rạp → phòng → ghế → truy vấn theo thành phố/id
2. **movie** — thể loại (tự tái sử dụng genre trùng thay vì báo 400) → phim → suất chiếu (tự sinh ghế-theo-suất) → sơ đồ ghế
3. **user** — tạo user
4. **booking** — 2 lần đặt **trùng ghế** (lần 2 kỳ vọng 409) → huỷ booking (trả ghế)
5. **payment** — thanh toán SUCCESS/FAILED qua RabbitMQ → verify `getBookingById` trạng thái `PAID`/`CANCELLED`

Collection chính các id động (`cinemaId`, `roomId`, `seatId`, `showtimeId`, `userId`, `bookingId`, `seatIds`) giữa các request qua collection variables.

## Phạm vi chức năng

- [x] CRUD rạp/phòng/ghế + truy vấn theo thành phố
- [x] CRUD phim/thể loại + tìm kiếm/lọc phim (status, genre, từ khoá)
- [x] Tạo suất chiếu + tự sinh sơ đồ ghế-theo-suất (`showtime_seats`)
- [x] Sơ đồ ghế theo suất (AVAILABLE/BOOKED)
- [x] Đặt vé Saga-lite: hold ghế, Booking PENDING, chi tiết vé
- [x] Thanh toán mô phỏng (REST + publish event qua RabbitMQ)
- [x] Cập nhật trạng thái vé theo event (PAID/CANCELLED) + trả ghế cho bù trừ
- [x] Thông báo nhận event (log — phát triển tiếp SMTP)
- [ ] Giao diện frontend hoàn chỉnh (duyệt phim, chọn ghế, đặt vé)
- [ ] Transactional outbox + idempotency chi tiết cho event Saga (vẫn trên DB Supabase dùng chung)
- [ ] Xác thực/phân quyền (Spring Security/JWT)
- [ ] TTL hold ghế (Redis) + worker dọn ghế hết hạn
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
| `VITE_API_BASE_URL` | Base URL gateway cho frontend (mặc định `http://localhost:8080`) |

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
