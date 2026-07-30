import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles/globals.css";
import "./styles/App.css";

// 中文：创建 React 根节点，并在严格模式下挂载整个应用。
// English: Creates the React root and mounts the application in Strict Mode.
const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement,
);

root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
