import axios from "axios";

// 中文：所有前端 REST 请求共用 /api 前缀，Docker/Nginx 会把它转发给 Spring Boot。
// English: All frontend REST requests share /api; Docker/Nginx proxies that path to Spring Boot.
const api = axios.create({
  baseURL: "/api",
});

export default api;
