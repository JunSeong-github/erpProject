# 1) Build stage (여기서 build 라는 이름을 붙임)
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew build -x test

# 2) Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java","-jar","app.jar"]
