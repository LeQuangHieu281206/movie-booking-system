# Kết quả kiểm tra bản bàn giao

## Phạm vi

Đọc mã nguồn controller/DTO/enum/service và routes Gateway trong `movie-booking-source-share.zip`. Bản React nằm riêng trong `frontend-react`; không thay đổi Java, database hoặc cấu hình backend.

## Đã thực hiện

| Kiểm tra | Kết quả |
|---|---|
| Cài dependency có package-lock | Thành công; React/React DOM 19.3.1, Vite 8.3.5, plugin React 6.1.2 |
| `npm test` | 17/17 đạt |
| `npm run build` | Thành công |
| Trang chủ desktop trong trình duyệt | Hiển thị giao diện CineRoom, phim và lịch chiếu mẫu |
| Chọn A1 + A2 của một suất mẫu | Tổng tiền 220.000đ với basePrice 110.000đ; ghế đã BOOKED bị khóa |
| Tạo hồ sơ và booking mẫu | Nhận bookingCode, PENDING |
| Thanh toán mẫu thành công | Đọc lại booking trả PAID, hiển thị Đã thanh toán |
| Lịch sử và tải lại trang | Hồ sơ và vé mẫu vẫn tải được |
| Thanh toán mẫu thất bại | Booking mẫu trả CANCELLED; unit test xác nhận ghế được trả |
| Chuyển sang Backend thật khi Gateway không có trong môi trường QA | Báo HTTP 502, không thay dữ liệu thật bằng phim mẫu |
| Tách hồ sơ demo/live | Chuyển sang live không sử dụng userId mẫu |
| Khung nhìn 390px | Trang chủ và dialog đặt vé không tràn ngang toàn trang; đo scrollWidth = clientWidth; thử được bước chọn ghế, xác nhận, booking và payment FAILED |

## Nội dung 17 kiểm tra tự động

- JSON thô và ApiResponse wrapper.
- Hiển thị fieldErrors từ lỗi validation, không fallback demo.
- Phản hồi HTML thay JSON phải báo lỗi.
- Ghép dữ liệu ghế từ hai API public bằng ID đúng.
- Request booking/payment/cancel đúng endpoint và DTO SOA.
- Tạo hồ sơ CUSTOMER, không dùng getUsers để giả lập login.
- Lỗi kết nối không tự gửi lại POST.
- Demo PENDING → PAID, khóa ghế và từ chối đặt lại ghế đó.
- Demo FAILED/hủy trả ghế AVAILABLE.
- Demo lưu dữ liệu giữa các lần tạo adapter.
- Giữ showtimeSeatId khi ghép vị trí vật lý.
- Thiếu metadata ghế phải báo lỗi.
- Từ chối ghế BOOKED, ghế trùng, ghế khác suất, không chọn ghế.
- Từ chối suất cũ/hủy, giá sai và chưa chọn khách.
- Tìm phim không dấu kết hợp thể loại/status.
- Lấy ngày theo Việt Nam khi qua nửa đêm UTC.
- Tạo mã bằng crypto.getRandomValues khi HTTP preview không có randomUUID.

## Chưa được xác nhận

- Chưa chạy end-to-end với Supabase, RabbitMQ và các service Java trên máy nhóm.
- Chưa kiểm chứng xử lý cạnh tranh nhiều khách đặt cùng ghế; đây là trách nhiệm chính của backend.
- Chưa triển khai hosting hoặc push/merge vào GitHub của nhóm.
- Chưa đánh dấu thay cho các thành viên rằng họ đã tự làm/kiểm thử các nhiệm vụ trong bảng phân công.

Sử dụng `KIEM-THU-VA-NOP-BAI.md` để nhóm ghi kết quả thực tế và hoàn tất nghiệm thu.
