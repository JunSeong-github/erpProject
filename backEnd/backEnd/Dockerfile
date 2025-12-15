FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY src/main/java/erp/backEnd .
# gradle wrapper로 빌드 (가장 안정적)
RUN chmod +x ./gradlew && ./gradlew build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8"
EXPOSE 8080
ENV PORT=8080
CMD ["java","-jar","build/libs/*.jar"]
