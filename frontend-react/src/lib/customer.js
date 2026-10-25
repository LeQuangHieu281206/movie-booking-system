// Kiểm tra dữ liệu hồ sơ khách hàng (Frontend 3). Hồ sơ này KHÔNG phải đăng nhập.

export function parseCustomerId(value) {
  const text = String(value ?? "")
    .trim()
    .replace(/^#/, "");
  const id = Number(text);
  if (!/^\d{1,15}$/.test(text) || !Number.isSafeInteger(id) || id <= 0)
    throw new Error("Mã khách hàng phải là số nguyên dương, ví dụ 12.");
  return id;
}

export function normalizeCustomerFields(fields) {
  const fullName = String(fields?.fullName ?? "")
    .trim()
    .replace(/\s+/g, " ");
  const email = String(fields?.email ?? "").trim();
  const phoneNumber = String(fields?.phoneNumber ?? "").trim();
  if (fullName.length < 2)
    throw new Error("Vui lòng nhập họ và tên (ít nhất 2 ký tự).");
  if (fullName.length > 100)
    throw new Error("Họ và tên tối đa 100 ký tự.");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 150)
    throw new Error("Email chưa đúng định dạng, ví dụ ban@example.com.");
  if (phoneNumber) {
    const digits = phoneNumber.replace(/\D/g, "").length;
    if (
      !/^\+?[0-9][0-9 .()-]*$/.test(phoneNumber) ||
      digits < 8 ||
      digits > 15 ||
      phoneNumber.length > 20
    )
      throw new Error(
        "Số điện thoại chưa hợp lệ. Có thể bỏ trống hoặc nhập 8–15 chữ số.",
      );
  }
  return { fullName, email, phoneNumber };
}

// Chuẩn hóa và kiểm tra hồ sơ backend trả về trước khi đưa vào ứng dụng.
// - userId phải là số nguyên dương thật (không dựng mã giả).
// - Nếu tra theo mã, mã trả về phải trùng mã đã nhập.
// - Không nạp tài khoản ADMIN vào luồng khách hàng.
// - fullName luôn là chuỗi, vì header gọi fullName.split().
export function toCustomerProfile(user, expectedId) {
  const userId = Number(user?.userId);
  if (!Number.isSafeInteger(userId) || userId <= 0)
    throw new Error(
      "Backend không trả về mã khách hàng hợp lệ. Hãy kiểm tra user-service.",
    );
  if (expectedId !== undefined && userId !== expectedId)
    throw new Error("Hồ sơ trả về không khớp mã khách hàng đã nhập.");
  if (String(user.role ?? "").toUpperCase() === "ADMIN")
    throw new Error("Mã này không phải hồ sơ khách hàng.");
  const email = typeof user.email === "string" ? user.email : "";
  const fullName =
    typeof user.fullName === "string" && user.fullName.trim()
      ? user.fullName.trim()
      : email || `Khách #${userId}`;
  return { ...user, userId, fullName, email };
}

export function customerErrorMessage(error, action) {
  const message = error?.message || "";
  if (
    action === "create" &&
    (error?.status === 409 ||
      /đã tồn tại|already|exist|duplicate|unique|trùng/i.test(message))
  )
    return "Email này đã có hồ sơ. Hãy chọn “Đã có mã khách hàng” và nhập mã đã được cấp.";
  if (
    action === "lookup" &&
    (error?.status === 404 || /not found|không tìm thấy/i.test(message))
  )
    return "Không tìm thấy khách hàng với mã này. Hãy kiểm tra lại mã và chế độ dữ liệu (mẫu hay backend) đang chọn.";
  return message || "Có lỗi xảy ra. Vui lòng thử lại.";
}
