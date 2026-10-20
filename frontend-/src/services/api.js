/**
 * SOA Step 0 — Tầng gọi API tập trung (qua API Gateway :8080 khi có, tạm thời :8080 monolith).
 * Luôn dùng BASE_URL; KHÔNG hard-code host ở component nào khác.
 */
const BASE_URL =
  import.meta.env?.VITE_API_BASE_URL ?? "http://localhost:8080";

const api = {
  async request(path, options = {}) {
    const res = await fetch(`${BASE_URL}${path}`, {
      headers: { "Content-Type": "application/json", ...options.headers },
      ...options,
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw Object.assign(new Error(body.message || res.statusText), {
        status: res.status,
        body,
      });
    }
    // 204 / không body
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  },

  get: (path) => api.request(path),
  post: (path, data) =>
    api.request(path, { method: "POST", body: JSON.stringify(data) }),
  put: (path, data) =>
    api.request(path, { method: "PUT", body: JSON.stringify(data) }),
  delete: (path) => api.request(path, { method: "DELETE" }),
};

export default api;
