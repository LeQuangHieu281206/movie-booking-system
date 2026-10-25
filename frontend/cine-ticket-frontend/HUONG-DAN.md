# CineRoom — frontend đặt vé xem phim

Bản frontend này nối vào backend microservices trong repo **movie-booking-system**: api-gateway `http://localhost:8080` chuyển tiếp đến movie-service, venue-service, user-service và booking-service, dữ liệu nằm trong Supabase PostgreSQL. Không dùng Eureka hoặc Docker để chạy riêng frontend này.

## 1. Xem giao diện — 3 bước

1. Backend microservices phải đang chạy (api-gateway `http://localhost:8080` và các service con, dữ liệu Supabase).
2. Mở thư mục `cine-ticket-frontend`, bấm đúp **CHAY-FRONTEND.bat** (hoặc chạy `node server.mjs`).
3. Truy cập **http://localhost:5173**. Giữ cửa sổ đang chạy mở.

Máy cần có **Node.js 18 trở lên**. Không cần `npm install`. Nếu cửa sổ báo thiếu Node.js thì cần cài Node.js trước. Frontend này không yêu cầu đổi phiên bản Java.

Nếu không dùng file BAT: mở Terminal ngay trong thư mục chứa `server.mjs` rồi nhập đúng **một dòng**:

```powershell
node server.mjs
```

Website luôn dùng **backend thật**: phim, rạp, suất chiếu và vé đọc trực tiếp từ Supabase qua api-gateway. Nếu backend chưa chạy, trang báo lỗi kết nối thay vì hiện dữ liệu giả.

Luồng thử: chọn một phim đang chiếu → chọn suất → chọn ghế → Tiếp tục đặt vé → tạo hồ sơ → xác nhận → Vé của tôi → xem/in phiếu.

## 2. Kết nối backend thật

Frontend gốc trong ZIP là mẫu React/Vite chưa hoàn thiện, thiếu `App.css` và tham chiếu logo chưa khai báo. Bản thay thế này dùng HTML/CSS/JavaScript thuần, có chia module để dễ học và sửa. Nếu báo cáo của nhóm bắt buộc React, cần cập nhật lựa chọn công nghệ hoặc chuyển giao diện sang React; đây không phải bản React.

### Bước A — backend đã có đủ API

Không cần chép thêm file Java nào. Backend microservices hiện có sẵn các endpoint frontend cần, đọc dữ liệu trực tiếp từ Supabase:

- `GET /api/getMovies`, `GET /api/getGenres`, `GET /api/getCinemas`: danh sách phim, thể loại, rạp.
- `GET /api/getShowtimes`, `GET /api/getShowtimeById`: thông tin suất chiếu (movie-service).
- `GET /api/getShowtimeSeats?showtimeId=...`: ghế theo suất (`showtimeSeatId`, `seatId`, trạng thái) từ bảng `showtime_seats` (movie-service); frontend ghép thêm hàng/số/loại ghế từ venue-service qua `GET /api/getSeatsByRoomId?roomId=...`.
- `POST /api/createBooking`: booking-service giữ ghế qua movie-service (internal hold/confirm), tạo đơn `PENDING`, thanh toán `CASH` tại quầy; `totalAmount` do frontend gửi theo `basePrice × số ghế`.

Các ghế của đơn chờ thanh toán được giữ/đánh dấu qua movie-service; hủy hoặc hết hạn đơn sẽ trả ghế.

### Bước B — có dữ liệu hợp lệ

Database cần có phim, thể loại, rạp, phòng, ghế vật lý, suất chiếu tương lai có trạng thái `SCHEDULED`, giá > 0 và dữ liệu `showtime_seats`.

Nếu thấy sơ đồ ghế trống dù phòng đã có ghế: kiểm tra bảng `showtime_seats` trong Supabase có đủ bản ghi cho từng suất chiếu (một bản ghi cho mỗi ghế vật lý của phòng, trạng thái `AVAILABLE`). Không có kết nối hay thao tác database tự động trong gói frontend.

### Bước C — kiểm tra kết nối trên giao diện

1. Backend đang chạy ở `http://localhost:8080`.
2. Chạy frontend như mục 1.
3. Bấm nút **Backend thật** ở thanh trên cùng, rồi bấm **Kiểm tra API phim**.

Frontend gửi yêu cầu đến `/api/...` trên server Node; server chuyển tiếp về backend để tránh vướng CORS giữa hai cổng khi chạy local. Không cần mở CORS `*` cho các controller.

Nếu backend dùng cổng khác, mở PowerShell tại thư mục frontend và nhập **mỗi dòng riêng**, nhấn Enter sau từng dòng:

```powershell
$env:BACKEND_URL = 'http://localhost:8081'
```

```powershell
node server.mjs
```

Nếu nhập lệnh mà PowerShell hiện `>>`, nhấn **Ctrl+C** rồi nhập lại nguyên một dòng, kiểm tra đủ dấu nháy. Không sao chép dấu ``` vào Terminal.

Nếu cổng frontend 5173 bận, dừng cửa sổ frontend cũ bằng Ctrl+C; hoặc đặt PORT rồi chạy lại:

```powershell
$env:PORT = '5174'
```

```powershell
node server.mjs
```

Khi đổi PORT, tự mở `http://localhost:5174`; file BAT mặc định vẫn mở cổng 5173.

## 3. Chức năng đã làm

| Màn hình | Chức năng |
| --- | --- |
| Trang phim | Banner, phim đang/sắp chiếu, tìm tên không dấu, lọc thể loại |
| Chi tiết phim | Poster, nội dung, đạo diễn, diễn viên, giới hạn tuổi, trailer nếu API có URL |
| Lịch chiếu | Lọc phim, ngày, rạp; chỉ nhận đặt suất tương lai SCHEDULED; giờ Việt Nam |
| Rạp chiếu | Danh sách rạp, địa chỉ, thành phố, số điện thoại |
| Chọn ghế | Sơ đồ hàng ghế, trạng thái trống/đã đặt/đang giữ, VIP, giới hạn 8 ghế, tổng tiền |
| Hồ sơ | Tạo hồ sơ khách hàng qua createUser hoặc dùng mã hồ sơ đã có |
| Xác nhận đặt | Kiểm tra lại trạng thái ghế, gọi API, hiển thị lỗi cụ thể |
| Vé của tôi | Lấy lịch sử theo hồ sơ, xem mã vé, trạng thái, tổng tiền, in/lưu PDF bằng trình duyệt |
| Kết nối | Kiểm tra API phim ngay trên giao diện; lỗi backend hiển thị rõ |

Bố cục có CSS cho điện thoại, tablet và máy tính; hỗ trợ focus bàn phím, nhãn biểu mẫu, thông báo lỗi, giảm chuyển động theo tùy chọn hệ thống. Tất cả ảnh mẫu là SVG đi kèm, không cần tải ảnh/font ngoài.

## 4. Giới hạn cần biết để trình bày đúng với giáo viên

- **Chưa có đăng nhập/xác thực/phân quyền** vì backend gốc chưa có API này. Mã khách hàng và hồ sơ lưu trong phiên không phải cơ chế xác thực. Bản này dành cho thử nghiệm local/đồ án, không phải hệ thống đã sẵn sàng đưa cho khách dùng công khai.
- Backend gốc lưu `request.password()` trực tiếp vào `passwordHash`; form ghi rõ chỉ dùng mật khẩu thử nghiệm. Frontend không lưu mật khẩu và không giả vờ tạo phiên đăng nhập.
- **Thanh toán tại quầy**. Không có VNPay/MoMo/Stripe, không có giao dịch thật hoặc QR thanh toán. Chưa có quản trị, xác nhận thanh toán, hủy vé hay tự hết hạn đơn trong frontend này.
- API lịch sử gốc chỉ trả booking, không trả tên phim/ghế. Các vé vừa đặt trên trình duyệt này có thông tin đầy đủ lưu cục bộ. Đổi máy/xóa dữ liệu trình duyệt vẫn xem được booking từ API nhưng không có chi tiết ghế; cần thêm API chi tiết vé để khắc phục.
- Chưa có phụ thu loại ghế; mọi ghế dùng `basePrice` của suất chiếu. Thay đổi chính sách giá cần cập nhật cả frontend và backend.
- Ghế được giữ/đặt qua internal API của movie-service khi booking-service tạo đơn; một ghế chỉ thành công cho một đơn trong cùng thời điểm. Hủy hoặc hết hạn đơn trả ghế về trạng thái trống. `totalAmount` do frontend gửi phải bằng `basePrice × số ghế`.
- Các API quản lý backend gốc chưa được bảo vệ. Server Node chỉ lắng nghe ở `127.0.0.1`; không mở frontend/proxy này ra Internet trước khi hoàn thiện xác thực và phân quyền backend.

## 5. Cấu trúc để học và chỉnh sửa

```text
cine-ticket-frontend/
  index.html             Khung trang, header, footer, hộp thoại
  src/
    styles.css           Màu sắc và bố cục responsive
    app.js               Render màn hình, xử lý bấm nút và form
    domain.mjs           Lọc phim, thời gian, chọn ghế, kiểm tra đầu vào
    api.js               Gọi API thật, lưu hồ sơ/chi tiết vé
  assets/                Poster SVG tự tạo, favicon
  server.mjs             Server local và proxy backend
  CHAY-FRONTEND.bat      Chạy nhanh trên Windows
```

Sửa màu chính: biến `--gold`, `--bg`, `--panel` đầu file `src/styles.css`. Sửa tiêu đề/thương hiệu: `index.html` và hàm `header()` trong `app.js`. Phim và poster lấy từ database Supabase qua backend, không có dữ liệu cứng trong frontend.

Lưu file rồi tải lại trang để thấy thay đổi. Không có bước build, không có package phụ thuộc, không cần Vite. Có thể giữ nguyên backend hiện tại và đặt frontend này cạnh nó; không cần xóa thư mục frontend cũ trước khi thử.

## 6. Kiểm tra

Bộ kiểm tra tự động (thư mục `tests/`) đã được gỡ khi hợp nhất repo. Kiểm tra bằng luồng thật: chạy đủ backend thật với dữ liệu Supabase hợp lệ, chuyển sang **Kết nối backend thật**, bấm **Kiểm tra API phim**, rồi đi qua toàn bộ luồng chọn phim → chọn ghế → tạo hồ sơ → đặt vé → xem vé và đối chiếu dữ liệu trong bảng `bookings`, `booking_details`, `showtime_seats`.

**Chưa kiểm tra tích hợp trực quan trong trình duyệt với backend/database thực tế trên máy bạn.** Cần chạy luồng thật và kiểm tra dữ liệu trước khi báo cáo.

## 7. Việc nhóm cần làm tiếp

1. Chạy backend thật và frontend để thống nhất giao diện với dữ liệu thật.
2. Bật backend thật (api-gateway :8080 và các service con), tạo một booking bằng dữ liệu thử; kiểm tra bảng bookings, booking_details, showtime_seats.
3. Xác nhận tiêu chí nộp bài: có bắt buộc React, đăng nhập, quản trị hay thanh toán online không. Những mục đó cần triển khai thêm nếu giáo viên yêu cầu.
4. Hoàn thiện xác thực và phân quyền backend, băm mật khẩu; sau đó mới nối giao diện đăng nhập/thông tin người dùng.
5. Bổ sung quản trị/phần thanh toán theo phạm vi nhóm thống nhất và viết báo cáo đúng những gì đã kiểm thử.
