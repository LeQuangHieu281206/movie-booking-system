# Cập nhật README gốc của repository

Giữ các hướng dẫn backend còn đúng. Thay phần frontend scaffold/không còn phù hợp bằng nội dung dưới đây (các liên kết tính từ thư mục gốc repo):

---

## Frontend React — CineRoom

Frontend chính nằm trong `frontend-react`. Giao diện gồm phim, rạp, lịch chiếu, sơ đồ ghế, hồ sơ khách hàng, đặt vé, thanh toán mô phỏng và vé đã đặt.

Yêu cầu Node.js 20.19+ thuộc dòng 20 hoặc 22.12 trở lên. Từ thư mục gốc:

```powershell
cd frontend-react
```

```powershell
npm.cmd ci
```

```powershell
npm.cmd run dev
```

Mở <http://localhost:5173>. Mặc định proxy đến Gateway `http://localhost:8080`; nếu Gateway của máy bạn là cổng khác, cấu hình `BACKEND_URL` trong `.env.local`, rồi khởi động lại Vite. Các service domain vẫn dùng cổng cấu hình trong backend. Docker Compose chỉ khởi động hạ tầng, không tự chạy các Java service.

- [Hướng dẫn chạy và cấu hình frontend](frontend-react/README.md)
- [Phân công ba thành viên frontend](frontend-react/docs/PHAN-CONG-3-NGUOI.md)
- [Hợp đồng API và giới hạn](frontend-react/docs/API-VA-GIOI-HAN.md)
- [Checklist chạy thử, đóng góp và nộp bài](frontend-react/docs/KIEM-THU-VA-NOP-BAI.md)

Có hai chế độ dữ liệu: mẫu cục bộ và backend thật. Thanh toán hiện là mô phỏng, không thu tiền. Hồ sơ khách hàng theo userId chưa thay thế xác thực đăng nhập.

```powershell
npm.cmd test
```

```powershell
npm.cmd run build
```

Các thư mục frontend cũ chỉ giữ để đối chiếu nếu nhóm chưa dọn dẹp. Không chạy đồng thời frontend cũ và React mới ở cùng cổng 5173.
