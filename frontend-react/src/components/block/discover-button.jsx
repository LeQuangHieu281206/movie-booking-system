import React from "react";
import { Icon } from "../UI.jsx";
import "./discover-button.css";

export function DiscoverButton({ label = "Khám phá", href, onClick, className = "" }) {
  const face = (
    <>
      <span className="obsidian-discover-button__fill" aria-hidden="true" />
      <span className="obsidian-discover-button__icon" aria-hidden="true">
        <Icon name="arrow" size={18} strokeWidth={2.25} />
      </span>
      <span className="obsidian-discover-button__text">{label}</span>
    </>
  );

  if (href) {
    return (
      <a className={`obsidian-discover-button ${className}`} href={href}>
        {face}
      </a>
    );
  }

  return (
    <button
      className={`obsidian-discover-button ${className}`}
      type="button"
      onClick={onClick}
    >
      {face}
    </button>
  );
}

export default DiscoverButton;
