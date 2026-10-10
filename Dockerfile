# ---------- Etapa 1: compilar el .jar ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .

# sed: quita saltos de linea de Windows (CRLF) que rompen gradlew dentro de Linux
RUN sed -i 's/\r$//' gradlew \
 && chmod +x gradlew \
 && ./gradlew bootJar -x test --no-daemon \
 && cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" app.jar

# ---------- Etapa 2: imagen final, solo con lo necesario para correr ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

# El plan gratis de Render tiene 512 MB de RAM: se limita la memoria de la JVM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

# Render inyecta el puerto via $PORT. Spring lo lee con server.port=${PORT:8081}.
# No se fija un EXPOSE estatico para evitar discrepancias.
EXPOSE ${PORT:-8081}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
