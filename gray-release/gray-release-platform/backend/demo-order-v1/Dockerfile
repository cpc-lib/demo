FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl demo-order-v1 -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/demo-order-v1/target/demo-order-v1-1.0.0.jar app.jar
EXPOSE 18081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

