# monitoring/ — 독립 모니터링 스택

앱과 분리된 전용 EC2 에서 Prometheus + Grafana + CloudWatch Exporter 를 Docker 로 운영한다.
운영 문서는 [docs/monitoring.md](../docs/monitoring.md) 참고. 이 디렉토리는 **배포되는 스택**이며,
루트 `docker-compose.yml` 의 `monitoring` 프로파일(로컬 개발용)과는 별개다.

## 구성

| 디렉토리 | 이미지 | 책임 |
|----------|--------|------|
| `prometheus/` | `FROM prom/prometheus` + config 베이크 | 앱(:8083)·node_exporter(:9100)·cloudwatch-exporter 스크랩. 타깃은 런타임 주입 |
| `cloudwatch-exporter/` | `FROM prom/cloudwatch-exporter` + config 베이크 | AWS/RDS·AWS/ElastiCache 지표를 IAM 역할로 조회 |
| `grafana/` | `FROM grafana/grafana` + provisioning·대시보드 베이크 | 대시보드 4종 자동 프로비저닝 |

대시보드: `app-service`(STT/WS/LLM/Debate), `jvm-app`(JVM/Tomcat/HikariCP/ThreadPool),
`ec2-node`(호스트 CPU/Mem/Disk/Net), `aws-rds-elasticache`(CloudWatch).

## 로컬 빌드/실행

```bash
# 이미지 빌드
docker build -t mon-prometheus monitoring/prometheus
docker build -t mon-grafana monitoring/grafana
docker build -t mon-cw-exporter monitoring/cloudwatch-exporter

# 기동 (로컬은 앱이 host 에 떠 있다고 가정)
PROMETHEUS_IMAGE=mon-prometheus \
GRAFANA_IMAGE=mon-grafana \
CLOUDWATCH_EXPORTER_IMAGE=mon-cw-exporter \
APP_DEBATE_TARGET=host.docker.internal:8083 \
NODE_EXPORTER_TARGET=host.docker.internal:9100 \
docker compose -f monitoring/docker-compose.monitoring.yml up -d
```

- Prometheus: http://localhost:9090 (`/targets` 로 스크랩 상태 확인)
- Grafana: http://localhost:3000 (admin / `GF_SECURITY_ADMIN_PASSWORD`)
- CloudWatch Exporter: http://localhost:9106/metrics (로컬에선 AWS 자격증명 없으면 비어 있음)

## 데이터 유지

`prometheus-data`·`grafana-data` named volume 에 TSDB·대시보드 상태가 저장된다.
`docker compose down && up -d`(이미지 교체 재배포 포함) 후에도 시계열·대시보드가 보존된다.
완전 초기화하려면 `docker compose ... down -v`.

## 환경별 값

- `APP_DEBATE_TARGET` / `NODE_EXPORTER_TARGET`: 앱 EC2 프라이빗 IP 기반 (`docs/monitoring.md`).
- `cloudwatch-exporter/config.yml`: `DBInstanceIdentifier`·ElastiCache `clusterId` 는 환경 값으로 채운다(차원 미지정 시 전체 조회).
