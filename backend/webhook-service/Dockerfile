# Build from repo root: docker build -f webhook-service/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B -pl webhook-service -am -Dmaven.test.skip=true package \
  && JAR=$(ls -S webhook-service/target/*.jar | grep -v -- '-plain.jar$' | head -n1) \
  && test -n "$JAR" && cp "$JAR" /app/boot.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/boot.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
