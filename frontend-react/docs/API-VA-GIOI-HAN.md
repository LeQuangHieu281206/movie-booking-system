# Hợp đồng API và giới hạn tích hợp

Đối chiếu từ các Java controller, DTO, enum, service và cấu hình Gateway trong ZIP nhóm gửi. Mọi API trình duyệt gọi dưới đây đi qua `/api` rồi Gateway; không gọi `/internal/**` trực tiếp từ frontend.

## API đã tích hợp

| Chức năng | Method + đường dẫn sau Gateway | Dữ liệu / kết quả cần dùng |
|---|---|---|
| Danh sách phim | GET `/api/getMovies` | movieId, title, description, durationMinutes, status, genres, posterUrl… |
| Thể loại | GET `/api/getGenres` | genreId, name |
| Rạp | GET `/api/getCinemas` | cinemaId, name, city, address, phoneNumber |
| Lịch chiếu | GET `/api/getShowtimes?movieId=...` hoặc không query | showtimeId, movieId, roomId, cinemaId/name/city/address, startTime, basePrice, status |
| Chi tiết suất | GET `/api/getShowtimeById?id=...` | tải lại trước khi đặt |
| Trạng thái ghế | GET `/api/getShowtimeSeats?showtimeId=...` | showtimeSeatId, showtimeId, seatId, status |
| Vị trí ghế | GET `/api/getSeatsByRoomId?roomId=...` | id, seatRow, seatNumber, seatType |
| Lấy hồ sơ | GET `/api/getUserById?id=...` | userId, fullName, email, phoneNumber, role |
| Tạo hồ sơ | POST `/api/createUser` | fullName, email, phoneNumber, password, role=CUSTOMER |
| Đặt vé | POST `/api/createBooking` | userId, showtimeId, showtimeSeatIds[], totalAmount, paymentMethod |
| Vé của khách | GET `/api/getBookingsByUserId?userId=...` | BookingResponse[] |
| Trạng thái booking | GET `/api/getBookingById?id=...` | BookingResponse |
| Hủy vé | PUT `/api/updateBooking?id=...` | `{ "status": "CANCELLED" }` |
| Thanh toán mô phỏng | POST `/api/createPayment` | bookingId, userId, amount, simulateFailure |

Tầng HTTP nhận JSON thô hoặc envelope `{ success, message, data }`, kiểm tra HTTP status và fieldErrors. Các service trong bản ZIP hiện trả DTO/danh sách thô; có ApiResponse chung nhưng adapter không ép buộc service phải bọc envelope.

## Ghép ghế đúng

1. Lấy roomId từ getShowtimeById.
2. Gọi song song getShowtimeSeats và getSeatsByRoomId.
3. Ghép `ShowtimeSeatView.seatId === SeatResponse.id`.
4. Hiển thị seatRow + seatNumber; dùng trạng thái của showtime seat.
5. Khi đặt gửi **showtimeSeatId**, không gửi SeatResponse.id.
6. Không khớp metadata thì báo lỗi, không tự dựng hàng/số ghế giả.

Ví dụ: ghế vật lý id=9 nằm ở C4; suất 20 có showtimeSeatId=801 trỏ seatId=9. Nhãn UI là C4; POST gửi 801.

Sơ đồ thể hiện theo hàng và thứ tự seatNumber. API chưa cung cấp tọa độ lối đi, độ lệch hoặc bố cục vật lý chi tiết; không coi bố cục hàng ghế này là bản vẽ kiến trúc rạp.

## Booking

```json
{
  "userId": 12,
  "showtimeId": 20,
  "showtimeSeatIds": [801, 802],
  "totalAmount": 170000,
  "paymentMethod": "BANK_TRANSFER"
}
```

Các ID trên chỉ minh họa. Phải lấy ID thật từ API của cùng môi trường. Giới hạn 8 ghế là quy tắc UI, không phải kiểm soát bảo mật phía server.

Enum paymentMethod: CASH, BANK_TRANSFER, CARD, E_WALLET.
Enum booking status: PENDING, PAID, CANCELLED, EXPIRED.
Enum showtime status: SCHEDULED, ONGOING, ENDED, CANCELLED. UI chỉ cho đặt SCHEDULED trong tương lai.

## Thanh toán

POST createPayment hiện phát sự kiện PaymentCompletedEvent qua RabbitMQ. SUCCESS/FAILED trong phản hồi là trạng thái sự kiện mô phỏng; booking-service xử lý bất đồng bộ.

UI gửi payment một lần, hỏi getBookingById tối đa 8 lần trong vòng khoảng 8 giây cộng thời gian API. Chỉ khi API booking trả PAID mới hiển thị đã thanh toán. Nếu vẫn PENDING, giữ trạng thái chờ và cho cập nhật thủ công. Khi đã thử gửi, chặn gửi lại trong cùng phiên tab bằng sessionStorage, để giảm nguy cơ lặp thanh toán khi kết nối bị gián đoạn. Đây không thay thế idempotency phía server.

Với FAILED, backend dự kiến chuyển PENDING → CANCELLED và yêu cầu movie-service release ghế. Không tự gán PAID/CANCELLED trên client trong chế độ backend.

Hủy dùng updateBooking CANCELLED, không dùng deleteBooking vì service delete hiện không đảm bảo trả ghế.

## Phân biệt mẫu và thật

| Nội dung | Demo | Backend thật |
|---|---|---|
| Nguồn | demo-data.js + localStorage | API Java qua Gateway |
| Mã khách | localStorage riêng của demo | getUserById/createUser |
| Booking | chỉ trình duyệt | POST createBooking |
| Thanh toán | chuyển trạng thái mẫu cục bộ | payment-service phát RabbitMQ; đọc lại booking-service |
| Backend lỗi | không liên quan | báo lỗi, không tự fallback demo |

Bộ dữ liệu mẫu tự reset sau 24 giờ; userId demo cũ có thể không còn tồn tại sau reset. Khi đó tạo hồ sơ mẫu mới. Demo không phải bằng chứng Supabase hoạt động.

## Những việc cần hai bạn backend phối hợp

Đây là nhận xét dựa trên mã nguồn gửi, không phải đã kiểm chứng bằng tấn công hay thay đổi database.

| Điểm hiện tại | Hệ quả | Công việc phía backend |
|---|---|---|
| Chưa có API login/token, xác thực/ủy quyền theo người dùng | Nhập userId không chứng minh danh tính; không bảo vệ vé/hồ sơ khi public | Bổ sung auth và kiểm tra quyền từng API; UI hồ sơ hiện chỉ phù hợp đồ án cục bộ |
| UserServiceImpl gán request.password trực tiếp vào passwordHash | Tên field không đồng nghĩa đã băm mật khẩu | Dùng password encoder; không dùng tài khoản/mật khẩu thật để thử bản hiện tại |
| BookingCreateRequest nhận totalAmount; BookingServiceImpl dùng trực tiếp | Sửa request có thể thay tổng tiền | Server tính giá từ suất/ghế và quy tắc giá; so sánh hoặc bỏ qua giá client |
| InternalShowtimeSeatController hold đọc rồi ghi trạng thái, chưa khóa/optimistic version; chưa ràng buộc từng ID thuộc đúng showtime trong mã hold | Cạnh tranh đặt ghế và ID sai môi trường chưa được bảo vệ đầy đủ | Khóa hoặc update có điều kiện; validate showtime ownership, unique IDs; kiểm thử đồng thời |
| Đóng modal PENDING không tự hủy; chưa thấy cơ chế TTL hoàn chỉnh trong mã hiện tại | Ghế có thể giữ lâu nếu người dùng bỏ dở | Thêm hết hạn booking và trả ghế; trước mắt dùng nút hủy khi chưa gửi payment |
| PaymentService mô phỏng và phát sự kiện, chưa xác thực giao dịch hay idempotency; listener SUCCESS đặt PAID mà chưa giới hạn chuyển trạng thái | Không dùng để thu tiền; sự kiện trùng/muộn có thể chuyển trạng thái không mong muốn | Validate booking/amount/owner, state machine, idempotency; không để SUCCESS hồi sinh vé đã hủy/hết hạn |
| BookingResponse không có danh sách booking detail | Lịch sử hiện có mã, phim/rạp/suất/tổng tiền nhưng không đọc lại được ghế từ API booking | Bổ sung API chi tiết hoặc showtimeSeatIds + nhãn ghế có kiểm tra quyền; frontend không bịa dữ liệu lịch sử |

Bản bàn giao chỉ sửa frontend, không tự sửa Java/config/database của nhóm. Không chứa mật khẩu database từ ZIP gốc.

## Tài liệu công nghệ

- React: <https://react.dev/learn>
- Vite env: <https://vite.dev/guide/env-and-mode>
- Vite proxy: <https://vite.dev/config/server-options#server-proxy>

BACKEND_URL không có tiền tố VITE_ nên chỉ dùng trong cấu hình proxy Node, không đưa mật khẩu hoặc bí mật vào biến client. File tĩnh build cần web server/reverse proxy riêng khi triển khai thật.
