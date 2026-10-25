# Checklist kiểm thử và nộp bài

Bản nền đã build và chạy thử frontend. Bảng dưới là việc nhóm cần làm **trên backend thật**; chưa đánh dấu hoàn thành thay nhóm.

## Trước khi thử

- [ ] Cả nhóm thống nhất dùng `frontend-react`, nhánh tích hợp và phân công.
- [ ] Node đúng phiên bản; `npm.cmd ci` thành công.
- [ ] Gateway đang chạy ở cổng 8080 hoặc BACKEND_URL đã đổi đúng.
- [ ] movie, venue, user, booking, payment và notification không có lỗi khởi động; RabbitMQ sẵn sàng.
- [ ] Có phim SHOWING, rạp, phòng, ghế vật lý, suất SCHEDULED trong tương lai và showtime_seats cho suất đó.
- [ ] Dùng hồ sơ thử nghiệm, không dùng mật khẩu/thông tin ngân hàng thật.

## Trường hợp kiểm thử

| Mã | Người chính | Thao tác | Kết quả mong đợi | Kết quả chạy thật / bằng chứng |
|---|---|---|---|---|
| F01 | Frontend 2 | Mở phim với Backend thật | API getMovies trả dữ liệu và card hiển thị khớp | Chưa thực hiện |
| F02 | Frontend 2 | Tìm tên không dấu và phối hợp bộ lọc | Chỉ phim phù hợp xuất hiện | Chưa thực hiện |
| F03 | Frontend 2 | Dữ liệu trống, mô tả dài, poster sai | Không vỡ trang, thông báo/placeholder đúng | Chưa thực hiện |
| V01 | Frontend 3 | Lọc rạp/thành phố, mở lịch một rạp | Không lẫn cinemaId | Chưa thực hiện |
| V02 | Frontend 3 | Chọn phim/ngày/giờ | showtimeId đúng, ngày GMT+7, không cho đặt suất cũ/hủy | Chưa thực hiện |
| U01 | Frontend 3 | Tạo hồ sơ, thử email trùng, nhập mã đã có | userId thật, lỗi hiển thị, role CUSTOMER | Chưa thực hiện |
| B01 | Bạn | Chọn 2 ghế trống, đặt vé | POST đúng showtimeSeatIds, nhận PENDING và bookingCode | Chưa thực hiện |
| B02 | Bạn + backend | Khách khác chiếm ghế đã chọn trước khi xác nhận | Báo xung đột/tải lại, không báo thành công sai | Chưa thực hiện |
| B03 | Bạn | Bấm xác nhận liên tiếp khi mạng chậm | UI không gửi nhiều POST đồng thời | Chưa thực hiện |
| B04 | Bạn | Mất phản hồi sau POST | Hướng dẫn kiểm tra Vé của tôi, không tự gửi lại | Chưa thực hiện |
| P01 | Bạn + backend | Mô phỏng SUCCESS | Payment được gửi; booking API sau đó trả PAID | Chưa thực hiện |
| P02 | Bạn + backend | Mô phỏng FAILED | Booking CANCELLED; API ghế trả AVAILABLE | Chưa thực hiện |
| P03 | Bạn | Hủy PENDING chưa thanh toán | Backend CANCELLED, ghế trả về AVAILABLE | Chưa thực hiện |
| P04 | Bạn + backend | RabbitMQ/listener có lỗi | UI giữ PENDING, không tự báo PAID | Chưa thực hiện |
| T01 | Bạn | Vé của tôi rồi tải lại trang | Hồ sơ/booking đọc đúng nguồn, trạng thái không mất | Chưa thực hiện |
| X01 | Cả ba | Chuyển demo ↔ backend | Không trộn mã khách/vé, backend lỗi không dùng mẫu thay thế | Chưa thực hiện |
| X02 | Cả ba | Dùng điện thoại hoặc DevTools 390px | Không tràn ngang trang; sơ đồ ghế có thể cuộn riêng | Chưa thực hiện |
| X03 | Cả ba | Bàn phím Tab/Shift+Tab/Esc | Focus trong dialog, đóng được khi không gửi yêu cầu | Chưa thực hiện |

Không kết luận đã chống trùng ghế đồng thời chỉ vì B02 đạt. Backend phải kiểm thử cạnh tranh và có cơ chế khóa phù hợp.

## Khi API lỗi: xác định đúng dịch vụ

| Biểu hiện | Kiểm tra |
|---|---|
| Không có JSON từ Gateway | Cổng Gateway, JAR/process, log api-gateway |
| Phim được nhưng lịch lỗi | venue-service cũng phải chạy vì movie gọi lấy thông tin phòng |
| Ghế chưa khớp | getShowtimeSeats.seatId phải có trong getSeatsByRoomId.id của roomId đó |
| Tạo khách lỗi | user-service, email trùng, kết nối DB |
| Tạo booking lỗi | booking, user, movie, venue; xác nhận cùng database/môi trường |
| Payment gửi rồi PENDING mãi | RabbitMQ, payment-service và PaymentCompletedListener của booking-service |
| Ghế vẫn BOOKED sau hủy | Log release của booking và movie; không tự sửa UI để che lỗi backend |
| npm báo cổng 5173 bận | Dừng frontend cũ bằng Ctrl+C trong đúng terminal hoặc thống nhất cổng khác |

Cổng 8080 bận: tìm chương trình đang dùng (`netstat -ano | findstr :8080`), hoặc chạy Gateway ở cổng khác và đặt `BACKEND_URL` trong `.env.local`.

## Trước khi nộp

- [ ] Điền tên thật vào phân công, ghi công việc thực sự đã làm, link commit/PR/review.
- [ ] Sau merge cuối, Pull đúng nhánh nộp; tests/build đạt.
- [ ] Thử lại luồng backend thật trên đúng commit nộp; ghi rõ tính năng chưa đạt.
- [ ] README gốc repository liên kết tới `frontend-react/README.md`, mô tả đúng React và Gateway.
- [ ] Không commit node_modules, target, logs, dist hoặc mật khẩu/token.
- [ ] Thầy có quyền xem repository theo cách nhóm đã chọn; link repo khác link website đang chạy.
- [ ] Chỉ ghi có link website nếu đã triển khai frontend và backend lên hạ tầng thầy truy cập được. localhost:5173 chỉ chạy trên máy đang mở frontend.
- [ ] Có kịch bản demo 3–5 phút: phim → lịch → ghế → PENDING → payment mô phỏng → PAID → lịch sử; nói rõ đâu là mô phỏng.

Nhóm hoàn thành khi luồng cần nộp chạy trên backend thật và mọi người có đóng góp/hiểu phần của mình. Có file React và link GitHub chưa đủ để khẳng định toàn hệ thống đã hoàn tất.
