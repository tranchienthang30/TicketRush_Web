FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S ticketrush && adduser -S ticketrush -G ticketrush

COPY --from=build /workspace/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R ticketrush:ticketrush /app

USER ticketrush

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
