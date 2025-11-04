# Sử dụng OpenJDK 17 làm base image
FROM eclipse-temurin:17-jdk-jammy
# Thiết lập thông tin metadata
LABEL maintainer="demo-app"
LABEL version="1.0"
LABEL description="Spring Boot Learn Performance Test API"

# Tạo thư mục làm việc
WORKDIR /app

# Copy file JAR từ thư mục target (đổi tên nếu cần)
COPY target/learn-performance-test-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Thiết lập JVM options để tối ưu hóa cho container
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Health check để kiểm tra ứng dụng có hoạt động không
RUN apt-get update && apt-get install -y curl
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Chạy ứng dụng
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
