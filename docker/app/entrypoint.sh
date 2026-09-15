#!/bin/sh
set -e

CA_CERT=${CA_CERT_PATH:-/certs/ca-cert}
TRUSTSTORE=${JAVA_HOME}/lib/security/cacerts
TRUSTSTORE_PASSWORD=${TRUSTSTORE_PASSWORD:-changeit}

if [ -f "$CA_CERT" ]; then
    echo "Importando certificado CA do Kafka no truststore do JDK..."
    keytool -import -alias skill-kafka-ca -file "$CA_CERT" \
        -keystore "$TRUSTSTORE" -storepass "$TRUSTSTORE_PASSWORD" -noprompt || true
else
    echo "AVISO: Certificado CA nao encontrado em $CA_CERT"
fi

exec java -jar /app/app.jar "$@"
