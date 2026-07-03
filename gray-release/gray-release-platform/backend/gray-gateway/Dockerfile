FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl gray-gateway -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/gray-gateway/target/gray-gateway-1.0.0.jar app.jar
EXPOSE 18000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

