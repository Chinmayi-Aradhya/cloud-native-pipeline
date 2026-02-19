FROM maven:3.8.6-amazoncorretto-11 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package

FROM amazoncorretto:11-alpine
COPY --from=build /app/target/java-producer-1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]
EOF