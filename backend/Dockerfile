FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# El API corre con un usuario sin privilegios: si alguien lograra ejecutar
# codigo dentro del contenedor, no tendria permisos de root.
RUN groupadd --system t4kash && useradd --system --gid t4kash --no-create-home t4kash
COPY --from=build --chown=t4kash:t4kash /app/target/*.jar app.jar
USER t4kash
EXPOSE 8080
# Con memoria agotada el proceso termina y Render lo reinicia, en lugar de
# quedar vivo pero sin responder. El healthcheck lo hace Render (/api/health).
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
