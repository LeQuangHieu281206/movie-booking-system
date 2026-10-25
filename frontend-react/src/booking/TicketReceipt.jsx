import React from "react";
import { bookingStore } from "./storage.js";
import { readableDate, receiptText } from "./flow.js";
import { Button } from "../components/UI.jsx";

export default function TicketReceipt({ mode, booking }) {
  const receipt = bookingStore.receipt(mode, booking);
  function download() {
    const blob = new Blob(["\uFEFF", receiptText(booking, receipt, mode)], {
      type: "text/plain;charset=utf-8",
    });
    const url = URL.createObjectURL(blob),
      link = document.createElement("a");
    link.href = url;
    link.download = `CineRoom-${String(booking.bookingCode).replace(/[^a-z0-9-]/gi, "")}.txt`;
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  return (
    <section className="pvt-receipt">
      <h3>Thông tin đặt vé</h3>
      {receipt ? (
        <>
          <dl>
            <dt>Phim lúc đặt</dt>
            <dd>{receipt.title}</dd>
            <dt>Rạp / phòng</dt>
            <dd>
              {receipt.cinemaName} · {receipt.roomName}
            </dd>
            <dt>Suất lúc đặt</dt>
            <dd>{readableDate(receipt.startTime)}</dd>
            <dt>Ghế đã chọn</dt>
            <dd>{receipt.seats.map((seat) => seat.label).join(", ")}</dd>
          </dl>
          <p className="hint">
            Thông tin ghế được lưu trên trình duyệt này lúc tạo booking. Trạng
            thái và tổng tiền được đọc từ booking hiện tại.
          </p>
        </>
      ) : (
        <p className="hint">
          Chưa có bản ghi ghế trên trình duyệt này. Lịch sử API hiện tại chưa
          cung cấp chi tiết ghế.
        </p>
      )}
      <Button className="small" onClick={download}>
        Tải thông tin vé (.txt)
      </Button>
      <p className="hint">
        Bản ghi tham khảo, không thay thế vé do rạp phát hành.
      </p>
    </section>
  );
}
