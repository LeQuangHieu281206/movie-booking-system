# Biên bản kiểm thử – Frontend 3 (Rạp, Lịch chiếu, Hồ sơ khách)

Phạm vi: `pages/Venues.jsx`, `components/ScheduleBoard.jsx`, `components/CustomerForm.jsx`, cùng các hàm mới `lib/venues.js`, `lib/customer.js`, `lib/useNow.js`.

## 1. Đã kiểm tra tự động (`npm test`: 33/33 đạt)

| Tình huống | File test | Kết quả |
|---|---|---|
| Bỏ suất đã bắt đầu / CANCELLED / ONGOING / thiếu showtimeId / giờ lỗi; không sửa mảng đầu vào | `tests/venues.test.mjs` | Đạt |
| Lọc theo rạp bằng ID (rạp 1 không lẫn rạp 10; number và string đều khớp) | `tests/venues.test.mjs` | Đạt |
| Gom suất theo rạp, giữ đúng `showtimeId`; thiếu `cinemaId` thì tách theo tên | `tests/venues.test.mjs` | Đạt |
| Ngày chiếu theo giờ Việt Nam, qua nửa đêm UTC | `tests/venues.test.mjs` | Đạt |
| Danh sách thành phố: bỏ rỗng, bỏ trùng, sắp tiếng Việt | `tests/venues.test.mjs` | Đạt |
| Ô chọn phim giữ phim đang chọn (sắp chiếu / chưa tải xong) | `tests/venues.test.mjs` | Đạt |
| Mã khách: `12`, `#12` hợp lệ; rỗng, 0, âm, số thập phân, chữ, `1e3` bị từ chối | `tests/customer.test.mjs` | Đạt |
| Tạo hồ sơ: cắt khoảng trắng, email sai, tên toàn khoảng trắng, SĐT sai | `tests/customer.test.mjs` | Đạt |
| Hồ sơ trả về: userId thật, khớp mã đã nhập, không nạp ADMIN, thiếu tên không làm hỏng header | `tests/customer.test.mjs` | Đạt |
| `createCustomer` luôn gửi role `CUSTOMER` dù form có role khác | `tests/customer.test.mjs` | Đạt |
| Thông báo lỗi email trùng (HTTP 409 hoặc chữ "exists/đã tồn tại") và mã không tồn tại (404 / "not found") | `tests/customer.test.mjs` | Đạt |
| Chế độ mẫu: email trùng bị từ chối, mã lạ báo không tìm thấy | `tests/customer.test.mjs` | Đạt |

## 2. Chưa kiểm tra – cần thành viên tự chạy và ghi kết quả

Các mục dưới đây cần trình duyệt và/hoặc backend thật. Điền kết quả vào cột cuối.

| # | Tình huống | Cách thử | Kết quả mong đợi | Kết quả thực tế |
|---|---|---|---|---|
| 1 | Danh sách rạp theo dữ liệu thật | Chế độ Backend thật → `#cinemas` | Số rạp, tên, địa chỉ khớp `GET /api/getCinemas` | |
| 2 | Lọc thành phố | Chọn từng thành phố | Chỉ hiện rạp thuộc thành phố đó; màu thẻ không đổi khi lọc | |
| 3 | Rạp thiếu địa chỉ / SĐT | Dữ liệu có rạp trống `address`, `phoneNumber` | Hiện "Địa chỉ đang được cập nhật", không vỡ bố cục | |
| 4 | Xem lịch theo rạp | Bấm "Xem lịch chiếu" ở một rạp | Hiện tên rạp (không phải `#id`), chỉ suất của rạp đó; "Xem tất cả rạp" bỏ lọc | |
| 5 | Rạp chưa có suất | Chọn rạp không có suất SCHEDULED tương lai | Thông báo rõ + nút "Xem lịch tất cả rạp" | |
| 6 | Chọn đúng suất | Bấm một giờ chiếu → xem tiêu đề, rạp, phòng, giờ trong cửa sổ đặt vé | Khớp với giờ vừa bấm; `getShowtimeById?id=` trong tab Network đúng ID | |
| 7 | Suất đã bắt đầu / hủy | Có suất giờ quá khứ, `CANCELLED`, `ONGOING` trong DB | Không xuất hiện trong lịch | |
| 8 | Suất sắp bắt đầu | Để trang mở qua giờ chiếu của một suất | Tối đa ~1 phút sau suất biến mất | |
| 9 | Phim sắp chiếu | Mở lịch chiếu từ chi tiết phim UPCOMING / mở `#showtimes` rồi chọn phim | Ô chọn phim vẫn hiện đúng tên phim, không bị trống | |
| 10 | Giờ Việt Nam | Suất 23:30 và 00:30 (giờ VN) | Nằm đúng tab ngày VN | |
| 11 | Tạo hồ sơ hợp lệ | Nhập tên, email mới | `POST /api/createUser` có `role: "CUSTOMER"`; thẻ hiển thị `#userId` thật từ backend | |
| 12 | Email trùng | Tạo lại cùng email | Thông báo "Email này đã có hồ sơ…" | |
| 13 | Lấy hồ sơ bằng mã | Nhập mã đúng; nhập `#<mã>` | Hiện đúng hồ sơ | |
| 14 | Mã không tồn tại / sai định dạng | `999999`, `abc`, `1.5` | Báo lỗi rõ, không tạo hồ sơ | |
| 15 | Mẫu và thật không lẫn mã | Tạo hồ sơ ở chế độ mẫu → đổi sang Backend thật | Hồ sơ không bị mang sang (lưu riêng theo chế độ) | |
| 16 | Màn hình hẹp 360–390px | DevTools → thiết bị di động | Không cuộn ngang trang; tab ngày cuộn ngang được; nút suất ≥ 56px cao; chữ ≥ 10px | |

## 3. Lỗi đã sửa (so với bản nền)

1. `Venues.jsx` hiển thị "rạp #id" thay vì tên rạp và không có cách bỏ lọc rạp.
2. Ô chọn phim ở trang Lịch chiếu bị trống khi phim được chọn là UPCOMING hoặc danh mục chưa tải xong.
3. `ScheduleBoard` so sánh ID bằng `===` (number/string có thể không khớp) khi tra tên phim.
4. Danh sách suất không tự cập nhật khi trang mở lâu: suất đã bắt đầu vẫn bấm được.
5. Lọc rạp trong `ScheduleBoard` làm danh sách rạp rỗng không có hướng dẫn; thành phố đã chọn có thể không còn trong dữ liệu mới.
6. Màu thẻ rạp đổi theo vị trí trong danh sách đã lọc.
7. `CustomerForm`: mã khách hàng `1.5`, `-1`, `1e3` không bị chặn; tên toàn khoảng trắng và email sai không có thông báo rõ; email trùng và mã không tồn tại hiện thông báo kỹ thuật của backend.
8. Hồ sơ trả về thiếu `userId`/`fullName` có thể làm hỏng header (`fullName.split`), hoặc nạp tài khoản ADMIN vào luồng khách.
9. Chữ 8–10px trên điện thoại ở nút giờ chiếu, thẻ rạp, thẻ khách; nút thiếu `type="button"`, tab ngày thiếu `aria-pressed`.

## 4. Hạn chế đã biết

- Hồ sơ vẫn chỉ là mã khách hàng, chưa có đăng nhập (xem `API-VA-GIOI-HAN.md`).
- Quy tắc "từ chối hồ sơ ADMIN" dựa trên trường `role` backend trả về; nếu backend dùng tên role khác, cần chỉnh `lib/customer.js`.
- Kiểm tra SĐT/email phía client chỉ giúp báo lỗi sớm; backend vẫn phải tự validate.
