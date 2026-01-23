FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY recommendation-services/pom.xml ./recommendation-services/
COPY recommendation-services/common-proto ./recommendation-services/common-proto/
COPY recommendation-services/common-avro ./recommendation-services/common-avro/
COPY recommendation-services/analyzer-service ./recommendation-services/analyzer-service/
COPY recommendation-services/collector-service ./recommendation-services/collector-service/
COPY recommendation-services/aggregator-service ./recommendation-services/aggregator-service/
# Создать минимальные pom.xml для модулей, которые требуются в корневом pom.xml
RUN mkdir -p stats-server/client stats-server/common-dto stats-server/service core infra && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>ru.practicum</groupId><artifactId>stats-server</artifactId><version>0.0.1-SNAPSHOT</version></project>' > stats-server/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>ru.practicum</groupId><artifactId>stats-server-client</artifactId><version>0.0.1-SNAPSHOT</version></project>' > stats-server/client/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>ru.practicum</groupId><artifactId>stats-server-common-dto</artifactId><version>0.0.1-SNAPSHOT</version></project>' > stats-server/common-dto/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>ru.practicum</groupId><artifactId>stats-server-service</artifactId><version>0.0.1-SNAPSHOT</version></project>' > stats-server/service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>ru.practicum</groupId><artifactId>core</artifactId><version>0.0.1-SNAPSHOT</version></project>' > core/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>ru.practicum</groupId><artifactId>infra</artifactId><version>0.0.1-SNAPSHOT</version></project>' > infra/pom.xml

RUN mvn clean install -DskipTests -pl recommendation-services/common-proto,recommendation-services/common-avro,recommendation-services/analyzer-service -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/recommendation-services/analyzer-service/target/analyzer-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
