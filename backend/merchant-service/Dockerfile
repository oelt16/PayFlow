# Build from repo root: docker build -f merchant-service/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B -pl merchant-service -am -Dmaven.test.skip=true package \
  && JAR=$(ls -S merchant-service/target/*.jar | grep -v -- '-plain.jar$' | head -n1) \
  && test -n "$JAR" && cp "$JAR" /app/boot.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/boot.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
