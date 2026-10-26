# Movie Ticket Booking System (SOA)

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Các service](#các-service)
- [Cấu trúc repository](#cấu-trúc-repository)
- [Cài đặt & chạy bằng Docker (khuyến nghị)](#cài-đặt--chạy-bằng-docker-khuyến-nghị)
- [Chạy không qua Docker (tuỳ chọn)](#chạy-không-qua-docker-tuỳ-chọn)
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

### Frontend (CineRoom — `frontend-react`)
- **React 19 + Vite** — giao diện đặt vé (phim, lịch chiếu, chọn ghế, thanh toán, vé của tôi)
- Vite dev server port **5173**, proxy `/api/**` về gateway (cấu hình `BACKEND_URL`)

### Hạ tầng
- **Supabase PostgreSQL** — một database dùng chung cho tất cả service
- **RabbitMQ 3.13** (Docker Compose) — topic exchange cho các sự kiện Saga thanh toán

## Kiến trúc tổng quan

```
Trình duyệt
    |
    v
CineRoom frontend :5173  (React + Vite — proxy /api/** về gateway)
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
| user-service | 8084 | User, Auth | CRUD người dùng; đăng ký/đăng nhập JWT (`/api/auth/**`); expose `/internal/users/{id}/exists` |
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
├── frontend-react/         # Giao diện React + Vite (:5173, proxy /api/**)
├── docker-compose.yml      # Cách chạy nhanh: RabbitMQ + 7 service Java + frontend
├── Dockerfile              # Build chung 1 image chứa 7 jar backend
├── frontend-react/Dockerfile  # Build React + Nginx (proxy /api → gateway)
├── run-services.sh         # Chạy 7 service nền
├── stop-services.sh        # Dừng toàn bộ service
├── postman_collection_v3.json  # Bộ kiểm thử E2E qua gateway :8080
└── pom.xml                 # Parent pom
```

## Cài đặt & chạy bằng Docker (khuyến nghị)

Chỉ cần cài **Docker Desktop** (Windows/macOS) hoặc **Docker Engine + Compose plugin** (Linux), kèm internet. Không cần JDK, Maven hay Node — database là Supabase chung của nhóm đã cấu hình sẵn.

```bash
git clone <repo> && cd movie-booking-system
docker compose up -d --build
```

- Lần đầu build khoảng **3–6 phút** (Maven + npm trong Docker), các lần sau dùng cache.
- Mở **http://localhost:5173** để dùng hệ thống.
- Gateway: **http://localhost:8080** · RabbitMQ UI: **http://localhost:15672** (guest/guest).
- Lệnh hữu ích: `docker compose logs -f api-gateway` (xem log) · `docker compose down` (dừng, thêm `-v` để xoá dữ liệu RabbitMQ/Redis).
- Trước khi chạy, tắt service local để không trùng cổng (`./stop-services.sh`, `Ctrl+C` ở `npm run dev`). Cần ~4 GB RAM trống.
- Muốn đổi `JWT_SECRET`/`INTERNAL_TOKEN` (production): tạo file `.env` cạnh `docker-compose.yml` rồi `docker compose up -d` lại — gateway và user-service phải dùng cùng `JWT_SECRET`. Muốn dùng DB khác Supabase, thêm `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` vào `.env`.

## Chạy không qua Docker (tuỳ chọn)

Yêu cầu: **JDK 21 + Maven**, **Node.js ≥ 20.19 (hoặc ≥ 22.12)**, RabbitMQ chạy sẵn (`docker compose up -d rabbitmq`).

```bash
mvn -DskipTests install     # build 8 module backend
./run-services.sh           # chạy nền 7 service, log tại logs/<service>.log; dừng: ./stop-services.sh
cd frontend-react && npm install && npm run dev
```

Đợi ~45 giây rồi mở **http://localhost:5173**. Nếu gateway không ở cổng 8080, tạo `frontend-react/.env.local` với `BACKEND_URL=http://localhost:<cổng>`.

Chạy thành công khi: trang hiển thị phim đang chiếu (có poster), bấm vào phim thấy lịch chiếu, chọn suất thấy sơ đồ ghế.

## Phạm vi chức năng

- [x] CRUD rạp/phòng/ghế (venue) + CRUD phim/thể loại/suất chiếu (movie)
- [x] Tồn kho ghế-theo-suất phát sinh theo tương tác: chọn ghế → `HOLDING` (giữ 10 phút qua `hold_until`), chốt vé → `BOOKED`, quá hạn tự coi như trống
- [x] Đặt vé Saga-lite: giữ/chốt ghế, Booking PENDING, chi tiết vé
- [x] Thanh toán mô phỏng: publish event → Booking `PAID/CANCELLED` + trả ghế (bù trừ); đặt trùng ghế trả **409**
- [x] Giao diện React: duyệt phim, lịch chiếu, chọn ghế, đặt vé, vé của tôi
- [x] Xác thực JWT (đăng ký/đăng nhập, refresh token ở user-service)
- [ ] Cổng thanh toán thật, kiểm thử tự động + OpenAPI

## Nhóm thực hiện

**Nhóm 8** — đồ án môn học.

| STT | Họ và tên | Vai trò |
| ---: | --- | --- |
| 1 | Phạm Văn Thăng | Nhóm trưởng |
| 2 | Lê Quang Hiếu | Thành viên |
| 3 | Vi Tiến Thành | Thành viên |
| 4 | Hoàng Phúc Giáp | Thành viên |
| 5 | Nguyễn Đình Phú | Thành viên |
