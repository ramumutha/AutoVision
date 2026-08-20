FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn ./.mvn
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

COPY --from=build /workspace/target/platform.jar /app/platform.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/platform.jar"]