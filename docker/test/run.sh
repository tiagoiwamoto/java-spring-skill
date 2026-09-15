#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
REPORT_DIR="$SCRIPT_DIR/reports"
NETWORK="java-spring-skill_default"
IMAGE_TAG="skill-tester"

mkdir -p "$REPORT_DIR"

if ! docker network ls --format '{{.Name}}' | grep -q "^${NETWORK}$"; then
    echo "ERRO: Rede Docker '${NETWORK}' nao encontrada."
    echo "Suba os servicos primeiro com: docker compose up -d"
    exit 1
fi

echo "Construindo imagem de testes..."
docker build -t "$IMAGE_TAG" -f "$SCRIPT_DIR/Dockerfile" "$PROJECT_DIR"

echo "Executando testes..."
docker run --rm \
    --network "$NETWORK" \
    -e REPORT_PATH=/reports/report.html \
    -v "$REPORT_DIR:/reports" \
    "$IMAGE_TAG"

REPORT_FILE="$REPORT_DIR/report.html"
if [ -f "$REPORT_FILE" ]; then
    echo ""
    echo "Relatorio gerado com sucesso:"
    echo "  $REPORT_FILE"
    echo ""
    echo "Abra no navegador: file://$REPORT_FILE"
else
    echo "ERRO: Relatorio nao foi gerado em $REPORT_FILE"
    exit 1
fi
