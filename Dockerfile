# Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/dreamshops-*.jar app.jar

# Run
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home app
USER app
WORKDIR /app
COPY --from=build /src/app.jar app.jar
EXPOSE 9193
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
