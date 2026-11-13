import { useEffect, useRef } from "react";

/**
 * Hiệu ứng "click spark": mỗi lần bấm chuột/chạm tay, một chùm tia sáng
 * toả ra từ đúng điểm bấm rồi mờ dần (giống hiệu ứng click trên các trang
 * docs hiện đại). Toàn bộ được vẽ trên một <canvas> phủ màn hình nên không
 * ảnh hưởng layout và không chặn thao tác người dùng.
 */

const EASINGS = {
  "ease-out": (t) => 1 - (1 - t) ** 3,
  "ease-in-out": (t) =>
    t < 0.5 ? 4 * t * t * t : 1 - (-2 * t + 2) ** 3 / 2,
  linear: (t) => t,
};

const resolveColor = (value) => {
  if (!value.startsWith("var(")) return value;
  const name = value.slice(4, -1).split(",")[0].trim();
  const fromToken = getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
  return fromToken || "#d9a900";
};

export default function ClickSpark({
  color = "var(--spark-color)",
  size = 11, // độ dài tia lúc vừa bấm (px)
  radius = 18, // bán kính chùm tia toả ra (px)
  count = 8, // số tia
  duration = 420, // thời gian sống của một chùm tia (ms)
  lineWidth = 2,
  easing = "ease-out",
}) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!canvas || !ctx) return;

    const reduced = window.matchMedia?.("(prefers-reduced-motion: reduce)");
    const ease = EASINGS[easing] || EASINGS["ease-out"];
    const sparks = [];
    let frame = 0;

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const width = window.innerWidth;
      const height = window.innerHeight;
      canvas.width = Math.round(width * dpr);
      canvas.height = Math.round(height * dpr);
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    const draw = (now) => {
      frame = 0;
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.lineCap = "round";
      for (let i = sparks.length - 1; i >= 0; i -= 1) {
        const spark = sparks[i];
        const progress = (now - spark.start) / duration;
        if (progress >= 1) {
          sparks.splice(i, 1);
          continue;
        }
        const eased = ease(progress);
        const travel = eased * radius;
        const length = size * (1 - eased);
        const cos = Math.cos(spark.angle);
        const sin = Math.sin(spark.angle);
        ctx.globalAlpha = 1 - eased;
        ctx.strokeStyle = spark.color;
        ctx.lineWidth = lineWidth;
        ctx.beginPath();
        ctx.moveTo(spark.x + travel * cos, spark.y + travel * sin);
        ctx.lineTo(
          spark.x + (travel + length) * cos,
          spark.y + (travel + length) * sin,
        );
        ctx.stroke();
      }
      ctx.globalAlpha = 1;
      if (sparks.length) frame = requestAnimationFrame(draw);
      else ctx.clearRect(0, 0, canvas.width, canvas.height);
    };

    const onPointerDown = (event) => {
      if (reduced?.matches) return;
      if (event.pointerType === "mouse" && event.button !== 0) return;
      const start = performance.now();
      const stroke = resolveColor(color);
      for (let i = 0; i < count; i += 1) {
        sparks.push({
          x: event.clientX,
          y: event.clientY,
          angle: (Math.PI * 2 * i) / count,
          start,
          color: stroke,
        });
      }
      if (!frame) frame = requestAnimationFrame(draw);
    };

    resize();
    window.addEventListener("resize", resize);
    window.addEventListener("pointerdown", onPointerDown, { passive: true });
    return () => {
      window.removeEventListener("resize", resize);
      window.removeEventListener("pointerdown", onPointerDown);
      if (frame) cancelAnimationFrame(frame);
      sparks.length = 0;
    };
  }, [color, size, radius, count, duration, lineWidth, easing]);

  return <canvas ref={canvasRef} className="click-spark" aria-hidden="true" />;
}
