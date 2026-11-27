import React from "react";
import { Poster } from "./UI.jsx";

export default function HeroHotList({ movies, activeId, onPick }) {
  return (
    <div className="container hot-strip">
      <h2>Top phim đang hot</h2>
      <ol>
        {movies.map((movie, index) => (
          <li key={movie.movieId}>
            <button
              type="button"
              className={movie.movieId === activeId ? "active" : ""}
              aria-pressed={movie.movieId === activeId}
              onClick={() => onPick(movie.movieId)}
            >
              <span className="hot-rank">{index + 1}</span>
              <Poster movie={movie} />
              <span className="hot-title">{movie.title}</span>
            </button>
          </li>
        ))}
      </ol>
    </div>
  );
}
