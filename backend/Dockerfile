# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 使用阿里云镜像仓库加速依赖下载（兼容 Maven Central）
COPY settings.xml .
COPY pom.xml .
RUN mvn -s settings.xml -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -s settings.xml -q -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/hd-center-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
