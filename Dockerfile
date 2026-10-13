
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/venue-booking.war app.war

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.war"]
