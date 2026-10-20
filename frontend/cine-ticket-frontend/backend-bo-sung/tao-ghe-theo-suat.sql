-- TUY CHON: chi chay thu cong sau khi sao luu DB va kiem tra dung du lieu.
-- Can co phim, rap, phong, ghe vat ly va suat chieu truoc.
-- Tao ShowtimeSeat cho tung Seat thuoc phong cua suat; khong tao ghe vat ly.
-- Khong chay dong thoi voi thao tac tao suat/ghe o noi khac.
BEGIN;
INSERT INTO showtime_seats (showtime_id, seat_id, status, updated_at)
SELECT st.showtime_id, s.seat_id, 'AVAILABLE', CURRENT_TIMESTAMP
FROM showtimes st
JOIN seats s ON s.room_id = st.room_id
WHERE st.status = 'SCHEDULED' AND st.start_time > CURRENT_TIMESTAMP
  AND NOT EXISTS (
    SELECT 1 FROM showtime_seats ss
    WHERE ss.showtime_id = st.showtime_id AND ss.seat_id = s.seat_id
  );
COMMIT;
