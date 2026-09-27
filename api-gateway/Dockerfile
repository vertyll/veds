ARG JDK_IMAGE=azul/zulu-openjdk:25-latest
ARG JRE_IMAGE=azul/zulu-openjdk:25-jre-headless-latest

FROM ${JDK_IMAGE} AS build
WORKDIR /workspace
COPY shared-web/ ./shared-web/
COPY shared-authz/ ./shared-authz/
COPY shared-error/ ./shared-error/
COPY api-gateway/ ./api-gateway/
WORKDIR /workspace/api-gateway
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

FROM ${JDK_IMAGE} AS extract
WORKDIR /extract
COPY --from=build /workspace/api-gateway/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

FROM ${JRE_IMAGE} AS runtime
WORKDIR /app
RUN groupadd --system --gid 1001 veds && useradd --system --uid 1001 --gid veds veds

COPY --from=extract --chown=veds:veds /extract/layers/dependencies/ ./
COPY --from=extract --chown=veds:veds /extract/layers/spring-boot-loader/ ./
COPY --from=extract --chown=veds:veds /extract/layers/snapshot-dependencies/ ./
COPY --from=extract --chown=veds:veds /extract/layers/application/ ./

USER veds
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/liveness HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q '\"UP\"' <&3"]

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
