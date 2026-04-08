# ===== BASE IMAGE NHẸ HƠN =====
FROM eclipse-temurin:17-jre-jammy

# Metadata
LABEL maintainer="demo-app"
LABEL version="1.0"
LABEL description="Spring Boot Learn Performance Test API"

# Workdir
WORKDIR /app

# Copy jar
COPY target/learn-performance-test-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# ===== JVM TỐI ƯU CHO VPS 3GB =====
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxGCPauseMillis=200"

RUN apt-get update && apt-get install -y curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]