# Phần PVT — chọn ghế, đặt vé, thanh toán và vé đã đặt

Bản cập nhật ngày 24/10/2026, dựa trên `CineRoom-React-SOA.zip` đã bàn giao ngày 22/10. Mã được hỗ trợ hoàn thiện bằng AI theo yêu cầu người dùng. Nhóm cần chạy thử với backend thật và hiểu phần mã trước khi trình bày.

## Đã hoàn thiện ở frontend

| Vấn đề / nhu cầu | Hành vi mới |
|---|---|
| Ghế bị khách khác chiếm trước khi tiếp tục hoặc xác nhận | Tải lại dữ liệu; bỏ ghế không còn trống, giữ ghế còn trống, quay lại chọn ghế; chưa POST booking |
| Giá, giờ, phim hoặc phòng thay đổi | Cập nhật thông tin mới, bỏ đồng ý cũ và yêu cầu kiểm tra lại trước khi gửi |
| Người dùng chọn lâu | Sơ đồ đọc lại sau 30 giây hoặc khi quay lại cửa sổ; đọc lỗi thì tạm chặn gửi đến khi cập nhật được |
| Muốn xem lựa chọn nhanh | Thống kê ghế trống/giữ/đặt, nhãn HELD riêng, nút bỏ chọn từng ghế trong tóm tắt |
| Gửi booking rồi mất phản hồi | Ghi nhớ yêu cầu chưa rõ kết quả theo chế độ/khách/suất; không tự gửi lại sau tải trang; có bước đọc lịch sử gần đây để đối chiếu |
| Cần thử lại booking chưa rõ kết quả | Chỉ mở lại khi không tìm thấy booking gần thời điểm gửi, đã chờ ít nhất 30 giây và người dùng tự xác nhận; không khẳng định tuyệt đối chưa có booking |
| Payment báo SUCCESS nhưng booking chưa cập nhật | Đọc booking, giữ PENDING đến khi chính booking API trả trạng thái cuối |
| Tải lại hoặc mở lại chi tiết sau khi đã gửi payment/cancel | Ghi nhớ dấu thao tác theo userId + bookingId + bookingCode + chế độ; không tự gửi lặp |
| Hai component cùng thao tác một vé | Khóa thao tác trong cùng ứng dụng; đọc lại booking trước POST/PUT |
| Backend trả sai booking | Kiểm tra ID, mã, khách, suất và status; dừng thao tác nếu không khớp |
| Đóng booking đang chờ | Nhắc rằng đóng không tự hủy/trả ghế; cho tiếp tục, mở lịch sử hoặc để chờ |
| Tìm lịch sử vé | Lọc trạng thái có số lượng, tìm theo mã/tên phim không dấu, sắp xếp mới/cũ, xóa bộ lọc |
| Xem lại ghế từ lần đặt trên máy này | Lưu snapshot tối thiểu sau khi nhận booking thành công; đọc theo đúng môi trường/khách/mã; tải bản ghi TXT |

## File được thay

- `src/pages/Booking.jsx`
- `src/pages/Tickets.jsx`
- `src/components/PaymentPanel.jsx`

## File mới

- `src/booking/flow.js`: kiểm tra dữ liệu mới trước khi gửi, kiểm tra booking, lọc lịch sử, nội dung bản ghi.
- `src/booking/storage.js`: dấu yêu cầu và snapshot cục bộ, xử lý storage bị chặn.
- `src/booking/payment.js`: trình tự payment/cancel, khóa và polling.
- `src/booking/useInventory.js`: tải/cập nhật/huỷ các lần đọc ghế cũ.
- `src/booking/TicketReceipt.jsx`: hiển thị/tải thông tin đặt vé.
- `src/booking/booking.css`: CSS riêng cho phần PVT.
- `tests/pvt-booking.test.mjs`: 23 kiểm tra bổ sung.
- Ba tài liệu `PVT-*.md` trong docs.

Không sửa App, Catalog, Venues, ScheduleBoard, CustomerForm, API adapter, styles.css chung, package hoặc Java. Bản vá không thêm thư viện.

## Chạy

Tại `E:\ProjectGH\movie-booking-system\frontend-react`:

```powershell
npm.cmd test
```

```powershell
npm.cmd run build
```

```powershell
npm.cmd run dev
```

Dùng `npm.cmd ci` nếu máy chưa cài thư viện. Cổng frontend/Gateway giữ cấu hình hiện tại của dự án; mặc định 5173/8088. Xem `README.md` của bản nền nếu cần khởi động backend.

## Cách thử phần mới

1. Demo: chọn phim, suất tương lai, 2 ghế; quan sát thống kê và tổng tiền; bỏ/chọn lại ghế.
2. Tạo/lấy hồ sơ, xác nhận; nhận PENDING. Bấm Đóng để thấy lời nhắc.
3. Mô phỏng thành công; phải thấy PAID. Mở Vé của tôi, tìm mã, lọc Đã thanh toán, mở chi tiết để xem ghế và tải TXT.
4. Tạo vé khác rồi thử FAILED hoặc hủy PENDING chưa thanh toán; trạng thái phải trả CANCELLED, sơ đồ mẫu trả ghế.
5. Backend thật: làm lại, kiểm tra các request trong Network và log Java. Dùng dữ liệu thử.
6. Nếu mạng lỗi sau POST, không gửi liên tục. Dùng kiểm tra booking gần đây và Vé của tôi. Dù không tìm thấy, việc thử lại vẫn có thể tạo trùng nếu server xử lý chậm.

## Giới hạn còn thuộc backend / môi trường

- Chưa kiểm chứng end-to-end với các service Java, RabbitMQ và database thật trên máy nhóm trong phiên bàn giao này.
- Frontend revalidate không thay thế khóa ghế, transaction, kiểm tra giá, quyền người dùng hay idempotency phía server.
- Khóa module chỉ ngăn đồng thời trong cùng ứng dụng; localStorage giúp giữ dấu qua tải lại. Hai trình duyệt/máy khác nhau vẫn cần backend bảo vệ. Nếu storage bị chặn/xóa, dấu có thể chỉ còn trong bộ nhớ phiên hiện tại.
- Thanh toán mô phỏng không thu tiền. Không dùng để giao dịch thật.
- Snapshot ghế là bản ghi trên máy đã đặt, không phải API chi tiết vé. Trình duyệt khác hoặc vé cũ không có snapshot sẽ thông báo thiếu; không tự dựng ghế giả.
- Đóng cửa sổ không hủy booking; backend chưa có TTL hoàn chỉnh thì vé chờ cần xử lý bằng luồng hủy phù hợp.
- API hồ sơ hiện chưa có xác thực thực sự; không coi kiểm tra ID trên frontend là bảo mật backend.

## Commit / PR gợi ý

Sau khi áp dụng và tự thử, commit phần thay đổi thực tế với tiêu đề:

`feat(booking): handle stale seats and payment recovery`

PR về nhánh tích hợp `feature/PVT/frontend` từ nhánh cá nhân `feature/PVT/booking-flow` (hoặc nhánh nhóm đã thống nhất). Mô tả: ghế/giá thay đổi được review trước gửi, dấu yêu cầu chưa rõ kết quả, lịch sử tìm/lọc và bản ghi ghế; 40 unit tests/build đạt trong bản bàn giao; ghi riêng kết quả backend thật của chính bạn.
