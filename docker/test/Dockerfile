FROM confluentinc/cp-kafka:7.8.0

USER root

RUN yum install -y python3 python3-pip jq && \
    pip3 install --no-cache-dir boto3 requests jinja2 && \
    yum clean all

WORKDIR /tests

COPY docker/test/run-tests.py /tests/run-tests.py
COPY docker/kafka/certs/ca-cert /certs/ca-cert

ENV PYTHONUNBUFFERED=1

ENTRYPOINT ["python3", "/tests/run-tests.py"]
