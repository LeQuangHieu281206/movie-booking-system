import path from "node:path";
import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const rootEnv = loadEnv(mode, path.resolve(process.cwd(), ".."), "");
  const env = { ...rootEnv, ...loadEnv(mode, process.cwd(), "") };
  const target =
    process.env.BACKEND_URL || env.BACKEND_URL || "http://localhost:8080";
  const googleClientId =
    process.env.VITE_GOOGLE_CLIENT_ID ||
    env.VITE_GOOGLE_CLIENT_ID ||
    process.env.GOOGLE_CLIENT_ID ||
    env.GOOGLE_CLIENT_ID ||
    "your-google-client-id.apps.googleusercontent.com";
  const proxy = {
    "/api": { target, changeOrigin: true, timeout: 15000, proxyTimeout: 15000 },
  };
  return {
    plugins: [react()],
    define: {
      "import.meta.env.VITE_GOOGLE_CLIENT_ID": JSON.stringify(googleClientId),
    },
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
