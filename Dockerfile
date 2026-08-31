# admin-pro 多阶段构建
# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/admin-pro-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV DB_URL=jdbc:mysql://mysql:3306/admin_pro?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true \
    DB_USERNAME=root \
    DB_PASSWORD=root \
    REDIS_HOST=redis \
    REDIS_PORT=6379
ENTRYPOINT ["java", "-jar", "app.jar"]
