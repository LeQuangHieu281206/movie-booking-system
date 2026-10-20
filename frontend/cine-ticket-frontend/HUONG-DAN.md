# CineRoom — frontend đặt vé xem phim

Bản frontend được viết cho backend trong file **movie-booking-system-main (1)(1).zip** bạn gửi, package Java `com.ttcsn.moviebooking`. Đây là dự án Spring Boot đơn khối, khác dự án CineVision microservices trao đổi trước đó. Không dùng Eureka hoặc Docker để chạy riêng frontend này.

## 1. Xem giao diện ngay — 3 bước

1. Giải nén file ZIP này ra một thư mục riêng.
2. Mở thư mục `cine-ticket-frontend`, bấm đúp **CHAY-FRONTEND.bat**.
3. Truy cập **http://localhost:5173**. Giữ cửa sổ đang chạy mở.

Máy cần có **Node.js 18 trở lên**. Không cần `npm install`. Nếu cửa sổ báo thiếu Node.js thì cần cài Node.js trước. Frontend này không yêu cầu đổi phiên bản Java.

Nếu không dùng file BAT: mở Terminal ngay trong thư mục chứa `server.mjs` rồi nhập đúng **một dòng**:

```powershell
node server.mjs
```

Mặc định website mở ở **chế độ dữ liệu mẫu**, không cần backend hoặc database. Tên phim, poster, rạp, địa chỉ và số điện thoại mẫu đều là minh họa. Vé có tiền tố DEMO và không có giá trị sử dụng.

Luồng thử: chọn một phim đang chiếu → chọn suất → chọn ghế → Tiếp tục đặt vé → tạo hồ sơ → xác nhận → Vé của tôi → xem/in phiếu.

## 2. Kết nối backend thật

Frontend gốc trong ZIP là mẫu React/Vite chưa hoàn thiện, thiếu `App.css` và tham chiếu logo chưa khai báo. Bản thay thế này dùng HTML/CSS/JavaScript thuần, có chia module để dễ học và sửa. Nếu báo cáo của nhóm bắt buộc React, cần cập nhật lựa chọn công nghệ hoặc chuyển giao diện sang React; đây không phải bản React.

### Bước A — bổ sung API đang thiếu

Backend gốc có `getSeatsByRoomId` nhưng API đặt vé lại yêu cầu **showtimeSeatIds**. `seatId` của ghế vật lý không thay thế được `showtimeSeatId` của ghế theo suất. Vì vậy cần bổ sung API đọc ghế theo suất.

Chép file này:

```text
backend-bo-sung/src/main/java/com/ttcsn/moviebooking/controller/FrontendBridgeController.java
```

vào backend của bạn tại:

```text
movie-booking-system-main/backend/src/main/java/com/ttcsn/moviebooking/controller/FrontendBridgeController.java
```

Chỉ thêm **một file Java**, không chép đè các controller cũ. Nếu đã có file trùng tên, đối chiếu trước khi thay thế. Khởi động lại backend theo cách nhóm đang dùng. Backend trong ZIP yêu cầu **Java 21** theo `pom.xml`; tài liệu này không thay đổi yêu cầu đó.

File bổ sung cung cấp:

- `GET /api/frontend/getShowtimeSeats?showtimeId=...`: trả ID đúng theo suất, hàng, số ghế, loại ghế, trạng thái.
- `POST /api/frontend/createBooking`: kiểm tra ghế thuộc đúng suất/phòng, ghế trống, không trùng ID, suất chưa bắt đầu, tối đa 8 ghế; khóa hàng database giữa các yêu cầu qua API bổ sung; tính tổng tiền ở server từ basePrice.
- Đơn mới có trạng thái `PENDING`, thanh toán `CASH` tại quầy. Không giả lập giao dịch ngân hàng hay báo đã thu tiền.

API đặt vé mới không dùng `totalAmount` do frontend cung cấp. Các ghế của đơn chờ thanh toán được đánh dấu `BOOKED`; việc xác nhận tại quầy và giải phóng ghế khi hủy/hết hạn chưa có trong gói này.

### Bước B — có dữ liệu hợp lệ

Database cần có phim, thể loại, rạp, phòng, ghế vật lý, suất chiếu tương lai có trạng thái `SCHEDULED`, giá > 0 và dữ liệu `showtime_seats`.

Nếu thấy sơ đồ ghế trống dù phòng đã có ghế: kiểm tra `showtime_seats`. File **backend-bo-sung/tao-ghe-theo-suat.sql** là lệnh tùy chọn tạo các bản ghi còn thiếu từ phòng và suất sẵn có. Đọc file, sao lưu database và chỉ chạy thủ công sau khi kiểm tra đúng database. Không chạy SQL này để thay cho việc tạo phim/phòng/ghế/suất ban đầu. Không có kết nối hoặc thao tác database tự động trong gói frontend.

### Bước C — chuyển chế độ trên giao diện

1. Backend đang chạy ở `http://localhost:8080`.
2. Chạy frontend như mục 1.
3. Bấm **Dữ liệu mẫu** ở thanh trên cùng (trên điện thoại là nút chấm tròn vàng).
4. Bấm **Kiểm tra API phim**. Nếu báo thành công, chọn **Kết nối backend thật**.

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
| Kết nối | Hai chế độ dữ liệu mẫu/backend thật; báo lỗi rõ, không âm thầm dùng dữ liệu mẫu khi API lỗi |

Bố cục có CSS cho điện thoại, tablet và máy tính; hỗ trợ focus bàn phím, nhãn biểu mẫu, thông báo lỗi, giảm chuyển động theo tùy chọn hệ thống. Tất cả ảnh mẫu là SVG đi kèm, không cần tải ảnh/font ngoài.

## 4. Giới hạn cần biết để trình bày đúng với giáo viên

- **Chưa có đăng nhập/xác thực/phân quyền** vì backend gốc chưa có API này. Mã khách hàng và hồ sơ lưu trong phiên không phải cơ chế xác thực. Bản này dành cho thử nghiệm local/đồ án, không phải hệ thống đã sẵn sàng đưa cho khách dùng công khai.
- Backend gốc lưu `request.password()` trực tiếp vào `passwordHash`; form ghi rõ chỉ dùng mật khẩu thử nghiệm. Frontend không lưu mật khẩu và không giả vờ tạo phiên đăng nhập.
- **Thanh toán tại quầy**. Không có VNPay/MoMo/Stripe, không có giao dịch thật hoặc QR thanh toán. Chưa có quản trị, xác nhận thanh toán, hủy vé hay tự hết hạn đơn trong frontend này.
- API lịch sử gốc chỉ trả booking, không trả tên phim/ghế. Các vé vừa đặt trên trình duyệt này có thông tin đầy đủ lưu cục bộ. Đổi máy/xóa dữ liệu trình duyệt vẫn xem được booking từ API nhưng không có chi tiết ghế; cần thêm API chi tiết vé để khắc phục.
- Chưa có phụ thu loại ghế; mọi ghế dùng `basePrice` của suất chiếu. Thay đổi chính sách giá cần cập nhật cả frontend và backend.
- File Java bổ sung khóa ghế cho các yêu cầu qua API bổ sung. API `/api/createBooking` gốc vẫn tồn tại và cần được nhóm rà soát/thống nhất nếu muốn dùng song song.
- Các API quản lý backend gốc chưa được bảo vệ. Server Node chỉ lắng nghe ở `127.0.0.1`; không mở frontend/proxy này ra Internet trước khi hoàn thiện xác thực và phân quyền backend.

## 5. Cấu trúc để học và chỉnh sửa

```text
cine-ticket-frontend/
  index.html             Khung trang, header, footer, hộp thoại
  src/
    styles.css           Màu sắc và bố cục responsive
    app.js               Render màn hình, xử lý bấm nút và form
    domain.mjs           Lọc phim, thời gian, chọn ghế, kiểm tra đầu vào
    api.js               Gọi API thật, chọn chế độ, lưu hồ sơ/chi tiết vé
    demo.js              Dữ liệu mẫu và mô phỏng luồng đặt vé
  assets/                Poster SVG tự tạo, favicon
  server.mjs             Server local và proxy backend
  backend-bo-sung/       Một controller cần thêm + SQL tùy chọn
  tests/                 Kiểm tra logic và server
  CHAY-FRONTEND.bat      Chạy nhanh trên Windows
```

Sửa màu chính: biến `--gold`, `--bg`, `--panel` đầu file `src/styles.css`. Sửa tiêu đề/thương hiệu: `index.html` và hàm `header()` trong `app.js`. Sửa dữ liệu mẫu: `demo.js`. Khi dùng backend thật, phim và poster lấy từ database, không sửa `demo.js` để đổi dữ liệu thật.

Lưu file rồi tải lại trang để thấy thay đổi. Không có bước build, không có package phụ thuộc, không cần Vite. Có thể giữ nguyên backend hiện tại và đặt frontend này cạnh nó; không cần xóa thư mục frontend cũ trước khi thử.

## 6. Kiểm tra

Chạy trong thư mục frontend:

```powershell
npm test
```

Hoặc không dùng npm:

```powershell
node --test tests/domain.test.mjs tests/demo.test.mjs tests/server.test.mjs tests/api.test.mjs
```

Đã kiểm tra tự động: lọc phim không dấu, chuyển giờ Việt Nam, loại suất hết hạn, giới hạn chọn ghế, đúng ID ghế theo suất, chặn URL không an toàn, luồng đặt vé mẫu và chống đặt trùng, serve module/ảnh/CSS, proxy POST/query/body/lỗi và backend không chạy. API thật được kiểm tra bằng máy chủ giả lập, không phải database của bạn.

**Chưa kiểm tra trực quan trong trình duyệt ở môi trường này. Chưa chạy tích hợp với backend/database trên máy bạn, và chưa biên dịch controller bằng bộ Java 21/Maven của nhóm.** Cần chạy luồng thật sau khi chép controller và kiểm tra dữ liệu; không coi các bài kiểm tra frontend là xác nhận toàn hệ thống đã hoạt động.

## 7. Việc nhóm cần làm tiếp

1. Chạy frontend mẫu để thống nhất giao diện.
2. Thêm controller, bật backend thật, tạo một booking bằng dữ liệu thử; kiểm tra bảng bookings, booking_details, showtime_seats.
3. Xác nhận tiêu chí nộp bài: có bắt buộc React, đăng nhập, quản trị hay thanh toán online không. Những mục đó cần triển khai thêm nếu giáo viên yêu cầu.
4. Hoàn thiện xác thực và phân quyền backend, băm mật khẩu; sau đó mới nối giao diện đăng nhập/thông tin người dùng.
5. Bổ sung quản trị/phần thanh toán theo phạm vi nhóm thống nhất và viết báo cáo đúng những gì đã kiểm thử.
