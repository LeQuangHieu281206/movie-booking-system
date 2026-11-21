import { useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Icon } from "./UI.jsx";

const MENU_GAP = 6;
const MENU_MAX_HEIGHT = 260;
const CLOSE_MS = 130;

/**
 * Dropdown tự vẽ thay cho <select> gốc: menu bám theo nút, mở/đóng có
 * chuyển động, hỗ trợ bàn phím và đọc màn hình.
 */
export default function Select({
  label,
  value,
  options = [],
  onChange,
  disabled = false,
  placeholder = "Chọn",
  className = "",
}) {
  const id = useId();
  const wrapRef = useRef(null);
  const triggerRef = useRef(null);
  const menuRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [closing, setClosing] = useState(false);
  const [active, setActive] = useState(-1);
  const [pos, setPos] = useState(null);
  const [root, setRoot] = useState(null);
  const timer = useRef(0);
  const selectedIndex = options.findIndex(
    (option) => String(option.value) === String(value ?? ""),
  );
  const selected = options[selectedIndex];

  const openMenu = () => {
    window.clearTimeout(timer.current);
    // <dialog> chặn tương tác với mọi thứ ngoài nó, nên menu phải nằm trong
    // dialog khi dropdown đặt trong modal.
    setRoot(triggerRef.current?.closest("dialog") || document.body);
    setClosing(false);
    setActive(selectedIndex >= 0 ? selectedIndex : 0);
    setOpen(true);
  };

  const closeMenu = () => {
    if (!open || closing) return;
    setClosing(true);
    timer.current = window.setTimeout(() => {
      setOpen(false);
      setClosing(false);
    }, CLOSE_MS);
  };

  useEffect(() => () => window.clearTimeout(timer.current), []);

  // Bám theo nút và lật lên trên khi gần đáy màn hình.
  useLayoutEffect(() => {
    if (!open) return undefined;
    const place = () => {
      const trigger = triggerRef.current;
      if (!trigger) return;
      const rect = trigger.getBoundingClientRect();
      const width = Math.max(Math.round(rect.width), 168);
      const estimate = Math.min(options.length * 42 + 12, MENU_MAX_HEIGHT);
      const below = window.innerHeight - rect.bottom - 12;
      const up = below < estimate && rect.top > below;
      setPos({
        left: Math.max(8, Math.min(rect.left, window.innerWidth - width - 8)),
        top: up ? undefined : Math.round(rect.bottom + MENU_GAP),
        bottom: up
          ? Math.round(window.innerHeight - rect.top + MENU_GAP)
          : undefined,
        width,
        up,
      });
    };
    place();
    window.addEventListener("resize", place);
    window.addEventListener("scroll", place, true);
    return () => {
      window.removeEventListener("resize", place);
      window.removeEventListener("scroll", place, true);
    };
  }, [open, options.length]);

  useEffect(() => {
    if (!open) return undefined;
    const onPointer = (event) => {
      if (
        wrapRef.current?.contains(event.target) ||
        menuRef.current?.contains(event.target)
      ) {
        return;
      }
      closeMenu();
    };
    document.addEventListener("pointerdown", onPointer);
    return () => document.removeEventListener("pointerdown", onPointer);
  });

  const choose = (option) => {
    onChange(String(option.value));
    closeMenu();
  };

  const move = (delta) => {
    if (!options.length) return;
    setActive((index) => {
      const next = index + delta;
      if (next < 0) return options.length - 1;
      if (next >= options.length) return 0;
      return next;
    });
  };

  return (
    <div className={`select ${className}`.trim()} ref={wrapRef}>
      <button
        type="button"
        ref={triggerRef}
        className="select-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? `${id}-menu` : undefined}
        aria-label={label}
        aria-activedescendant={
          open && options[active] ? `${id}-opt-${active}` : undefined
        }
        disabled={disabled}
        onClick={() => (open ? closeMenu() : openMenu())}
        onKeyDown={(event) => {
          if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            if (!open) {
              openMenu();
              setActive(
                event.key === "ArrowUp" ? options.length - 1 : selectedIndex >= 0 ? selectedIndex : 0,
              );
            } else {
              move(event.key === "ArrowDown" ? 1 : -1);
            }
          } else if (event.key === "Escape") {
            if (open) {
              // Chỉ đóng menu, không để <dialog> đóng luôn cả modal.
              event.preventDefault();
              event.stopPropagation();
            }
            closeMenu();
          } else if ((event.key === "Enter" || event.key === " ") && open) {
            event.preventDefault();
            if (options[active]) choose(options[active]);
          }
        }}
      >
        <span className={`select-value${selected ? "" : " is-placeholder"}`}>
          {selected ? selected.label : placeholder}
        </span>
        <Icon name="chevron" size={14} className="select-caret" />
      </button>

      {open &&
        pos &&
        createPortal(
          <ul
            id={`${id}-menu`}
            ref={menuRef}
            role="listbox"
            aria-label={label}
            className={`select-menu${pos.up ? " up" : ""}${closing ? " is-closing" : ""}`}
            style={{
              left: pos.left,
              top: pos.top,
              bottom: pos.bottom,
              width: pos.width,
            }}
          >
            {options.map((option, index) => (
              <li
                key={option.value}
                id={`${id}-opt-${index}`}
                role="option"
                aria-selected={index === selectedIndex}
                className={`select-option${index === active ? " active" : ""}`}
                onMouseEnter={() => setActive(index)}
                onMouseDown={(event) => {
                  event.preventDefault();
                  choose(option);
                }}
              >
                <span>{option.label}</span>
                {index === selectedIndex && (
                  <Icon name="check" size={15} className="select-check" />
                )}
              </li>
            ))}
          </ul>,
          root || document.body,
        )}
    </div>
  );
}
