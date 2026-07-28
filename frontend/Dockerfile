# ---- Build Stage ----
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制依赖文件，利用 Docker 层缓存
COPY package.json package-lock.json ./

# 安装所有依赖
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ---- Production Stage ----
FROM nginx:alpine

# 将构建产物复制到 nginx 静态目录
COPY --from=builder /app/build /usr/share/nginx/html

# 暴露 80 端口
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
