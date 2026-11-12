FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/order-service-1.0.0-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8081 20881
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
