# build
FROM gradle:8-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :app-debate:bootJar -x test --no-daemon

# runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app-debate/build/libs/app-debate-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
