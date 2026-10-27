import { Catalog } from "./Catalog";

const mockData = {
  movies: [
    {
      movieId: 1,
      title: "Avengers: Endgame",
      durationMinutes: 181,
      status: "SHOWING",
      genres: [{ name: "Hành động" }, { name: "Viễn tưởng" }],
      ageRestriction: "C13",
      description: "Cuộc chiến cuối cùng của các siêu anh hùng.",
    },
    {
      movieId: 2,
      title: "Dune: Part Two",
      durationMinutes: 166,
      status: "SHOWING",
      genres: [{ name: "Viễn tưởng" }, { name: "Phiêu lưu" }],
      ageRestriction: "C16",
      description: "Paul Atreides liên minh với người Fremen.",
    },
    {
      movieId: 3,
      title: "Inside Out 2",
      durationMinutes: 96,
      status: "UPCOMING",
      genres: [{ name: "Hoạt hình" }, { name: "Gia đình" }],
      ageRestriction: "P",
      description: "Riley bước vào tuổi teen với những cảm xúc mới.",
    },
  ],
  genres: [
    { genreId: 1, name: "Hành động" },
    { genreId: 2, name: "Viễn tưởng" },
    { genreId: 3, name: "Hoạt hình" },
    { genreId: 4, name: "Phiêu lưu" },
  ],
};

function App() {
  return (
    <div style={{ padding: "20px", fontFamily: "sans-serif" }}>
      <Catalog
        data={mockData}
        loading={false}
        error={null}
        reload={() => {}}
        onMovie={(movie) => alert(`Bạn chọn phim: ${movie.title}`)}
      />
    </div>
  );
}

export default App;
