# Báo cáo Nhóm 8 – đoạn sai và đoạn sửa lại

Mỗi mục dưới đây gồm **Đoạn sai** (trích nguyên văn từ `Bản sao của Nhóm 8 - TTCSN(1).md`, bỏ ký tự escape `\_` cho dễ đọc) và **Đoạn đúng** để dán thay thế. Số dòng là vị trí trong file gốc lúc đối chiếu.

---

## 1. 1.5 – Yêu cầu kỹ thuật (dòng 368–371)

**Đoạn sai**

> - Yêu cầu kỹ thuật:
> - Giao diện xây dựng bằng HTML, CSS, JavaScript và Bootstrap; xử lý nghiệp vụ bằng Java Spring Boot.
> - Sử dụng cơ sở dữ liệu MySQL, đảm bảo lưu trữ và truy xuất hiệu quả.
> - Hoạt động ổn định trên Google Chrome, Microsoft Edge và Mozilla Firefox; thuận lợi cho việc mở rộng trong tương lai.

**Đoạn đúng**

> - Yêu cầu kỹ thuật:
> - Giao diện xây dựng bằng **React 19 + Vite 8** (JavaScript/JSX, CSS thuần theo design token), chạy ở cổng 5173 và proxy `/api/**` về API Gateway.
> - Nghiệp vụ xây dựng bằng **Java 21, Spring Boot 3.5.6, Spring Cloud 2025.0.0** (Spring Web MVC, Spring Data JPA, OpenFeign, Spring AMQP).
> - Hệ thống tổ chức theo **kiến trúc hướng dịch vụ (SOA)**: API Gateway cổng 8080 điều hướng tới các service độc lập đăng ký với Eureka; giao tiếp đồng bộ qua OpenFeign, bất đồng bộ qua RabbitMQ.
> - Cơ sở dữ liệu **PostgreSQL trên Supabase**, dùng chung cho các service.
> - Xác thực bằng **JWT stateless (HS256)** gồm access token và refresh token; mật khẩu băm BCrypt; Gateway kiểm tra token và gắn `X-User-Id`, `X-User-Role`, `X-Internal-Token` trước khi chuyển tiếp.
> - Hoạt động ổn định trên Google Chrome, Microsoft Edge và Mozilla Firefox.

---

## 2. 1.6.2 (2) – Hệ quản trị cơ sở dữ liệu (dòng 403–406)

**Đoạn sai**

> Microsoft SQL Server được đề xuất làm hệ quản trị cơ sở dữ liệu theo định hướng của tài liệu mẫu. Trong giai đoạn thiết kế, công cụ này phục vụ xây dựng cấu trúc bảng, xác định kiểu dữ liệu và các ràng buộc cần thiết cho hệ thống:

> - Dữ liệu danh mục: Gồm người dùng, phim, thể loại, phòng chiếu, ghế và suất chiếu; …
> - Dữ liệu giao dịch: Gồm đơn đặt vé, vé và thanh toán. …

**Đoạn đúng**

> Hệ thống dùng **PostgreSQL** triển khai trên **Supabase**. Toàn bộ bảng, kiểu ENUM, khóa ngoại và index được tạo bằng script SQL trong thư mục `database/migrations/`; dữ liệu dùng chung cho mọi service nên các ràng buộc toàn vẹn được giữ ở mức cơ sở dữ liệu. Supabase cung cấp sẵn kết nối TLS qua pooler (cổng 6543), phù hợp với môi trường chạy thử.

> - Dữ liệu danh mục: người dùng, phim, thể loại, cụm rạp, phòng chiếu, ghế, suất chiếu.
> - Dữ liệu giao dịch: đơn đặt vé, chi tiết đơn, trạng thái ghế theo suất chiếu, thanh toán.
> - Ràng buộc: `showtime_seats` unique theo `(showtime_id, seat_id)`; `booking_details` tham chiếu cả `seats` và `showtime_seats`; mỗi booking chỉ có tối đa một payment `PENDING`.

---

## 3. 1.6.2 (3) – Công nghệ giao diện (dòng 408–410)

**Đoạn sai**

> HTML, CSS và JavaScript được định hướng sử dụng để xây dựng giao diện website: trình bày thông tin phim, hiển thị lịch chiếu, hỗ trợ thao tác chọn ghế và thông báo kết quả đặt vé. Giao diện cần dễ sử dụng trên máy tính và điện thoại, phân biệt rõ ghế trống, ghế đang chọn và ghế không còn khả dụng.

**Đoạn đúng**

> Giao diện là ứng dụng **React 19** tạo bằng **Vite**, chia thành các trang Trang chủ, Lịch chiếu, Rạp chiếu, Vé của tôi, Tài khoản và khu Quản trị. Luồng đặt vé gồm 5 bước (phim → thành phố → rạp → ngày → suất chiếu) rồi mở hộp thoại chọn ghế; ghế trống, ghế đang chọn và ghế đã bán được phân biệt bằng màu và nhãn trạng thái. Trạng thái đăng nhập lưu ở `localStorage` (`cineroom-auth`), giao diện hỗ trợ sáng/tối (`cineroom-theme`), đăng nhập Google qua `@react-oauth/google`.

---

## 4. 3.1 – Kiến trúc phần mềm (dòng 630–639)

**Đoạn sai**

> Chương 3 trình bày chi tiết về kiến trúc tổng thể của hệ thống website đặt vé xem phim. Dựa trên các yêu cầu đã phân tích ở Chương 2, chương này tập trung mô tả việc áp dụng mô hình 3-Tier. …

> Hệ thống quản lý và chia sẻ bài viết trực tuyến Edushare được thiết kế dựa trên kiến trúc 3 lớp, bao gồm:

> - Tầng giao diện: … hiển thị thông tin như danh sách bài viết, nội dung chi tiết, chức năng tìm kiếm, đăng nhập/đăng ký …
> - Tầng xử lý nghiệp vụ: Xử lý các chức năng như đăng ký, đăng nhập, tìm kiếm khóa học, xem bài học, kiểm tra quyền truy cập …
> - Tầng dữ liệu: … lưu trữ thông tin như tài khoản người dùng, bài viết, bình luận …

**Đoạn đúng**

> Chương 3 trình bày kiến trúc tổng thể của website đặt vé xem phim CineRoom. Hệ thống được triển khai theo **kiến trúc hướng dịch vụ (SOA)**; về mặt logic vẫn có ba tầng (giao diện, nghiệp vụ, dữ liệu) nhưng tầng nghiệp vụ được chia thành các service độc lập thay vì một khối duy nhất.

> - **Tầng giao diện:** ứng dụng React/Vite chạy cổng 5173, gọi API qua `/api/**`; Vite proxy các request này sang API Gateway.
> - **Tầng Gateway:** `api-gateway` (8080) là điểm vào duy nhất, xác thực JWT, gắn thông tin người dùng vào header nội bộ, kiểm tra quyền theo phương thức + đường dẫn rồi định tuyến tới service qua Eureka.
> - **Tầng nghiệp vụ:** `venue-service` (8083) quản lý rạp/phòng/ghế; `movie-service` (8081) quản lý phim/thể loại/suất chiếu/trạng thái ghế; `booking-service` (8082) quản lý đơn đặt vé và giữ ghế; `user-service` (8084) quản lý tài khoản và phát hành JWT; `payment-service` (8086) tạo QR SePay và xử lý webhook; `notification-service` (8087) nhận sự kiện thanh toán; `eureka-server` (8761) đăng ký và phát hiện service.
> - **Tầng dữ liệu:** một cơ sở dữ liệu **PostgreSQL trên Supabase** dùng chung.

> Giao tiếp giữa các service gồm hai kiểu: **đồng bộ** bằng OpenFeign khi cần phản hồi ngay (kiểm tra người dùng, đọc suất chiếu, giữ/nhả ghế, đọc đơn để đối soát) và **bất đồng bộ** bằng RabbitMQ khi thanh toán hoàn tất: payment publish routing key `payment.completed` lên topic exchange `mb.topic.exchange`, booking và notification mỗi bên có queue riêng nhận sự kiện này.

> Các endpoint `/internal/**` không được mở ở Gateway và chỉ dùng cho gọi service-to-service; các service nghiệp vụ bind `127.0.0.1` nên không truy cập được từ ngoài máy chủ.

---

## 5. 3.1.3 – Giao diện danh sách phim (dòng 665–668)

**Đoạn sai**

> Giao diện trên thể hiện danh sách các bộ phim đang chiếu hoặc sắp chiếu, bao gồm các thông tin: tên phim, thể loại, thời lượng phim. Ngoài ra còn có thanh tìm kiếm phim và bộ lọc thể loại.

###### *Hình 3.5. Giao diện danh sách phim*

**Đoạn đúng**

> Danh sách phim được hiển thị ngay trên **trang chủ**, không còn trang “Phim” riêng. Mỗi thẻ phim gồm poster, tên phim, thể loại và thời lượng; phía trên có ô tìm kiếm theo tên và bộ lọc thể loại. Người dùng bấm vào thẻ để mở trang chi tiết phim hoặc chuyển thẳng sang luồng chọn suất chiếu.

###### *Hình 3.5. Danh sách phim trên trang chủ*

---

## 6. 3.1.7 – Sai chính tả tiêu đề (dòng 684)

**Đoạn sai**

### ***3.1.7. Thiết kế giao diện chi tiết khim***

**Đoạn đúng**

### ***3.1.7. Thiết kế giao diện chi tiết phim***

---

## 7. 2.3.1 – Use case đăng nhập (dòng 486–490, 501)

**Đoạn sai**

> \| Hậu điều kiện thành công \| Người dùng đăng nhập thành công, hệ thống tạo phiên đăng nhập và cấp quyền tương ứng với vai trò Khách hàng hoặc Quản trị viên. \| … \| A4 \| \| Nếu tài khoản bị khóa hoặc vô hiệu hóa, hệ thống thông báo tài khoản hiện không thể đăng nhập. \|

**Đoạn đúng**

> \| Hậu điều kiện thành công \| Người dùng đăng nhập thành công, hệ thống cấp cặp JWT (access token + refresh token) và trả về hồ sơ người dùng kèm vai trò Khách hàng hoặc Quản trị viên. \| … \| A4 \| \| Nếu tài khoản được tạo bằng Google (không có mật khẩu nội bộ), hệ thống báo sai thông tin đăng nhập và người dùng phải dùng nút “Đăng nhập bằng Google”. Hệ thống không có trạng thái khóa tài khoản. \|

Ghi chú: bước 2 sửa “email/tên đăng nhập” thành **email**; bước 7 bỏ “tạo phiên đăng nhập”, thay bằng “phát hành access token và refresh token”.

---

## 8. 2.3.3 – Use case quản lý tài khoản cá nhân (dòng 554)

**Đoạn sai**

> \| 5. Khách hàng nhập/chỉnh sửa các thông tin cá nhân như họ tên, số điện thoại, email, ngày sinh,... \|

**Đoạn đúng**

> \| 5. Khách hàng nhập/chỉnh sửa **họ tên** và **số điện thoại**. Email và vai trò chỉ hiển thị, không sửa được; hệ thống không có chức năng đổi mật khẩu tự phục vụ. \|

---

## 9. Bảng 2.6 – Use case quản lý danh mục phim (dòng 592–597)

**Đoạn sai**

> \| Mô tả \| Use case cho phép khách hàng lựa chọn phim, suất chiếu, ghế ngồi và thực hiện thanh toán để đặt vé xem phim trên website. \| \| Tác nhân (Actor) \| Khách hàng \| \| Tiền điều kiện \| Khách hàng đã đăng nhập vào hệ thống. \| \| Hậu điều kiện \| Vé được đặt thành công, thông tin vé được lưu vào cơ sở dữ liệu và ghế được cập nhật trạng thái đã đặt. \|

**Đoạn đúng**

> \| Mô tả \| Use case cho phép Quản trị viên xem danh sách phim, thêm phim mới, cập nhật thông tin phim (kể cả thể loại, poster, trailer, trạng thái chiếu) và xóa phim không còn sử dụng. \| \| Tác nhân (Actor) \| Quản trị viên \| \| Tiền điều kiện \| Quản trị viên đã đăng nhập và có vai trò ADMIN. \| \| Hậu điều kiện \| Phim được thêm/cập nhật/xóa trong cơ sở dữ liệu; danh sách phim trên trang chủ và trang quản trị được làm mới. \|

---

## 10. 1.4 – Bảng yêu cầu chức năng (dòng 329–353)

**Đoạn sai**

> \| \| Cập nhật thông tin cá nhân \| Cho phép cập nhật họ tên, email, số điện thoại và đổi mật khẩu \| \| \| Quản lý tài khoản người dùng \| Cho phép Admin xem danh sách, tìm kiếm, xem và cập nhật thông tin, khóa/mở khóa và xóa tài khoản người dùng \| \| \| Quản lý rạp chiếu (Admin) \| … gồm tên rạp, địa chỉ, thông tin liên hệ và trạng thái hoạt động \| \| \| Quản lý phòng chiếu (Admin) \| … thiết lập số lượng ghế, sơ đồ ghế và phân loại ghế (thường, VIP...) \| \| \| Tạo vé điện tử \| … gồm mã vé, tên phim, rạp, phòng chiếu, ngày giờ chiếu, số ghế, giá vé và mã QR/mã xác nhận \| \| \| Xem vé và lịch sử đặt vé \| … trạng thái từng vé (đã đặt, đã thanh toán, đã sử dụng, đã hủy...) \|

**Đoạn đúng**

> \| \| Cập nhật thông tin cá nhân \| Cho phép cập nhật họ tên và số điện thoại; email chỉ đọc, không có chức năng đổi mật khẩu tự phục vụ (Admin đặt lại mật khẩu khi cần) \| \| \| Quản lý tài khoản người dùng \| Cho phép Admin xem danh sách, tìm kiếm, xem, cập nhật thông tin (họ tên, số điện thoại, vai trò, mật khẩu) và xóa tài khoản. Hệ thống không có cột trạng thái nên không hỗ trợ khóa/mở khóa \| \| \| Quản lý rạp chiếu (Admin) \| Cho phép Admin thêm, sửa, xóa rạp gồm tên, địa chỉ, thành phố, số điện thoại. Bảng rạp không có trường trạng thái hoạt động \| \| \| Quản lý phòng chiếu (Admin) \| Cho phép Admin thêm, sửa, xóa phòng gồm rạp, tên phòng, tổng số ghế, loại phòng; ghế được tạo riêng theo hàng, số, loại (NORMAL/VIP/COUPLE) chứ chưa có công cụ vẽ sơ đồ ghế \| \| \| Tạo vé điện tử \| Tự động sinh `bookingCode` sau khi thanh toán thành công; vé hiển thị mã vé, phim, rạp, phòng, ngày giờ, ghế, giá và trạng thái. Vé không có mã QR soát vé; QR chỉ dùng cho thanh toán chuyển khoản \| \| \| Xem vé và lịch sử đặt vé \| … trạng thái PENDING (chờ thanh toán), PAID (đã thanh toán), CANCELLED (đã hủy), EXPIRED (hết hạn). Không có trạng thái “đã sử dụng” \|

Bổ sung hai dòng vào nhóm Tài khoản/Thanh toán:

> \| \| Đăng nhập bằng Google \| Cho phép đăng nhập/đăng ký nhanh bằng tài khoản Google qua ID Token; tài khoản mới có `auth_provider = GOOGLE` \| \| \| Xác nhận tiền mặt (Admin) \| Admin xác nhận đã thu tiền tại quầy cho vé CASH đang chờ thanh toán; vé chuyển PENDING → PAID và giữ nguyên ghế \|

---

## 11. 4.1 – Quy ước chung (dòng 722, 735–736)

**Đoạn sai**

> \| Mã thành công \| 200 OK (đọc/cập nhật), 201 Created (tạo mới), 204 No Content (xoá) \| … \| 404 \| Không tìm thấy tài nguyên; path không khớp rule gateway; truy cập tài nguyên của người khác (che giấu sự tồn tại) \|

**Đoạn đúng**

> \| Mã thành công \| 200 OK (đọc/cập nhật; xóa user và xóa booking trả kèm chuỗi thông báo), 201 Created (tạo mới), 204 No Content (xóa movie, genre, cinema, room, seat, showtime) \| … \| 403 \| Đã xác thực nhưng sai role; truy cập tài nguyên của người khác; đường dẫn không khớp rule nào của Gateway (mặc định chỉ ADMIN) \| \| 404 \| Không tìm thấy tài nguyên theo id \|

---

## 12. 4.2.1 – Bảng API xác thực (dòng 745–753)

**Đoạn sai** (thiếu Google, và mô tả `PUT /api/users/me` chung chung)

> \| POST \| /api/auth/register \| Public \| … \| 201 {id, email, fullName, role} \| \| POST \| /api/auth/login \| Public \| … \| 200 {accessToken, refreshToken, tokenType:"Bearer", expiresIn, user} \| \| POST \| /api/auth/refresh \| Public \| {refreshToken} \| 200 cặp token mới + user \| \| GET \| /api/users/me \| CUSTOMER/ADMIN \| \| 200 {userId, fullName, email, phoneNumber, role} \| \| PUT \| /api/users/me \| CUSTOMER/ADMIN \| {fullName, phoneNumber?} \| 200 hồ sơ sau cập nhật \|

**Đoạn đúng** (thêm một dòng, sửa một dòng)

> \| POST \| /api/auth/register \| Public \| {fullName, email, password (≥8), phoneNumber?} \| 201 {id, email, fullName, role} \| \| POST \| /api/auth/login \| Public \| {email, password} \| 200 {accessToken, refreshToken, tokenType:"Bearer", expiresIn, user} \| \| POST \| /api/auth/google \| Public \| {idToken} \| 200 AuthResponse; tự tạo tài khoản `auth_provider=GOOGLE` nếu email chưa tồn tại \| \| POST \| /api/auth/refresh \| Public \| {refreshToken} \| 200 cặp token mới + user \| \| GET \| /api/users/me \| CUSTOMER/ADMIN \| \| 200 {userId, fullName, email, phoneNumber, role} \| \| PUT \| /api/users/me \| CUSTOMER/ADMIN \| {fullName, phoneNumber?} \| 200 hồ sơ sau cập nhật (email, mật khẩu, vai trò không đổi được ở đây) \|

---

## 13. 4.2.3 – Suất chiếu và ghế theo suất (dòng 789)

**Đoạn sai**

> \| GET \| /api/getShowtimeSeats?ShowtimeId=? \| Public \| \| 200 ShowtimeSeatView\[\] \|

**Đoạn đúng**

> \| GET \| /api/getShowtimeSeats?showtimeId= \| Public \| \| 200 ShowtimeSeatView\[\] \|

---

## 14. 4.2.4 – Bảng API rạp, phòng, ghế (dòng 809, 811, 816)

**Đoạn sai**

> \| GET \| /api/get/cities \| Public \| \| 200 danh sách thành phố \| \| POST \| /api/createCinema \| ADMIN \| {name, address, city, phonenumber?} \| 201 rạp \| \| POST \| /api/createRoom \| ADMIN \| {cinemaId, name, totalSeats (\>0), roomType} \| \|

**Đoạn đúng**

> \| GET \| /api/getCities \| Public \| \| 200 danh sách thành phố \| \| POST \| /api/createCinema \| ADMIN \| {name, address, city, phoneNumber?} \| 201 rạp \| \| POST \| /api/createRoom \| ADMIN \| {cinemaId, name, totalSeats (\>0), roomType} \| 201 phòng \|

---

## 15. 4.2.5 – Bảng API đặt vé (dòng 835–840)

**Đoạn sai**

> \| GET \| /api/getBookingsStat \| ADMIN \| \| 200 {revenue, paidBookings, ticketsSold, dailyRevenue\[7\]} \| … \| PUT \| /api/updateBooking?id= \| CUSTOMER/ADMIN \| {status} \| 200 BookingResponse \|

**Đoạn đúng**

> \| GET \| /api/getBookingStats?period= \| ADMIN \| period = day (mặc định) \| month \| year \| 200 {revenue, paidBookings, ticketsSold, period, revenueSeries:\[{key, revenue, paidBookings}\]} \| … \| POST \| /api/confirmCashPayment?id= \| ADMIN \| \| 200 BookingResponse: PENDING → PAID cho vé CASH đã thu tiền tại quầy \| \| PUT \| /api/updateBooking?id= \| CUSTOMER/ADMIN \| {status} \| 200 BookingResponse; chỉ nhận PENDING→CANCELLED (khách/Admin) và PENDING→EXPIRED (Admin); gửi PAID trả 409 \|

Ghi chú thêm dưới bảng:

> Booking `CASH` không bị scheduler cho hết hạn để khách kịp ra quầy; vé chỉ chuyển `PAID` khi Admin bấm “Xác nhận tiền mặt”.

---

## 16. 4.2.6 – Thanh toán (dòng 843–851)

**Đoạn sai**

> \| POST \| /api/createPayment \| CUSTOMER/ADMIN \| {bookingId} \| 201 {qrUrl, message} \| \| POST \| /api/sepayWebhook \| Public + API key \| SePayWebhookDto \| 200 chuỗi xác nhận; 401 sai key; 400 lỗi xử lý \|

**Đoạn đúng** (giữ nguyên hai dòng, thêm ghi chú)

> \| POST \| /api/createPayment \| CUSTOMER/ADMIN \| {bookingId} \| 201 {qrUrl, message} \| \| POST \| /api/sepayWebhook \| Public + API key \| SePayWebhookDto \| 200 chuỗi xác nhận; 401 sai key; 400 lỗi xử lý \|

> Hệ thống chỉ hỗ trợ hai phương thức: `CASH` (trả tại quầy) và `BANK_TRANSFER` (quét VietQR qua SePay). Không hỗ trợ thẻ và ví điện tử. Với `BANK_TRANSFER`, booking chỉ chuyển `PAID` sau khi webhook SePay xác nhận đúng nội dung và số tiền.

---

## 17. 4.2.7 – API nội bộ (dòng 858–866)

**Đoạn sai**

> \| GET \| /internal/showtimes/{showtimeId}/exists \| movie \| kiểm tra suất chiếu \| \| POST \| /internal/showtime-seats/hold \| movie \| chốt giữ ghế (body SeatIdsRequest) \| … (không có endpoint ghế theo id)

**Đoạn đúng**

> \| GET \| /internal/users/{userId}/exists \| user \| booking kiểm tra user tồn tại \| \| POST \| /internal/showtime-seats/hold \| movie \| giữ ghế (body `SeatIdsRequest`) \| \| POST \| /internal/showtime-seats/release \| movie \| nhả ghế khi hủy/hết hạn \| \| GET \| /internal/rooms/{roomId} \| venue \| lấy phòng chiếu \| \| GET \| /internal/rooms/{roomId}/seats \| venue \| lấy ghế của phòng \| \| GET \| /internal/rooms/by-ids?ids= \| venue \| lấy nhiều phòng \| \| POST \| /internal/seats/by-ids \| venue \| lấy nhiều ghế theo id \| \| GET \| /internal/bookings/{bookingId} \| booking \| payment đọc booking để đối soát \|

> Không có endpoint `/internal/showtimes/{id}/exists`: booking-service đọc suất chiếu qua `MovieClient` gọi `GET /api/getShowtimeById`.

---

## 18. 2.3 / Hình 4.10 – Bảng giao tiếp giữa các dịch vụ (dòng 872–880)

**Đoạn sai**

> \| Đồng bộ \| OpenFeign \| booking → user \| Kiểm tra user tồn tại \| \| Đồng bộ \| OpenFeign \| booking → movie \| Đọc suất chiếu: giữ/nhả/chốt ghế \| \| Đồng bộ \| OpenFeign \| booking → venue \| Kiểm tra phòng chiếu tồn tại \| \| Đồng bộ \| OpenFeign \| payment → booking \| Đọc booking để kiểm tra quyền và đối soát \| \| Bất đồng bô \| RabbitMQ topic payment, completed \| payment → booking, payment → notification \| Xác nhận/Huỷ vé và gửi thông báo \|

**Đoạn đúng**

> \| Đồng bộ \| OpenFeign \| booking → user \| kiểm tra user tồn tại \| \| Đồng bộ \| OpenFeign \| booking → movie \| đọc suất chiếu, giữ/nhả ghế nội bộ \| \| Đồng bộ \| OpenFeign \| booking → venue \| kiểm tra phòng và lấy ghế theo id \| \| Đồng bộ \| OpenFeign \| movie → venue \| lấy phòng/ghế khi tạo suất chiếu và trả sơ đồ ghế \| \| Đồng bộ \| OpenFeign \| payment → booking \| đọc booking để kiểm tra quyền và đối soát \| \| Bất đồng bộ \| RabbitMQ topic `mb.topic.exchange`, routing key `payment.completed` \| payment → booking \| đặt PAID/CANCELLED, trả ghế \| \| Bất đồng bộ \| RabbitMQ topic `mb.topic.exchange`, routing key `payment.completed` \| payment → notification \| ghi nhận sự kiện; hiện chỉ ghi log, chưa gửi email thật \|

---

## 19. 5.2.2 – Bảng thuộc tính `users` (dòng 921–931)

**Đoạn sai**

> \| user\_id \| INT8 \| Mã định danh duy nhất của người dùng \| Khóa chính (PK), Identity (tự tăng) \| \| full\_name \| TEXT \| Họ và tên đầy đủ của người dùng \| Not Null \| \| email \| TEXT \| Địa chỉ email dùng để đăng nhập \| Not Null, Unique \| \| password\_hash \| TEXT \| Mật khẩu đã được mã hóa (BCrypt) \| Not Null \| \| phone\_number \| VARCHAR(12) \| Số điện thoại liên hệ \| Có thể Null \| \| role \| user\_role\_enum \| Phân quyền truy cập (CUSTOMER, ADMIN) \| Not Null, Default 'CUSTOMER' \| \| created\_at \| TIMESTAMPTZ \| Thời điểm khởi tạo tài khoản \| Not Null, Default now() \| \| updated\_at \| TIMESTAMPTZ \| Thời điểm cập nhật thông tin gần nhất \| Not Null, Default now() \|

**Đoạn đúng**

> \| user\_id \| INT8 \| Mã định danh duy nhất của người dùng \| Khóa chính (PK), Identity (tự tăng) \| \| full\_name \| TEXT \| Họ và tên đầy đủ của người dùng \| Not Null \| \| email \| TEXT \| Địa chỉ email dùng để đăng nhập \| Not Null, Unique \| \| password\_hash \| TEXT \| Mật khẩu đã được mã hóa (BCrypt) \| **Có thể Null** (tài khoản đăng nhập Google) \| \| auth\_provider \| VARCHAR(20) \| Nguồn tạo tài khoản \| Not Null, Default 'LOCAL', CHECK IN ('LOCAL','GOOGLE') \| \| google\_id \| VARCHAR(255) \| Mã tài khoản Google \| Có thể Null, Unique \| \| phone\_number \| VARCHAR(12) \| Số điện thoại liên hệ \| Có thể Null \| \| role \| user\_role\_enum \| Phân quyền truy cập (CUSTOMER, ADMIN) \| Not Null, Default 'CUSTOMER' \| \| created\_at \| TIMESTAMPTZ \| Thời điểm khởi tạo tài khoản \| Not Null, Default now() \| \| updated\_at \| TIMESTAMPTZ \| Thời điểm cập nhật thông tin gần nhất \| Not Null, Default now() \|

---

## 20. 5.2.2 – Ba dòng sai trong `showtime_seats` và `bookings` (dòng 1048, 1066–1067)

**Đoạn sai**

> \| hold\_by \| INT8 \| Mã người dùng đang giữ chỗ \| Có thể Null, FK tham chiếu users(user\_id) \| … \| payment\_method \| payment\_method\_enum \| Phương thức thanh toán (CASH, BANK\_TRANSFER, CARD, E\_WALLET) \| Có thể Null (chọn khi thanh toán) \| \| booking\_time \| TIMESTAMPTZ \| Thời điểm khách chốt đặt vé \| Có thể Null \|

**Đoạn đúng**

> \| hold\_by \| TEXT \| Mã người dùng đang giữ chỗ \| Có thể Null, **không có khóa ngoại** (service tự kiểm tra và đặt NULL khi nhả ghế) \| … \| payment\_method \| payment\_method\_enum \| Phương thức thanh toán (CASH, BANK\_TRANSFER) \| Có thể Null; enum chỉ có CASH và BANK\_TRANSFER \| \| booking\_time \| TIMESTAMPTZ \| Thời điểm khách chốt đặt vé \| Not Null, Default now() \|

---

## 21. 5.2.2 – Bảng `payments` (dòng 1095–1097) và bảng enum (dòng 1122–1123)

**Đoạn sai**

> \| currency \| TEXT \| Đơn vị tiền tệ \| Not Null, Default 'VND' \| \| status \| payment\_status\_enum \| Trạng thái giao dịch (PENDING, SUCCESS, FAILED) \| Not Null, Default 'PENDING' \| \| provider \| TEXT \| Đối tác xử lý thanh toán (ví dụ SePay) \| Có thể Null \| \| provider\_txn\_id \| TEXT \| Mã giao dịch phía đối tác \| Có thể Null, Unique \| … \| payment\_method\_enum \| CASH, BANK\_TRANSFER \| bookings.payment\_method \| \| payment\_status\_enum \| PENDING, SUCCESS, FAILED \| payments.status \|

**Đoạn đúng**

> \| currency \| VARCHAR(8) \| Đơn vị tiền tệ \| Not Null, Default 'VND' \| \| status \| payment\_status\_enum \| Trạng thái giao dịch (PENDING, SUCCESS, FAILED, CANCELLED, EXPIRED) \| Not Null, Default 'PENDING' \| \| provider \| VARCHAR(32) \| Đối tác xử lý thanh toán \| Not Null, Default 'SEPAY' \| \| provider\_txn\_id \| TEXT \| Mã giao dịch phía đối tác \| Có thể Null; unique theo cặp (provider, provider\_txn\_id) \|

> Giữ cả hai dòng trong bảng enum. Có 10 enum native: `user_role, age_restriction, movie_status, room_type, seat_type, showtime_status, seat_status, booking_status, payment_method_enum (CASH, BANK_TRANSFER), payment_status_enum (PENDING, SUCCESS, FAILED, CANCELLED, EXPIRED)`.

> Bổ sung: mỗi `booking_id` chỉ có tối đa một payment `PENDING` (partial unique index `uk_payments_one_pending_per_booking`).

---

## 22. 5.2.3 – Quan hệ 11 (dòng 1168–1170)

**Đoạn sai**

> 11. Quan hệ Ghế vật lý – Chi tiết đơn đặt vé (1 – n)
> - Mô tả: Mỗi dòng chi tiết tham chiếu trực tiếp tới ghế vật lý và tới dòng trạng thái ghế theo suất. Composite FK đảm bảo hai tham chiếu này luôn khớp nhau.
> - Liên kết: seats.seat\_id (1) ⟷ booking\_details.seat\_id (n); cặp (showtime\_seats.showtime\_seat\_id, showtime\_seats.seat\_id) ⟷ (booking\_details.showtime\_seat\_id, booking\_details.seat\_id).

**Đoạn đúng**

> 11. Quan hệ Ghế vật lý – Chi tiết đơn đặt vé (1 – n) và Trạng thái ghế theo suất – Chi tiết đơn đặt vé (1 – n)
> - Mô tả: mỗi dòng chi tiết lưu đồng thời `seat_id` (ghế vật lý) và `showtime_seat_id` (dòng trạng thái ghế theo suất) để truy vết và trả ghế. Hai cột do service ghi theo cùng một ghế; cơ sở dữ liệu chỉ bảo đảm mỗi giá trị tồn tại ở bảng tương ứng bằng hai khóa ngoại độc lập, **không dùng composite FK**.
> - Liên kết: `seats.seat_id (1) ⟷ booking_details.seat_id (n)`; `showtime_seats.showtime_seat_id (1) ⟷ booking_details.showtime_seat_id (n)`.
> - Ràng buộc xóa: NO ACTION.

---

## 23. 5.3 – Kết luận chương 5 (dòng 1181)

**Đoạn sai**

> Các khóa ngoại, composite FK, unique constraint và index được thiết kế để bảo đảm toàn vẹn dữ liệu, chống đặt trùng ghế và hỗ trợ các truy vấn chính của hệ thống.

**Đoạn đúng**

> Các khóa ngoại, ràng buộc unique, CHECK và index được thiết kế để bảo đảm toàn vẹn dữ liệu, chống đặt trùng ghế (unique `(showtime_id, seat_id)`), chống ghi trùng webhook (unique `(provider, provider_txn_id)`) và hỗ trợ các truy vấn chính của hệ thống.

---

## 24. Mục lục và đánh số chương (dòng 97–101)

**Đoạn sai**

> [**CHƯƠNG 4: THIẾT KẾ VÀ CÀI ĐẶT CƠ SỞ DỮ LIỆU 50**](#chương-5:-thiết-kế-và-cài-đặt-cơ-sở-dữ-liệu) [4.1. Thiết kế cơ sở dữ liệu 50](#5.1.-thiết-kế-cơ-sở-dữ-liệu) [4.2. Đặc tả yêu cầu cơ sở dữ liệu 51](#5.2.-đặc-tả-yêu-cầu-cơ-sở-dữ-liệu) [TÀI LIỆU THAM KHẢO 63](#tài-liệu-tham-khảo)

**Đoạn đúng**

> [**CHƯƠNG 4: THIẾT KẾ API VÀ GIAO TIẾP DỊCH VỤ 50**](#chương-4:-thiết-kế-api-và-giao-tiếp-dịch-vụ) [4.1. Quy ước chung 50](#4.1.-quy-ước-chung) [4.2. Danh sách endpoint 51](#4.2.-danh-sách-endpoint) [**CHƯƠNG 5: THIẾT KẾ VÀ CÀI ĐẶT CƠ SỞ DỮ LIỆU 60**](#chương-5:-thiết-kế-và-cài-đặt-cơ-sở-dữ-liệu) [5.1. Thiết kế cơ sở dữ liệu 60](#5.1.-thiết-kế-cơ-sở-dữ-liệu) [5.2. Đặc tả yêu cầu cơ sở dữ liệu 61](#5.2.-đặc-tả-yêu-cầu-cơ-sở-dữ-liệu) [5.3. Kết luận chương 71](#5.3.-kết-luận-chương) [TÀI LIỆU THAM KHẢO 72](#tài-liệu-tham-khảo)

Kèm theo:

- Đổi `Hình 4.1. Biểu đồ thực thể liên kết` (chương CSDL) thành **Hình 5.1**; `Bảng 4.1…4.13` thành **Bảng 5.1…5.13**.
- Trong chương 4, các bảng đang ghi “Hình 4.1 Quy ước chung”, “Hình 4.2 Bảng mã lỗi”, “Hình 4.10 Danh sách api của authentication service” phải đổi thành **Bảng 4.1, Bảng 4.2, Bảng 4.10**.
- Bảng use case quản lý danh mục phim đổi nhãn thành **Bảng 2.6** (hiện đang trùng “Bảng 2.5” với bảng đặt vé).

---

## 25. Các lỗi gõ cần sửa

| Đoạn sai | Đoạn đúng |
| :--- | :--- |
| **RooomRequeest** (4.2.4) | **RoomRequest** |
| đặt vé xem **phm** (2.5) | đặt vé xem **phim** |
| Use case **uho** phép (2.3.2) | Use case **cho** phép |
| địa **chí** rạp (3.1.4) | địa **chỉ** rạp |
| **Bất đồng bô** (4.3) | **Bất đồng bộ** |

---

## 26. Việc phải làm thêm để báo cáo khớp sản phẩm

Sửa chữ là chưa đủ với 6 mục sau, vì báo cáo đang hứa tính năng chưa có. Chọn một trong hai:

**A. Sửa báo cáo cho đúng code** (đã áp ở mục 10, 16): bỏ QR soát vé, email vé, ví điện tử/thẻ, khóa tài khoản, trạng thái rạp, trạng thái “đã sử dụng”.

**B. Bổ sung code cho đúng báo cáo**:

1. Sinh QR từ `bookingCode` ở trang Vé của tôi + màn hình kiểm tra vé cho Admin.
2. Gửi email thật ở `notification-service` (hiện chỉ `log.info`).
3. API đổi mật khẩu/email tự phục vụ.
4. Cột trạng thái + khóa/mở khóa tài khoản.
5. Cột trạng thái hoạt động cho rạp.
6. Công cụ xếp sơ đồ ghế cho phòng chiếu.

Ảnh 3.2–3.11 cũng phải chụp lại: menu không còn mục “Phim”, lịch chiếu là wizard 5 bước, bộ lọc thành phố/thể loại là dropdown tuỳ biến, vé hiển thị trạng thái “Chờ thanh toán” và nút “Xác nhận tiền mặt” ở trang quản trị.
