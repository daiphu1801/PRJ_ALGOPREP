FROM eclipse-temurin:21-jre
WORKDIR /app
COPY algoprep-bootstrap-*.jar /app/algoprep.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/algoprep.jar"]
