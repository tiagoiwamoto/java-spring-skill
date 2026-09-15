FROM eclipse-temurin:25-jdk AS build

WORKDIR /build
COPY . .

RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:25-jdk

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /build/target/*.jar /app/app.jar
COPY docker/app/entrypoint.sh /entrypoint.sh
COPY docker/kafka/certs/ca-cert /certs/ca-cert

RUN chmod +x /entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
