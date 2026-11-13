import React, { useEffect, useRef, useState } from "react";
import { Icon } from "../../components/UI.jsx";
import { dayText, dayValue } from "./dayText.js";

export default function DateField({ label, value, onChange }) {
  const picker = useRef(null);
  const [text, setText] = useState(dayText(value));
  const [typing, setTyping] = useState(false);
  useEffect(() => {
    if (!typing) setText(dayText(value));
  }, [value, typing]);
  function commit() {
    setTyping(false);
    const next = dayValue(text);
    if (next) onChange(next);
    else if (!text.trim()) onChange("");
    else setText(dayText(value));
  }
  return (
    <span className="admin-date-field">
      <input
        type="text"
        inputMode="numeric"
        placeholder="dd/mm/yyyy"
        aria-label={label}
        value={text}
        onChange={(event) => {
          setTyping(true);
          setText(event.target.value);
        }}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault();
            commit();
          }
        }}
      />
      <button
        type="button"
        className="admin-date-button"
        aria-label={`Mở lịch ${label.toLowerCase()}`}
        title="Mở lịch"
        onClick={() => picker.current?.showPicker?.()}
      >
        <Icon name="calendar" size={16} />
      </button>
      <input
        ref={picker}
        type="date"
        className="admin-date-native"
        tabIndex={-1}
        aria-hidden="true"
        value={value}
        onChange={(event) => {
          onChange(event.target.value);
          setText(dayText(event.target.value));
        }}
      />
    </span>
  );
}

