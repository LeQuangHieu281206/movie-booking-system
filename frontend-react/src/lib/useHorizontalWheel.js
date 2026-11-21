import { useCallback, useEffect, useRef } from "react";

// Cho phép lăn chuột dọc để cuộn ngang một dải nội dung (thanh bước, dải ngày).
// Chỉ chặn cuộn trang khi dải còn cuộn được theo hướng lăn, nên tới mép thì
// trang lại cuộn bình thường. React gắn onWheel ở dạng passive nên phải dùng
// listener gốc với passive: false để preventDefault() có tác dụng.
// Trả về callback ref để vẫn gắn được khi dải chỉ xuất hiện ở một bước sau.
export function useHorizontalWheel() {
  const detach = useRef(null);
  const ref = useCallback((node) => {
    detach.current?.();
    detach.current = null;
    if (!node) return;
    const onWheel = (event) => {
      if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return;
      const max = node.scrollWidth - node.clientWidth;
      if (max <= 0) return;
      const next = node.scrollLeft + event.deltaY;
      if ((next <= 0 && event.deltaY < 0) || (next >= max && event.deltaY > 0))
        return;
      event.preventDefault();
      node.scrollLeft = Math.max(0, Math.min(max, next));
    };
    node.addEventListener("wheel", onWheel, { passive: false });
    detach.current = () => node.removeEventListener("wheel", onWheel);
  }, []);
  useEffect(() => () => detach.current?.(), []);
  return ref;
}
