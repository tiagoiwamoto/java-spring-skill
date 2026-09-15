#!/bin/bash
set -e

echo "Inicializando recursos AWS no LocalStack..."

awslocal sns create-topic --name skill-topic --region us-east-1
awslocal sqs create-queue --queue-name skill-queue --region us-east-1

echo "Recursos criados com sucesso."
