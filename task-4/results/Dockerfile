FROM gradle:8.10-jdk17 AS build
WORKDIR /build
COPY task-4/results/app/ ./
COPY task-5/results/java/ src/main/java/
COPY task-6/results/java/ src/main/java/
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 gradle --no-daemon bootJar copyAgent

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/build/libs/tradeware.jar /app/tradeware.jar
COPY --from=build /build/build/agent/jmx-agent.jar /app/jmx-agent.jar
COPY task-5/results/jmx.yml /app/jmx.yml
ENTRYPOINT ["java", "-jar", "/app/tradeware.jar"]
