// Ngày lưu dạng YYYY-MM-DD, hiển thị và nhập theo dd/mm/yyyy.
export const dayText = (value) => {
  const [, year, month, day] = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || "") || [];
  return year ? `${day}/${month}/${year}` : "";
};
export const dayValue = (text) => {
  const [, day, month, year] =
    /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(text.trim()) || [];
  if (!year) return null;
  const date = new Date(`${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}T00:00:00`);
  const valid =
    date.getFullYear() === +year &&
    date.getMonth() + 1 === +month &&
    date.getDate() === +day;
  return valid
    ? `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`
    : null;
};

