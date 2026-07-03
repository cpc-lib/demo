FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl demo-order-v2 -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/demo-order-v2/target/demo-order-v2-1.0.0.jar app.jar
EXPOSE 18082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

