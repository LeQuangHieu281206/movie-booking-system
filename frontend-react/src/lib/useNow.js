import { useEffect, useState } from "react";

// Trả về một số đổi mỗi `interval` ms để component tính lại "suất còn đặt được"
// khi trang mở lâu (suất đã bắt đầu sẽ tự biến mất).
export function useNow(interval = 60000) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), interval);
    return () => clearInterval(timer);
  }, [interval]);
  return now;
}
