FROM eclipse-temurin:17-jre
WORKDIR /app
COPY app.jar /app/app.jar
EXPOSE 8080
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
