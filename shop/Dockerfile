FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY gradle ./gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY shop/build.gradle.kts ./shop/
COPY payment/build.gradle.kts ./payment/
COPY openapi.yaml ./

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :shop:dependencies --no-daemon

COPY shop/src ./shop/src

RUN ./gradlew :shop:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV SERVER_PORT=8080
ENV SPRING_R2DBC_URL="r2dbc:h2:file:///app/data/market;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
ENV SPRING_DATA_REDIS_HOST=redis
ENV PAYMENT_SERVICE_URL=http://payment:8081

COPY --from=build /app/shop/build/libs/*.jar app.jar

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${SERVER_PORT}"]
