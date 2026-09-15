#!/usr/bin/env python3
import datetime
import json
import os
import re
import subprocess
import time
import urllib.request
from jinja2 import Template

REPORT_PATH = os.environ.get("REPORT_PATH", "/reports/report.html")
REPORT_DIR = os.path.dirname(REPORT_PATH)
os.makedirs(REPORT_DIR, exist_ok=True)

results = []


def run(cmd, timeout=60):
    try:
        p = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
        return p.returncode, p.stdout, p.stderr
    except subprocess.TimeoutExpired:
        return -1, "", "timeout"


def add_result(name, status, detail=""):
    results.append({
        "name": name,
        "status": status,
        "detail": detail.strip()[:500],
        "time": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    })


def http_get(url, timeout=10):
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", errors="ignore")
    except Exception as e:
        return -1, str(e)


def wait_for_service(url, attempts=20, delay=2):
    for _ in range(attempts):
        status, _ = http_get(url)
        if status == 200:
            return True
        time.sleep(delay)
    return False


def test_localstack_health():
    status, body = http_get("http://localstack:4566/_localstack/health")
    if status == 200:
        add_result("LocalStack Health", "PASS", f"HTTP {status} - {body[:100]}")
    else:
        add_result("LocalStack Health", "FAIL", f"HTTP {status} - {body}")


def test_sns_publish():
    rc, out, err = run(
        "aws --endpoint-url http://localstack:4566 --region us-east-1 "
        "sns publish --topic-arn arn:aws:sns:us-east-1:000000000000:skill-topic --message 'teste-sns'"
    )
    if rc == 0:
        add_result("SNS Publish", "PASS", out.strip()[:200])
    else:
        add_result("SNS Publish", "FAIL", err or out)


def test_sqs_send_receive():
    queue_url = "http://localstack:4566/000000000000/skill-queue"
    rc, out, err = run(
        f"aws --endpoint-url http://localstack:4566 --region us-east-1 "
        f"sqs send-message --queue-url {queue_url} --message-body 'teste-sqs'"
    )
    if rc != 0:
        add_result("SQS Send", "FAIL", err or out)
        return
    add_result("SQS Send", "PASS", out.strip()[:200])

    rc, out, err = run(
        f"aws --endpoint-url http://localstack:4566 --region us-east-1 "
        f"sqs receive-message --queue-url {queue_url} --max-number-of-messages 1"
    )
    if rc == 0 and "teste-sqs" in out:
        add_result("SQS Receive", "PASS", "Mensagem recebida")
    else:
        add_result("SQS Receive", "FAIL", err or out)


def test_kafka_plain_text():
    topic = "skill.plain-text"
    msg = f"teste-plain-{datetime.datetime.now().isoformat()}"
    rc, out, err = run(
        f"bash -c 'echo {msg} | kafka-console-producer --bootstrap-server kafka:29092 --topic {topic}'",
        timeout=30,
    )
    if rc != 0:
        add_result("Kafka Plain Text Producer", "FAIL", err or out)
        return

    time.sleep(2)
    rc, out, err = run(
        f"kafka-console-consumer --bootstrap-server kafka:29092 --topic {topic} "
        f"--from-beginning --max-messages 10 --timeout-ms 10000",
        timeout=30,
    )
    if rc == 0 and msg in out:
        add_result("Kafka Plain Text", "PASS", f"Mensagem '{msg}' publicada e consumida")
    else:
        add_result("Kafka Plain Text", "FAIL", f"{err or out}")


def test_kafka_ssl():
    topic = "skill.ssl.avro"
    msg = f"teste-ssl-{datetime.datetime.now().isoformat()}"

    config_path = "/tmp/ssl-producer.config"
    with open(config_path, "w") as f:
        f.write("security.protocol=SSL\n")
        f.write("ssl.truststore.location=/certs/kafka.server.truststore.jks\n")
        f.write("ssl.truststore.password=changeit\n")

    rc, out, err = run(
        f"bash -c 'echo {msg} | kafka-console-producer --bootstrap-server kafka:9093 --topic {topic} "
        f"--producer.config {config_path}'",
        timeout=30,
    )
    if rc != 0:
        add_result("Kafka SSL Producer", "FAIL", err or out)
        return

    time.sleep(2)
    rc, out, err = run(
        f"kafka-console-consumer --bootstrap-server kafka:9093 --topic {topic} "
        f"--consumer.config {config_path} --from-beginning --max-messages 10 --timeout-ms 10000",
        timeout=30,
    )
    if rc == 0 and msg in out:
        add_result("Kafka SSL", "PASS", f"Mensagem '{msg}' publicada via SSL e consumida")
    else:
        add_result("Kafka SSL", "FAIL", f"{err or out}")


def test_schema_registry():
    status, body = http_get("http://schema-registry:8081/subjects", timeout=10)
    if status == 200:
        add_result("Schema Registry", "PASS", f"HTTP {status} - subjects: {body[:200]}")
    else:
        add_result("Schema Registry", "FAIL", f"HTTP {status} - {body}")


def test_app_health():
    if not wait_for_service("http://app:8080/actuator/health", attempts=30, delay=2):
        add_result("App Health", "FAIL", "Aplicacao nao respondeu no /actuator/health")
        return
    status, body = http_get("http://app:8080/actuator/health")
    if status == 200:
        add_result("App Health", "PASS", body[:200])
    else:
        add_result("App Health", "FAIL", f"HTTP {status} - {body}")


def test_viacep():
    status, body = http_get("https://viacep.com.br/ws/01001000/json/", timeout=15)
    if status == 200 and "cep" in body:
        add_result("ViaCEP", "PASS", body[:200])
    else:
        add_result("ViaCEP", "FAIL", f"HTTP {status} - {body[:200]}")


def generate_html():
    passed = sum(1 for r in results if r["status"] == "PASS")
    failed = sum(1 for r in results if r["status"] == "FAIL")
    total = len(results)

    template = Template("""
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Relatório de Validação - java-spring-skill</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 2rem; background: #f5f5f5; }
        h1 { color: #333; }
        .summary { background: #fff; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .summary span { font-weight: bold; margin-right: 1rem; }
        .pass { color: green; }
        .fail { color: red; }
        table { width: 100%; border-collapse: collapse; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        th, td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #ddd; }
        th { background: #333; color: #fff; }
        tr:hover { background: #f1f1f1; }
        .status-pass { color: green; font-weight: bold; }
        .status-fail { color: red; font-weight: bold; }
        .detail { font-size: 0.85rem; color: #555; max-width: 500px; word-break: break-word; }
    </style>
</head>
<body>
    <h1>Relatório de Validação - java-spring-skill</h1>
    <p>Gerado em: {{ generated_at }}</p>
    <div class="summary">
        <span>Total: {{ total }}</span>
        <span class="pass">Passaram: {{ passed }}</span>
        <span class="fail">Falharam: {{ failed }}</span>
    </div>
    <table>
        <thead>
            <tr>
                <th>Horário</th>
                <th>Teste</th>
                <th>Status</th>
                <th>Detalhes</th>
            </tr>
        </thead>
        <tbody>
            {% for r in results %}
            <tr>
                <td>{{ r.time }}</td>
                <td>{{ r.name }}</td>
                <td class="status-{{ r.status.lower() }}">{{ r.status }}</td>
                <td class="detail">{{ r.detail }}</td>
            </tr>
            {% endfor %}
        </tbody>
    </table>
</body>
</html>
""")

    html = template.render(
        results=results,
        total=total,
        passed=passed,
        failed=failed,
        generated_at=datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    )
    with open(REPORT_PATH, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"Relatorio HTML gerado em: {REPORT_PATH}")


def main():
    print("Aguardando servicos ficarem prontos...")
    time.sleep(5)

    test_localstack_health()
    test_sns_publish()
    test_sqs_send_receive()
    test_kafka_plain_text()
    test_kafka_ssl()
    test_schema_registry()
    test_app_health()
    test_viacep()

    generate_html()

    failed = sum(1 for r in results if r["status"] == "FAIL")
    if failed > 0:
        print(f"{failed} teste(s) falharam.")
        return 1
    print("Todos os testes passaram.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
