FROM eclipse-temurin:11.0.22_7-jre-focal

ARG jar

RUN test -n "$jar"

RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

COPY $jar /app/iexec-sms.jar

COPY src/main/resources/ssl-keystore-dev.p12 /app/ssl-keystore-dev.p12

ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/iexec-sms.jar" ]
