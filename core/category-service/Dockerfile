FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
RUN apt-get update && apt-get install -y wget curl && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]

