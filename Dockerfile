# =============================================
# Netdisk 云盘 - Docker 多阶段构建
# =============================================

# ---------- Stage 1: 前端构建 ----------
FROM node:20-alpine AS frontend
WORKDIR /src/front_end
COPY front_end/package.json front_end/package-lock.json* ./
RUN npm ci --silent
COPY front_end/ ./
RUN npx vite build

# ---------- Stage 2: 后端构建 ----------
FROM maven:3.9-eclipse-temurin-17-alpine AS backend
WORKDIR /src/back_end
# 先拷贝 pom 独立下载依赖（利用 Docker 层缓存）
COPY back_end/pom.xml ./
RUN mvn dependency:go-offline -q
# 拷贝源码 + 前端静态产物
COPY back_end/src ./src
COPY --from=frontend /src/back_end/src/main/resources/static ./src/main/resources/static
RUN mvn package -DskipTests -q

# ---------- Stage 3: 运行镜像 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend /src/back_end/target/Netdisk-*.jar app.jar

# 容器对外端口（应用 = 8080，如需 HTTPS = 8443）
EXPOSE 8080

# 启动参数可通过环境变量覆盖
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
