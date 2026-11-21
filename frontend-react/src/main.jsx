import React from "react";
import { createRoot } from "react-dom/client";
import { GoogleOAuthProvider } from "@react-oauth/google";
import App from "./App.jsx";
import ClickSpark from "./components/ClickSpark.jsx";
import "./styles.css";

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  "your-google-client-id.apps.googleusercontent.com";

class ErrorBoundary extends React.Component {
  state = { error: false };
  static getDerivedStateFromError() {
    return { error: true };
  }
  render() {
    return this.state.error ? (
      <main className="container page-section">
        <h1>Giao diện gặp lỗi</h1>
        <p>
          Hãy tải lại trang. Nếu lỗi lặp lại, gửi thông báo trong Console cho
          nhóm frontend.
        </p>
        <button className="button primary" onClick={() => location.reload()}>
          Tải lại trang
        </button>
      </main>
    ) : (
      this.props.children
    );
  }
}

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ErrorBoundary>
      <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
        <App />
        <ClickSpark />
      </GoogleOAuthProvider>
    </ErrorBoundary>
  </React.StrictMode>,
);
