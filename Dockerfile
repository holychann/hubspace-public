# ===============================
# Build stage
# ===============================
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

# ===============================
# Runtime stage
# ===============================
FROM eclipse-temurin:17-jre
WORKDIR /app

EXPOSE 8080

COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

# 기존의 유연함(JAVA_OPTS)은 살리고, IPv6 설정만 추가
ENTRYPOINT ["sh", "-c", "java -Djava.net.preferIPv6Addresses=true $JAVA_OPTS -jar app.jar"]
