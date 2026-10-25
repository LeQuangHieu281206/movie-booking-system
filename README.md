# Movie Ticket Booking System (SOA)

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Các service](#các-service)
- [Cấu trúc repository](#cấu-trúc-repository)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Các bước chạy dự án](#các-bước-chạy-dự-án)
- [Phạm vi chức năng](#phạm-vi-chức-năng)
- [Nhóm thực hiện](#nhóm-thực-hiện)

## Giới thiệu

Hệ thống đặt vé xem phim trực tuyến của **Nhóm 8**, kiến trúc theo hướng dịch vụ (**SOA — Service-Oriented Architecture**): một **API Gateway** duy nhất tiếp nhận request từ frontend và điều hướng đến **7 service backend độc lập** (Spring Boot), liên lạc giữa các service qua **REST đồng bộ (OpenFeign)** và **bất đồng bộ (RabbitMQ)**, lưu dữ liệu trên **Supabase PostgreSQL**.

## Công nghệ sử dụng

### Backend
- **Java 21**, **Maven multi-module**, **Spring Boot 3.5.x** (Web MVC, Validation, Data JPA/Hibernate)
- **Spring Cloud OpenFeign** — giao tiếp đồng bộ giữa các service
- **Spring AMQP (RabbitMQ)** — giao tiếp bất đồng bộ (Saga choreography)
- **Lombok**; thư viện DTO/event/exception dùng chung ở module `common`

### Frontend (CineRoom — `frontend/cine-ticket-frontend`)
- **HTML/CSS/JavaScript thuần** (module ES) — không React, không Vite, **không có package phụ thuộc**
- `server.mjs` (Node ≥ 18): phục vụ file tĩnh + proxy `/api/**` về gateway

### Hạ tầng
- **Supabase PostgreSQL** — một database dùng chung cho tất cả service
- **RabbitMQ 3.13** (Docker Compose) — topic exchange cho các sự kiện Saga thanh toán

## Kiến trúc tổng quan

```
Trình duyệt
    |
    v
CineRoom frontend :5173  (Node server.mjs — file tĩnh + proxy /api/**)
    |
    v
API Gateway :8080  (routes /api/** theo service)
    |
    +-> venue-service :8083   (Cinema / Room / Seat)
    +-> movie-service :8081   (Movie / Genre / Showtime / ShowtimeSeat)
    +-> booking-service :8082 (Booking / BookingDetail)  <-- Feign user/venue/movie
    +-> user-service :8084    (User)
    +-> payment-service :8086 (Thanh toán mô phỏng)      --> RabbitMQ
    +-> notification-service :8087 (Nhận event)          <-- RabbitMQ

RabbitMQ "payment.completed" --> booking-service (PAID/CANCELLED + trả ghế)
                             --> notification-service (gửi vé)
```

Quy ước API: các endpoint dạng **verb-path** dưới gateway `/api/**` (ví dụ `POST /api/createBooking`); endpoint nội bộ `/internal/**` chỉ dành cho giao tiếp giữa các service.

## Các service

| Service | Port | Domain | Trách nhiệm chính |
| --- | ---: | --- | --- |
| api-gateway | 8080 | — | Router `/api/**` đến từng service, CORS cho frontend |
| venue-service | 8083 | Cinema, Room, Seat | CRUD rạp/phòng/ghế; expose `/internal/rooms/**` |
| movie-service | 8081 | Movie, Genre, Showtime, ShowtimeSeat | CRUD phim/suất chiếu; **tồn kho ghế phát sinh theo tương tác** (giữ/nhả/chốt mới sinh bản ghi); giữ/trả/chốt ghế cho booking |
| booking-service | 8082 | Booking, BookingDetail | Saga orchestrator-lite: kiểm user/suất/phòng, chốt ghế, tạo Booking PENDING; consume `payment.completed` |
| user-service | 8084 | User | CRUD người dùng; expose `/internal/users/{id}/exists` |
| payment-service | 8086 | Payment (mô phỏng) | Ghi nhận thanh toán, publish `payment.completed` lên RabbitMQ |
| notification-service | 8087 | Notification | Consume `payment.completed`, gửi vé (hiện log) |

## Cấu trúc repository

```
movie-booking-system/
├── api-gateway/            # Gateway :8080
├── venue-service/          # Cinema/Room/Seat :8083
├── movie-service/          # Movie/Genre/Showtime/ShowtimeSeat :8081
├── booking-service/        # Booking + Saga-lite :8082
├── user-service/           # User :8084
├── payment-service/        # Payment mô phỏng :8086
├── notification-service/   # Notification :8087
├── common/                 # DTO/event/exception dùng chung
├── frontend/cine-ticket-frontend/  # Giao diện CineRoom (vanilla JS + Node server :5173)
├── docker-compose.yml      # RabbitMQ (Redis chỉ dự phòng, không dùng)
├── run-services.sh         # Chạy 7 service nền
├── stop-services.sh        # Dừng toàn bộ service
├── postman_collection_v3.json  # Bộ kiểm thử E2E qua gateway :8080
└── pom.xml                 # Parent pom
```

## Yêu cầu môi trường

- **JDK 21** và **Maven** (kiểm tra bằng `mvn -v`)
- **Node.js ≥ 18** cho frontend — không cần `npm install`
- **Docker** — chỉ cần để bật RabbitMQ
- Database: dùng lại **Supabase chung của nhóm** (đã cấu hình sẵn trong các service); muốn dùng DB riêng thì sửa `spring.datasource.url/username/password` trong 5 file cấu hình (`venue/movie/user`: `application.properties`; `booking/payment/notification/api-gateway`: `application.yml`)

## Các bước chạy dự án

### 1. Bật RabbitMQ
```bash
docker compose up -d rabbitmq   # AMQP :5672, UI :15672 (guest/guest)
```

### 2. Build và chạy backend
```bash
mvn -DskipTests install     # build toàn bộ 8 module
./run-services.sh           # chạy nền 7 service, log tại logs/<service>.log
./stop-services.sh          # dừng toàn bộ khi cần
```
Đợi ~45 giây, script tự kiểm tra các port 8080–8087 đã UP.

### 3. Chạy frontend
```bash
cd frontend/cine-ticket-frontend
node server.mjs             # Windows: bấm đúp CHAY-FRONTEND.bat
```
Mở **http://localhost:5173** (phải chạy backend trước). Biến môi trường tùy chọn: `BACKEND_URL` (mặc định `http://localhost:8080`), `PORT` (mặc định 5173).

Chạy thành công khi: trang hiển thị phim đang chiếu (có poster), bấm vào phim thấy lịch chiếu, chọn suất thấy sơ đồ ghế.

## Phạm vi chức năng

- [x] CRUD rạp/phòng/ghế (venue) + CRUD phim/thể loại/suất chiếu (movie)
- [x] Tồn kho ghế-theo-suất phát sinh theo tương tác: chọn ghế → `HOLDING` (giữ 10 phút qua `hold_until`), chốt vé → `BOOKED`, quá hạn tự coi như trống
- [x] Đặt vé Saga-lite: giữ/chốt ghế, Booking PENDING, chi tiết vé
- [x] Thanh toán mô phỏng: publish event → Booking `PAID/CANCELLED` + trả ghế (bù trừ); đặt trùng ghế trả **409**
- [x] Giao diện CineRoom: duyệt phim, lịch chiếu, chọn ghế, đặt vé, vé của tôi
- [ ] Xác thực/phân quyền, cổng thanh toán thật, kiểm thử tự động + OpenAPI

## Nhóm thực hiện

**Nhóm 8** — đồ án môn học.

| STT | Họ và tên | Vai trò |
| ---: | --- | --- |
| 1 | Phạm Văn Thăng | Nhóm trưởng |
| 2 | Lê Quang Hiếu | Thành viên |
| 3 | Vi Tiến Thành | Thành viên |
| 4 | Hoàng Phúc Giáp | Thành viên |
| 5 | Nguyễn Đình Phú | Thành viên |
