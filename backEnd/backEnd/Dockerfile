FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY src/main/java/erp/backEnd .
RUN gradle build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java","-jar","app.jar"]
