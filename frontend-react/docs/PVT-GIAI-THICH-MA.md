# Đọc hiểu và trình bày phần của bạn

## 1. Đọc theo thứ tự

1. `pages/Booking.jsx`: UI có ba bước; selected lưu ID ghế theo suất, không phải vị trí trong mảng.
2. `booking/flow.js`: prepareBooking đọc lại suất/ghế rồi quyết định có cần người dùng kiểm tra lại không.
3. `booking/useInventory.js`: đọc lại sơ đồ, bỏ kết quả đọc đã cũ bằng serial/AbortController.
4. `booking/payment.js`: hỏi trạng thái trước khi gửi; phát payment một lần; đọc lại trạng thái cuối.
5. `booking/storage.js`: chỉ lưu dấu thao tác và snapshot tối thiểu; không lưu mật khẩu/thông tin thẻ.
6. `pages/Tickets.jsx`: tải lịch sử từ backend, lọc dữ liệu, mở chi tiết dùng PaymentPanel.

## 2. Chọn ghế và đặt vé

- `useState`: giữ lựa chọn, bước đang mở, lỗi và trạng thái chờ trong giao diện.
- `useRef`: giữ khóa gửi để hai lần bấm sát nhau không cùng chạy submit trước khi React render lại.
- `async/await`: chờ API lấy dữ liệu xong rồi mới kiểm tra và gửi bước tiếp theo.
- `prepareBooking`: nếu A2 đã BOOKED, trả review=seats và danh sách còn hợp lệ. Nếu giá hoặc phòng/giờ đổi, trả review=show. Không có payload thì Booking.jsx không gọi createBooking.
- Khi dữ liệu ổn, bookingPayload tạo userId, showtimeId, showtimeSeatIds, totalAmount, paymentMethod đúng DTO.
- Ghế vật lý như id=9 có thể là C4. Ghế theo suất như showtimeSeatId=801 trỏ tới ghế đó. Gửi 801 khi đặt, không gửi 9.

Việc hỏi lại API chỉ giảm dữ liệu cũ. Nếu hai người cùng thấy ghế trống rồi gửi đồng thời, server vẫn phải xử lý tranh chấp.

## 3. Vì sao không tự gửi lại khi mất mạng?

Client không nhận phản hồi không đồng nghĩa server chưa tạo booking. Gửi lại có thể tạo thêm booking. UI ghi nhớ lần gửi và yêu cầu xem lịch sử. Dấu cục bộ không thay thế idempotency key phía server. Kể cả lịch sử chưa thấy vé, server có thể còn đang xử lý.

## 4. Vì sao payment SUCCESS chưa đủ?

Payment-service gửi sự kiện qua RabbitMQ. Booking-service nhận sự kiện rồi cập nhật database. Có khoảng thời gian giữa hai bước, hoặc listener bị lỗi. Chỉ khi getBookingById trả PAID mới hiển thị đã thanh toán. Sau tối đa 8 lần đọc nếu vẫn PENDING, giữ chờ và cho cập nhật thủ công.

## 5. Hủy khác xóa

UI gọi updateBooking với status CANCELLED để service thực hiện luồng trả ghế. Không dùng deleteBooking để hủy vì xóa bản ghi chưa chắc có bù trừ ghế. Khi đã gửi payment mà chưa rõ kết quả, UI tạm chặn hủy để tránh đua giữa sự kiện thanh toán và hủy.

## 6. Bản ghi ghế lấy ở đâu?

API BookingResponse trong ZIP không trả booking detail. Sau khi POST thành công, frontend lưu snapshot các ghế đã gửi cùng mã booking, người dùng và môi trường. Lịch sử có snapshot thì hiển thị với nhãn “trên máy này”; không có thì báo thiếu. Trạng thái, mã và tổng tiền vẫn dùng dữ liệu booking từ API. TXT chỉ là bản ghi tham khảo, không tạo QR hoặc giả làm vé rạp.

## 7. Khi thầy hỏi đóng góp

Bạn có thể giải thích chính xác những gì đã tự làm: chạy tích hợp, kiểm tra các ca, tìm/sửa lỗi, điều chỉnh mã, viết biên bản và review. Nói rõ phần mã có AI hỗ trợ nếu được hỏi hoặc quy định môn yêu cầu. Không nhận rằng bản kiểm tra của trợ lý là kết quả bạn đã thực hiện trên máy.

Bảy câu cần tự trả lời được: ID ghế nào được gửi? Khi nào không POST? Vì sao giá đổi phải đồng ý lại? Vì sao PENDING không phải PAID? Mất mạng xử lý ra sao? Hủy trả ghế qua API nào? Snapshot khác dữ liệu backend thế nào?
