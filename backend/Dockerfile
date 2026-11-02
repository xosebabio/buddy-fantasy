# Backend image (RNF-06): builds with Maven and runs the layered jar on a JRE as a non-root user.
# The tests are not run here: the `backend` job of CI already passes them.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DskipTests
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests -Djacoco.skip=true \
    && java -Djarmode=tools -jar target/buddy-fantasy-backend-*.jar extract --layers --destination /build/layers

FROM eclipse-temurin:21-jre AS runtime
ARG REVISION=unknown
LABEL org.opencontainers.image.source="https://github.com/xosebabio/buddy-fantasy" \
      org.opencontainers.image.revision="${REVISION}"
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
# Least to most volatile, so a code change only rebuilds the last layer.
COPY --from=build /build/layers/dependencies/ ./
COPY --from=build /build/layers/spring-boot-loader/ ./
COPY --from=build /build/layers/snapshot-dependencies/ ./
COPY --from=build /build/layers/application/ ./
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=UTC", "-jar", "buddy-fantasy-backend-0.0.1-SNAPSHOT.jar"]
