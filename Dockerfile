# Start with a Java 25 runtime image
FROM eclipse-temurin:25-jre

COPY target/app.jar /app.jar

EXPOSE 7070

CMD ["java", "-jar", "/app.jar"]
