# syntax=docker/dockerfile:1

# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
COPY src src

# o cache do Gradle fica fora da imagem, então rebuilds não baixam tudo de novo
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test \
    && rm -f build/libs/*-plain.jar

# ---- runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home app
COPY --from=build /app/build/libs/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
