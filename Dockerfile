FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY gradle ./gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./

RUN ./gradlew --no-daemon dependencies

COPY src ./src

RUN ./gradlew --no-daemon build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV SERVER_PORT="8080"
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:./data/market;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
ENV SPRING_DATASOURCE_USERNAME="sa"
ENV SPRING_DATASOURCE_PASSWORD=""

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
