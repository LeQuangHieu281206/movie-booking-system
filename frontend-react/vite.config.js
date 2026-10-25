import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target =
    process.env.BACKEND_URL || env.BACKEND_URL || "http://localhost:8080";
  const proxy = {
    "/api": { target, changeOrigin: true, timeout: 15000, proxyTimeout: 15000 },
  };
  return {
    plugins: [react()],
    server: {
      host: "0.0.0.0",
      port: 5173,
      strictPort: true,
      allowedHosts: ["terminal.local", "localhost"],
      proxy,
    },
    preview: { host: "127.0.0.1", port: 5173, strictPort: true, proxy },
  };
});
