# Kiểm thử phần PVT

## Kết quả thực hiện trong bản bàn giao 24/10/2026

Các kiểm tra sau do trợ lý thực hiện trên bản mã được giao, không phải biên bản xác nhận người dùng đã thử trên máy mình.

| Kiểm tra | Kết quả |
|---|---|
| Unit test toàn bộ frontend | 40/40 đạt: 17 kiểm tra bản nền + 23 kiểm tra mới |
| Production build | Thành công |
| Trình duyệt, demo: chọn A1/A2, tạo hồ sơ, tạo vé, thanh toán | PENDING → PAID; tổng tiền 220.000đ, snapshot A1/A2 |
| Đóng vé PENDING | Hiện nhắc đóng không tự hủy/trả ghế |
| Vé của tôi: tìm không có kết quả và xóa bộ lọc | Có empty state, khôi phục lại danh sách |
| Chi tiết vé | Đọc lại trạng thái và hiện snapshot ghế đúng nguồn |
| Khung 390px, trang Vé của tôi | Nội dung tải đầy đủ; không tràn ngang toàn trang |
| Harness API giả: giá đổi từ 85.000 lên 100.000 | Hiển thị tổng mới 200.000 cho 2 ghế, bỏ đồng ý cũ; số POST=0 |
| Harness API giả: A2 bị chiếm | A2 bị khóa/bỏ chọn, A1 vẫn giữ, tổng còn 85.000; số POST=0 |
| Harness API giả: mất phản hồi sau createBooking | Chỉ gửi 1 POST; cảnh báo kết quả chưa rõ; tải lại trang vẫn giữ cảnh báo, không tự POST |
| Payment trả SUCCESS nhưng booking PENDING | Unit test giữ PENDING, không tự báo PAID |
| POST payment lỗi và mở lại | Unit test giữ dấu, chặn POST lần hai |
| Sai booking ID/user/code, thao tác song song, unmount | Unit test dừng sai đối tượng, khóa thao tác và dừng poll |

Harness API giả chỉ phục vụ QA, không kết nối Java. Không đưa harness vào bản vá bàn giao.

## Người dùng điền sau khi tự thử backend thật

Họ tên: …
Nhánh/commit: …
Gateway đang dùng: …
Phiên bản Node: …
Ngày thử: …

| Mã | Thử trên backend thật | Kết quả / lỗi / bằng chứng |
|---|---|---|
| PVT-01 | Chọn ghế, thay đổi lựa chọn, tổng tiền | Chưa thực hiện |
| PVT-02 | Một ghế bị khách khác đặt trước khi xác nhận | Chưa thực hiện |
| PVT-03 | Giá hoặc suất đổi, phải xác nhận lại | Chưa thực hiện |
| PVT-04 | Tạo booking thật, ghi bookingCode và ID | Chưa thực hiện |
| PVT-05 | Payment SUCCESS → booking PAID | Chưa thực hiện |
| PVT-06 | Payment FAILED → CANCELLED và ghế AVAILABLE | Chưa thực hiện |
| PVT-07 | Hủy PENDING chưa gửi payment và ghế được trả | Chưa thực hiện |
| PVT-08 | Tìm/lọc lịch sử và tải lại trang | Chưa thực hiện |
| PVT-09 | Mạng lỗi sau POST, không gửi lại ngay | Chưa thực hiện |
| PVT-10 | Kiểm tra ở điện thoại/màn hình hẹp | Chưa thực hiện |

Ghi rõ các lỗi backend chưa giải quyết; không đổi trạng thái trực tiếp trên UI để che lỗi. Chỉ ghi đạt khi đã thực hiện và quan sát kết quả.
