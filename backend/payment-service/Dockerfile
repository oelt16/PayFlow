# Build from repo root: docker build -f payment-service/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
# Spring Boot leaves a *-plain.jar (no Main-Class); copy only the repackaged executable jar.
RUN chmod +x mvnw && ./mvnw -B -pl payment-service -am -Dmaven.test.skip=true package \
  && JAR=$(ls -S payment-service/target/*.jar | grep -v -- '-plain.jar$' | head -n1) \
  && test -n "$JAR" && cp "$JAR" /app/boot.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/boot.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
