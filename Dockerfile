FROM eclipse-temurin:27-jdk AS build
WORKDIR /workspace
RUN apt-get update \
    && apt-get install --yes --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:27-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/target/interview-lab-0.1.0-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
