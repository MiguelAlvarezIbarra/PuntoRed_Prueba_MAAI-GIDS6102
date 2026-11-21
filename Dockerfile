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

# Importar certificado SSL de gestopago.portalventas.net al TrustStore de Java (cacerts)
COPY src/main/resources/certs/gestopago-portalventas.crt /tmp/gestopago-portalventas.crt
RUN keytool -importcert -noprompt -trustcacerts \
      -alias gestopago-portalventas \
      -file /tmp/gestopago-portalventas.crt \
      -keystore "$JAVA_HOME/lib/security/cacerts" \
      -storepass changeit \
 && rm -f /tmp/gestopago-portalventas.crt

RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --chown=appuser:appgroup --from=build /app/app.jar app.jar

# El plan gratis de Render tiene 512 MB de RAM: se limita la memoria de la JVM y se activan protocolos TLS.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Djdk.tls.client.protocols=TLSv1.3,TLSv1.2"

# Render inyecta el puerto via $PORT. Spring lo lee con server.port=${PORT:8081}.
# No se fija un EXPOSE estatico para evitar discrepancias.
EXPOSE ${PORT:-8081}

USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
