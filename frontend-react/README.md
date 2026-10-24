# CineRoom — Frontend React cho backend SOA

Frontend React đã tích hợp theo controller/DTO trong bản `movie-booking-source-share.zip` của nhóm. Giao diện tiếng Việt, màu tối/vàng, gồm phim, lịch chiếu, rạp, chọn ghế, hồ sơ khách, booking, thanh toán mô phỏng và vé đã đặt.

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

### Khi chỉ cần xem giao diện

Bấm **Thử dữ liệu mẫu** trên banner hoặc **Backend thật** ở thanh đầu trang để chuyển chế độ. Có thể mở trực tiếp <http://localhost:5173/?mode=demo>.

Dữ liệu mẫu gồm phim/rạp hư cấu, không gửi yêu cầu đến backend. Vé mẫu chỉ lưu trong trình duyệt; bộ dữ liệu mẫu tự làm mới sau 24 giờ. Không dùng ảnh vé mẫu làm bằng chứng đã kết nối database.

### Khi kết nối backend của nhóm

Mặc định frontend gọi `/api/...` trên cùng origin, Vite proxy sang **http://localhost:8088**. Đây là cổng Gateway đã chọn do cổng 8080 trên máy bạn đang có `AgentService.exe` sử dụng.

- Để nguyên Docker và các service Java đang hoạt động. Không khởi động trùng các dịch vụ.
- Nếu Gateway chưa chạy, trong PowerShell đã cấu hình JAVA_HOME của backend, đứng ở thư mục gốc repository và chạy:

```powershell
java -jar .\api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar --server.port=8088
```

Lệnh này dùng JAR đã build. Chờ thông báo Started, rồi kiểm tra <http://localhost:8088/api/getMovies>. JSON `[]` là danh sách trống, không phải lỗi kết nối.

Các service khác vẫn dùng cấu hình hiện tại: movie 8081, booking 8082, venue 8083, user 8084, payment 8086, notification 8087. RabbitMQ phải sẵn sàng cho sự kiện thanh toán. `common` là thư viện, không chạy riêng.

Trên giao diện, chọn **Backend thật**. Nếu API lỗi, trang báo lỗi; không tự thay bằng dữ liệu mẫu.

### Nếu nhóm đổi Gateway về 8080

Tại thư mục `frontend-react`, tạo file `.env.local` từ mẫu:

```powershell
Copy-Item .env.example .env.local
```

Sửa nội dung `.env.local` thành:

```dotenv
BACKEND_URL=http://localhost:8080
```

Tắt rồi chạy lại npm. Biến môi trường `BACKEND_URL` có sẵn trong cửa sổ PowerShell sẽ được ưu tiên hơn file này. Không đặt mật khẩu database vào frontend.

## 3. Thử chức năng theo thứ tự

1. Phim: thử tìm tên không dấu, lọc thể loại, Đang chiếu/Sắp chiếu, mở chi tiết phim.
2. Rạp: chọn thành phố, mở lịch chiếu của một rạp.
3. Lịch chiếu: chọn phim/ngày/thành phố; chỉ suất SCHEDULED chưa bắt đầu mới hiện nút đặt.
4. Chọn ghế: ghế BOOKED/HELD bị khóa, chọn 1–8 ghế trống, tổng tiền = basePrice × số ghế.
5. Hồ sơ: tạo khách hàng thử nghiệm hoặc dùng mã khách có sẵn. Ghi lại mã khách.
6. Xác nhận thông tin và đặt vé. Tạo thành công nhận bookingCode và trạng thái PENDING.
7. Chọn mô phỏng thanh toán thành công. UI chỉ báo PAID sau khi `getBookingById` trả PAID.
8. Tạo vé khác và thử thất bại: backend phải cập nhật CANCELLED và trả ghế. Kiểm tra lại sơ đồ.
9. Vé của tôi: xem danh sách, cập nhật trạng thái, hủy booking PENDING chưa gửi thanh toán.
10. Tải lại trang: mã khách của từng chế độ được lưu riêng; thông tin vé thật phải tải lại từ backend.

Chưa có API đăng nhập/xác thực trong backend được cung cấp. UI dùng **hồ sơ khách hàng**, không giả lập đăng nhập bằng cách đọc danh sách người dùng. Khi tạo hồ sơ, adapter gửi password ngẫu nhiên và role CUSTOMER theo DTO bắt buộc; password không được lưu hoặc hiển thị ở frontend. Chức năng này không thay thế hệ thống đăng nhập.

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
| Rạp, lịch chiếu, hồ sơ khách | `src/pages/Venues.jsx`, `src/components/ScheduleBoard.jsx`, `src/components/CustomerForm.jsx` | Bạn frontend 3 |
| Chọn ghế, booking, thanh toán, vé | `src/pages/Booking.jsx`, `src/pages/Tickets.jsx`, `src/components/PaymentPanel.jsx` | Bạn |
| API, kiểu dữ liệu, layout, CSS chung | `src/services/api.js`, `src/lib/`, `src/App.jsx`, `src/components/UI.jsx`, `src/styles.css` | Thống nhất người tích hợp; mọi thay đổi phải review |

Đây là bản nền được hỗ trợ tạo bằng AI. Đóng góp cá nhân nên là công việc thực tế: hiểu, chỉnh sửa, kiểm thử, tìm lỗi, tích hợp và tài liệu. Không chia một bản mã có sẵn thành commit giả để nhận là tự viết từ đầu.

## 6. Tài liệu còn lại

- [Hợp đồng API và các điểm cần phối hợp backend](docs/API-VA-GIOI-HAN.md)
- [Checklist chạy thử trước khi nộp](docs/KIEM-THU-VA-NOP-BAI.md)
- [Kết quả kiểm tra bản bàn giao](docs/KET-QUA-KIEM-TRA.md)

## 7. Các giới hạn cần biết

Đã đối chiếu controller/DTO trong ZIP và kiểm tra frontend bằng unit test, build và luồng mẫu trên trình duyệt. Chưa chạy thử với database và các service Java trên máy bạn trong phiên này; cần hoàn tất checklist Backend thật trước khi kết luận tích hợp end-to-end thành công.

Backend hiện chưa có xác thực/ủy quyền, còn nhận tổng tiền từ client, giữ ghế chưa có khóa bảo vệ cạnh tranh đầy đủ và thanh toán chỉ mô phỏng. Đây là phạm vi đồ án cục bộ, chưa phải hệ thống bán vé công khai sẵn sàng thu tiền. Chi tiết và nhiệm vụ phía backend nằm trong tài liệu API.

Các poster SVG mẫu được tái sử dụng từ frontend CineRoom cũ; phim thật hiển thị posterUrl từ backend, ảnh lỗi có placeholder.
