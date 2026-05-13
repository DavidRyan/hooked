# Multi-stage build for chat-server + mcp-server.
#
# Build context must be the repo root so we can grab both subprojects:
#   docker build -f chat-server/Dockerfile -t hooked-chat-server .
#
# Or, as orchestrated by the root docker-compose.yml.
#
# The final image bundles BOTH jars and sets MCP_SERVER_JAR so chat-server can
# spawn mcp-server as a subprocess (per-session, per-user) at runtime.

FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Copy gradle wrappers + build configs first for better layer caching
COPY chat-server/gradle ./chat-server/gradle
COPY chat-server/gradlew ./chat-server/
COPY chat-server/settings.gradle.kts ./chat-server/
COPY chat-server/build.gradle.kts ./chat-server/

COPY mcp-server/gradle ./mcp-server/gradle
COPY mcp-server/gradlew ./mcp-server/
COPY mcp-server/settings.gradle.kts ./mcp-server/
COPY mcp-server/build.gradle.kts ./mcp-server/

RUN chmod +x chat-server/gradlew mcp-server/gradlew

# Sources
COPY chat-server/src ./chat-server/src
COPY mcp-server/src ./mcp-server/src

# Build both fat jars
RUN cd mcp-server && ./gradlew shadowJar --no-daemon
RUN cd chat-server && ./gradlew shadowJar --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /build/chat-server/build/libs/hooked-chat-server.jar /app/chat-server.jar
COPY --from=builder /build/mcp-server/build/libs/hooked-mcp-server.jar  /app/mcp-server.jar

ENV MCP_SERVER_JAR=/app/mcp-server.jar
ENV PORT=8080

EXPOSE 8080

# chat-server spawns mcp-server as a subprocess (java -jar $MCP_SERVER_JAR ...)
# so /opt/java/openjdk/bin/java must be on PATH — it is in this base image.
CMD ["java", "-jar", "/app/chat-server.jar"]
