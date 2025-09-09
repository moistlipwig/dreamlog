# === Build stage ===
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# === Run stage (distroless) ===
FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
USER nonroot:nonroot
ENTRYPOINT ["java","-jar","/app/app.jar"]