# CineRoom — Frontend React cho backend SOA

Frontend React đã tích hợp theo controller/DTO của backend SOA. Giao diện tiếng Việt, màu tối/vàng, gồm phim, lịch chiếu, rạp, chọn ghế, đăng nhập JWT, booking, thanh toán QR SePay và vé đã đặt. Toàn bộ dữ liệu lấy từ backend/database thật; không còn chế độ dữ liệu mẫu.

Đây là mã nguồn frontend để đặt trong repository của nhóm. Không cần chép Java bridge của bản frontend cũ sang backend SOA.

## 1. Đưa vào dự án trên máy bạn

1. Giải nén ZIP được gửi. Bên trong có thư mục `frontend-react`.
2. Chép **cả thư mục `frontend-react`** vào `E:\ProjectGH\movie-booking-system`.
3. Đường dẫn đúng của package là `E:\ProjectGH\movie-booking-system\frontend-react\package.json`.
4. Các thư mục `frontend`, `frontend-` trước đó có thể giữ để đối chiếu. Nhóm thống nhất dùng `frontend-react` làm frontend chính và cập nhật README gốc sau khi tích hợp.

Không chạy `node server.mjs` cho bản React này; dùng các lệnh npm bên dưới. Tắt frontend cũ ở cổng 5173 bằng Ctrl+C trong đúng cửa sổ chạy `node server.mjs` trước khi mở bản mới.

## 2. Chạy trên Windows bằng PowerShell

Cần Node.js 20.19+ thuộc dòng 20 hoặc Node.js 22.12 trở lên. Khuyến nghị dùng bản LTS từ <https://nodejs.org/>. Kiểm tra:

```powershell
node -v
```

Mở PowerShell mới. Chạy từng lệnh, không chép phần `PS ...>` vào lệnh:

```powershell
cd E:\ProjectGH\movie-booking-system\frontend-react
```

```powershell
npm.cmd ci
```

```powershell
npm.cmd run dev
```

Mở <http://localhost:5173>. Giữ cửa sổ npm đang chạy. Dùng Ctrl+C để dừng. Các lệnh `.cmd` hoạt động trong PowerShell mà không cần đổi chính sách chạy script của Windows.

### Khi kết nối backend của nhóm

Mặc định frontend gọi `/api/...` trên cùng origin, Vite proxy sang **http://localhost:8080**, là cổng API Gateway của nhóm (`api-gateway/src/main/resources/application.yml`). Nếu máy bạn có chương trình khác đang chiếm cổng 8080, tạo `.env.local` với `BACKEND_URL=http://localhost:<cổng khác>` và chạy Gateway ở cổng đó.

- Để nguyên Docker và các service Java đang hoạt động. Không khởi động trùng các dịch vụ.
- Nếu Gateway chưa chạy, trong PowerShell đã cấu hình JAVA_HOME của backend, đứng ở thư mục gốc repository và chạy (cổng 8080 là mặc định trong `application.yml`, không cần tham số):

```powershell
java -jar .\api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar
```

Lệnh này dùng JAR đã build. Chờ thông báo Started, rồi kiểm tra <http://localhost:8080/api/getMovies>. JSON `[]` là danh sách trống, không phải lỗi kết nối.

Các service khác vẫn dùng cấu hình hiện tại: movie 8081, booking 8082, venue 8083, user 8084, payment 8086, notification 8087. RabbitMQ phải sẵn sàng cho sự kiện thanh toán. `common` là thư viện, không chạy riêng.

Trên giao diện, đăng nhập hoặc đăng ký tài khoản trước khi đặt vé. Nếu API lỗi, trang báo lỗi; không có dữ liệu thay thế.

### Nếu Gateway chạy ở cổng khác

Tại thư mục `frontend-react`, tạo file `.env.local` từ mẫu:

```powershell
Copy-Item .env.example .env.local
```

Sửa `BACKEND_URL` trong `.env.local` thành cổng Gateway thực tế, ví dụ `http://localhost:8088`.

Tắt rồi chạy lại npm. Biến môi trường `BACKEND_URL` có sẵn trong cửa sổ PowerShell sẽ được ưu tiên hơn file này. Không đặt mật khẩu database vào frontend.

## 3. Thử chức năng theo thứ tự

1. Phim: thử tìm tên không dấu, lọc thể loại, Đang chiếu/Sắp chiếu, mở chi tiết phim.
2. Rạp: chọn thành phố, mở lịch chiếu của một rạp.
3. Lịch chiếu: chọn phim/ngày/thành phố; chỉ suất SCHEDULED chưa bắt đầu mới hiện nút đặt.
4. Đăng ký tài khoản mới hoặc đăng nhập ở mục **Tài khoản**.
5. Chọn ghế: ghế BOOKED/HELD bị khóa, chọn 1–8 ghế trống, tổng tiền = basePrice × số ghế.
6. Xác nhận đặt vé. Tạo thành công nhận bookingCode và trạng thái PENDING.
7. Bấm **Tạo mã QR thanh toán**: backend payment-service trả mã VietQR SePay. Quét mã bằng ứng dụng ngân hàng, chuyển đúng số tiền/nội dung.
8. Chờ webhook SePay → RabbitMQ → booking-service cập nhật PAID; panel tự hỏi trạng thái 3 phút, hoặc bấm **Cập nhật trạng thái**.
9. Vé của tôi: xem danh sách, cập nhật trạng thái, hủy booking PENDING chưa thanh toán (ghế được trả).
10. Tải lại trang: phiên đăng nhập giữ nguyên; access token hết hạn được tự đổi bằng refresh token. Vé luôn tải lại từ backend.

Backend xác thực bằng JWT: gateway kiểm tra token, tự gắn `X-User-Id`/`X-User-Role` và chặn các API quản trị. Frontend không tự gửi `userId`, `totalAmount` hay `role`; booking-service lấy từ token và giá suất chiếu.

## 4. Kiểm tra và build

```powershell
npm.cmd test
```

```powershell
npm.cmd run build
```

```powershell
npm.cmd run preview
```

`preview` phục vụ build cục bộ ở 5173 và có proxy API giống dev. Thư mục `dist` là bản tĩnh sau build. Khi triển khai lên hosting thật, cần reverse proxy `/api` đến Gateway có thể truy cập qua mạng; proxy Vite dev không tự đi theo các file tĩnh. Đường dẫn localhost của máy bạn không dùng được từ máy thầy.

## 5. Ba người cùng làm

Đọc **[docs/PHAN-CONG-3-NGUOI.md](docs/PHAN-CONG-3-NGUOI.md)** trước khi sửa mã.

| Phần | File chính | Người đề xuất |
|---|---|---|
| Phim, trang chủ, chi tiết, tìm kiếm | `src/pages/Catalog.jsx` | Bạn frontend 2 |
| Rạp, lịch chiếu | `src/pages/Venues.jsx`, `src/components/ScheduleBoard.jsx` | Bạn frontend 3 |
| Chọn ghế, booking, thanh toán, vé | `src/pages/Booking.jsx`, `src/pages/Tickets.jsx`, `src/components/PaymentPanel.jsx` | Bạn |
| Đăng nhập JWT, API, kiểu dữ liệu, layout, CSS chung | `src/services/*`, `src/pages/Auth.jsx`, `src/lib/`, `src/App.jsx`, `src/components/UI.jsx`, `src/styles.css` | Thống nhất người tích hợp; mọi thay đổi phải review |

Đây là bản nền được hỗ trợ tạo bằng AI. Đóng góp cá nhân nên là công việc thực tế: hiểu, chỉnh sửa, kiểm thử, tìm lỗi, tích hợp và tài liệu. Không chia một bản mã có sẵn thành commit giả để nhận là tự viết từ đầu.

## 6. Tài liệu còn lại

- [Hợp đồng API và các điểm cần phối hợp backend](docs/API-VA-GIOI-HAN.md)
- [Checklist chạy thử trước khi nộp](docs/KIEM-THU-VA-NOP-BAI.md)
- [Kết quả kiểm tra bản bàn giao](docs/KET-QUA-KIEM-TRA.md)

## 7. Các giới hạn cần biết

Frontend đã đối chiếu controller/DTO và kiểm tra bằng unit test + build; cần chạy lại checklist với Gateway, database và RabbitMQ thực tế trước khi kết luận end-to-end thành công.

- Access token sống 2 giờ, refresh token 7 ngày; hết refresh token phải đăng nhập lại.
- Thanh toán dùng QR SePay; booking chỉ chuyển PAID khi webhook chuyển khoản về và RabbitMQ gửi được sự kiện. Chưa có huỷ/hoàn tiền tự động.
- API lịch sử booking không trả chi tiết ghế; ghế hiển thị theo dữ liệu suất chiếu hiện tại.
- Poster lấy từ `posterUrl` của backend (http/https hoặc `/assets/...`); URL lỗi hoặc thiếu thì hiện placeholder.
