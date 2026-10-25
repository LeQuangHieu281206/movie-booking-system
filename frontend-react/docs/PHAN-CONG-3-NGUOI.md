# Phân công 3 người làm frontend CineRoom

## Quyết định đề xuất

Dùng một dự án React chung: `frontend-react`. Chia theo chức năng trọn vẹn, mỗi người chịu trách nhiệm giao diện, API, xử lý lỗi, kiểm thử và trình bày phần của mình. Thay “Frontend 2/3” bằng tên thật khi nhóm chốt phân công.

Bản nền đã có đủ các chức năng dưới đây. Các nhiệm vụ còn lại cần được từng người thực sự thực hiện và ghi nhận; không đánh đồng mã được hỗ trợ tạo sẵn với phần mỗi người tự viết.

## 1. Bảng giao việc

| Người | Sở hữu | File chính | Nhiệm vụ thực tế tiếp theo | Điều kiện nghiệm thu |
|---|---|---|---|---|
| Bạn | Đặt vé, thanh toán và vé | `pages/Booking.jsx`, `pages/Tickets.jsx`, `components/PaymentPanel.jsx`; logic đặt vé trong `lib/domain.js` cần review chung | Chạy luồng thật với backend; kiểm tra ghế trống/ghế bị chiếm; kiểm tra tổng tiền; thử PENDING → PAID, thất bại và hủy; ghi lỗi, sửa lỗi, giải thích request/response | Tạo được booking thật; không chọn ghế đã đặt; chỉ hiển thị PAID khi API xác nhận; chứng minh ghế được trả khi hủy/thất bại bằng cách tải lại API |
| Frontend 2 | Trang chủ và phim | `pages/Catalog.jsx` | Dùng dữ liệu phim thật, kiểm tra tìm không dấu/genre/status; xử lý poster lỗi, mô tả dài, phim không có lịch; tối ưu bố cục; viết kết quả kiểm thử | Trang phim hiển thị đúng dữ liệu API, lọc đúng, mở chi tiết và truyền đúng movieId sang lịch; không vỡ khi dữ liệu thiếu |
| Frontend 3 | Rạp, lịch chiếu và hồ sơ khách | `pages/Venues.jsx`, `components/ScheduleBoard.jsx`, `components/CustomerForm.jsx` | Kiểm tra rạp/thành phố từ dữ liệu thật; ngày giờ Việt Nam; bỏ suất đã bắt đầu/hủy; chọn đúng showtimeId; thử tạo hồ sơ và mã khách đã có; lỗi email trùng; kiểm tra màn hình hẹp | Chọn đúng rạp/ngày/suất; không tráo ID; hồ sơ trả userId thật; không tự đổi thành ADMIN; không dùng mã mẫu lẫn backend |

Mỗi người nên có một biên bản kiểm thử riêng: môi trường, tình huống, kết quả, lỗi đã sửa và link PR. Không chỉ đổi màu hoặc sửa tên để có commit.

## 2. Hợp đồng giữa các phần

| Từ | Sang | Dữ liệu | Quy tắc |
|---|---|---|---|
| Phim | Lịch chiếu | `movieId` | Là ID từ getMovies, không phải chỉ số mảng |
| Lịch chiếu | Đặt vé | `showtimeId` | Đặt vé phải tải getShowtimeById, không tin dữ liệu cũ trên card |
| Hồ sơ | Đặt vé / Vé | `userId` | API trả về; lưu riêng theo demo/live; không thay thế xác thực |
| Sơ đồ ghế | Tạo booking | `showtimeSeatIds[]` | Không gửi seatId vật lý |
| Tạo booking | Thanh toán | bookingId, userId, totalAmount | Dùng booking backend vừa trả về |
| Thanh toán | Trạng thái vé | getBookingById.status | Sự kiện payment SUCCESS chưa đủ để kết luận booking PAID |

Tên API và DTO chi tiết: `API-VA-GIOI-HAN.md`.

## 3. File dùng chung: tránh xung đột

Chọn một người tích hợp (có thể là bạn), không có nghĩa người đó tự nhận toàn bộ công sức.

- `App.jsx`: routing hash, state khách, chế độ dữ liệu, mở modal. Chỉ người tích hợp sửa sau khi thống nhất.
- `styles.css`: giữ token màu, font, nút thống nhất. Sửa selector thuộc chức năng của mình; báo nhóm nếu sửa selector chung. Nếu dự án mở rộng, tách CSS theo chức năng bằng một PR riêng.
- `UI.jsx`: Button/Icon/Modal/Poster/Loading/ErrorBox chung. Không tự tạo thêm ba bộ Button riêng.
- `services/api.js`: mọi gọi API thật tập trung tại đây, không hard-code cổng trong component.
- `lib/domain.js`: quy tắc chọn ghế, tiền vé, ngày giờ. Chỉnh sửa phải chạy lại tests.
- `package.json` và `package-lock.json`: chỉ thêm thư viện khi nhóm thống nhất; commit cả hai.

Không chép đè cả thư mục của nhau sau khi đã bắt đầu làm riêng. Dùng Git và PR để ghép thay đổi.

## 4. Trình tự thực hiện ngay

### Bước A — Đưa bản nền vào nhánh tích hợp

Người tích hợp chép `frontend-react` vào repo đang có. Dùng GitHub Desktop kiểm tra đúng repository và Current branch. Nếu nhóm đang dùng `feature/PVT/frontend` làm nhánh tích hợp frontend, có thể dùng nhánh đó; đích PR cuối cùng do nhóm thống nhất.

Commit bản nền một lần, mô tả trung thực, ví dụ: `chore(frontend): add AI-assisted React SOA baseline`. Push nhánh này lên remote. Không force-push.

Nếu nhánh đó đã có người khác dùng, tạo nhánh tích hợp mới, ví dụ `feature/frontend-integration`, từ nhánh frontend đã thống nhất. Không tự thay nhánh mặc định của repository.

### Bước B — Mỗi người lấy cùng bản nền

Mỗi người Fetch, chọn nhánh tích hợp, Pull phiên bản mới nhất, rồi **tạo nhánh riêng từ nhánh tích hợp đó**:

| Người | Nhánh gợi ý |
|---|---|
| Bạn | `feature/PVT/booking-flow` |
| Frontend 2 | `feature/TEN-BAN-2/movie-catalog` |
| Frontend 3 | `feature/TEN-BAN-3/venues-showtimes` |

Thay TEN-BAN-2/3 bằng tên thật, không dùng chung tên nhánh. GitHub Desktop: Current branch → New branch → nhập tên → Create branch → Publish branch.

### Bước C — Chạy và nhận việc

Mỗi người chạy `npm.cmd ci`, `npm.cmd run dev`; thử demo trước, sau đó Backend thật với Gateway của máy mình. Backend không chạy trên máy một người khác chỉ nhờ có link GitHub.

Mỗi người nhận các tình huống kiểm thử trong bảng giao việc. Khi có lỗi, ghi lại trigger và API liên quan, rồi sửa trong phần sở hữu. Không sửa backend tùy tiện nếu chưa thống nhất với hai bạn phụ trách backend.

### Bước D — Commit công việc thực tế

Commit khi hoàn thành một thay đổi có thể giải thích. Ví dụ sau khi đã làm thật:

- `fix(booking): refresh seats after booking conflict`
- `fix(catalog): handle missing movie poster`
- `test(showtimes): document Vietnam timezone checks`

Không tạo commit rỗng, lùi ngày, đổi author hoặc chia nhỏ giả tạo chỉ để tăng số lượng. Số lượng commit không thay thế chất lượng đóng góp.

### Bước E — PR và review

Push nhánh cá nhân → tạo Pull Request với **base là nhánh tích hợp frontend**, compare là nhánh cá nhân.

Nội dung PR ghi: vấn đề; đã sửa gì; cách kiểm tra; kết quả; hạn chế. Gắn ảnh nếu chính thành viên tự chạy và chụp; dữ liệu nhạy cảm cần được che.

Luân phiên review: bạn review Frontend 2, Frontend 2 review Frontend 3, Frontend 3 review bạn. Reviewer thực sự chạy/đọc thay đổi, chỉ ra lỗi nếu có. Review không có nghĩa tự nhận là tác giả phần đó.

Merge PR theo quy ước nhóm. Dùng “Create a merge commit” nếu muốn giữ các commit gốc và thể hiện lần ghép rõ ràng; squash cũng hợp lệ nếu nhóm đã thống nhất, nhưng lịch sử cá nhân sẽ cần đối chiếu trong PR.

### Bước F — Nghiệm thu chung

Sau merge, cả ba Pull nhánh tích hợp, chạy tests/build và thử luồng phim → suất chiếu → ghế → booking → thanh toán → vé. Một lần kiểm tra cuối trên đúng commit sẽ nộp là đủ nếu không phát sinh thay đổi mới.

Tạo PR từ nhánh tích hợp sang nhánh nộp bài của nhóm. README gốc trỏ tới `frontend-react/README.md`. Bỏ frontend cũ chỉ khi nhóm đồng ý và đã xác nhận không còn dùng.

## 5. Cách bạn trình bày phần quan trọng

Bạn cần tự giải thích được:

1. Vì sao phải ghép getShowtimeSeats với getSeatsByRoomId?
2. Phân biệt seatId và showtimeSeatId.
3. Tại sao tải lại ghế trước POST vẫn chưa giải quyết hoàn toàn hai người đặt cùng lúc? Backend cần khóa/transaction đúng.
4. Vì sao createBooking trả PENDING, payment trả SUCCESS nhưng UI vẫn phải hỏi lại booking?
5. Khi mạng lỗi sau POST, vì sao không tự gửi lại ngay?
6. Hủy booking khác xóa booking ra sao? UI dùng updateBooking CANCELLED để backend có cơ hội trả ghế.
7. Hạn chế hiện tại của bảo mật hồ sơ, giá client và thanh toán giả lập.

Đọc hiểu và kiểm thử được những điểm này giúp phần của bạn có chiều sâu hơn việc chỉ trang trí giao diện.
