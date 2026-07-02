FROM prom/cloudwatch-exporter:v0.16.0

# 자격증명은 모니터링 EC2 의 IAM 인스턴스 역할에서 자동 획득 (키 파일 미사용).
COPY config.yml /config/config.yml

CMD ["/config/config.yml"]
