FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY gradle ./gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY payment/build.gradle.kts ./payment/
COPY shop/build.gradle.kts ./shop/
COPY openapi.yaml ./

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :payment:dependencies --no-daemon

COPY payment/src ./payment/src

RUN ./gradlew :payment:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV SERVER_PORT=8081
ENV SPRING_R2DBC_URL="r2dbc:h2:mem:///payment;DB_CLOSE_DELAY=-1"

COPY --from=build /app/payment/build/libs/*.jar app.jar

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${SERVER_PORT}"]
