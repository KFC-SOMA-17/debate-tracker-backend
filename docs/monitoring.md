# 모니터링 가이드 (Monitoring CD)

앱과 **분리된 전용 EC2** 에서 Grafana + Prometheus + CloudWatch Exporter 를 Docker 로 운영하는
모니터링 환경 문서. 배포 파이프라인은 `app-debate` 의 [deployment.md](deployment.md) 와 같은 패턴
(이미지 빌드·push → SSH → docker compose up)을 따른다. 스택 소스는 [`monitoring/`](../monitoring/).

## 무엇을 보는가

1. **앱 서비스 + EC2 호스트** — `app-debate` 가 이미 노출하는 서비스 지표(STT/WS/LLM/Debate),
   JVM·Tomcat·**HikariCP**·**스레드풀(`executor_*`)**, 그리고 앱 EC2 의 CPU/Memory/Disk/Network(node_exporter).
2. **ElastiCache Valkey Serverless** — 메모리 사용률, ECPU, 커넥션, hit/miss, evictions (CloudWatch).
3. **RDS (MySQL)** — CPU, 커넥션, freeable memory, free storage, IOPS, latency (CloudWatch).

## 아키텍처

```
[모니터링 EC2]  IAM Role: CloudWatchReadOnlyAccess
  ├─ prometheus :9090         scrape → 앱 EC2 :8083/monitoring/prometheus
  │                                  → 앱 EC2 :9100 (node_exporter)
  │                                  → cloudwatch-exporter:9106
  ├─ cloudwatch-exporter :9106  AWS/RDS · AWS/ElastiCache (ap-northeast-2)
  └─ grafana :3000            대시보드 4종 자동 프로비저닝
  named volumes: prometheus-data, grafana-data  ← 재배포해도 유지

[앱 EC2]  (기존 dev)
  ├─ debate-app :8080 / :8083
  └─ node-exporter :9100  (docker-compose.dev.yml)
```

## 배포 흐름

```
GitHub Actions (develop 의 monitoring/** push / 수동 workflow_dispatch)
  └─ prometheus·grafana·cloudwatch-exporter 이미지 빌드 (config 베이크)
  └─ Docker Hub push (debatetracker/debate-monitoring-*:latest, :<sha>)
  └─ SSH → 모니터링 EC2
        └─ docker-compose.monitoring.yml 전달 (scp)
        └─ .env 생성 (이미지 태그 3개 + 스크랩 타깃 + Grafana 비번)
        └─ docker compose pull && up -d
        └─ /-/healthy(Prometheus) · /api/health(Grafana) 로 검증
```

- 앱 CD(`Dev_CD.yml`)와 **완전 분리** — `monitoring/**` 변경은 앱을 재배포하지 않고, 앱 변경은 모니터링을 재배포하지 않는다.
- 설정은 **이미지에 베이크**되고, 환경별로 바뀌는 스크랩 타깃만 런타임 env 로 주입한다(`prometheus/entrypoint.sh` 의 sed 치환).

## 필요한 GitHub Secrets

| Secret | 용도 |
|--------|------|
| `MONITORING_EC2_HOST` | 모니터링 EC2 퍼블릭 IP/도메인 (SSH/SCP) |
| `MONITORING_EC2_USER` | SSH 계정 (`ubuntu` 등) |
| `MONITORING_EC2_SSH_KEY` | SSH 개인키 (PEM 전문) |
| `APP_EC2_PRIVATE_IP` | 앱 EC2 프라이빗 IP — `:8083`/`:9100` 스크랩 타깃 조립 |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin 비밀번호 |
| `DEV_DOCKERHUB_USERNAME` / `DEV_DOCKERHUB_TOKEN` | **기존 재사용** (이미지 push/pull) |

> 모니터링 이미지에는 비밀이 포함되지 않으므로(자격증명은 IAM 역할) Docker Hub 가 public 이어도 앱 이미지만큼 위험하진 않다.
> 다만 일관성을 위해 private 권장.

## EC2 / IAM / 보안그룹 사전 준비

### 모니터링 EC2
1. Docker + compose plugin 설치 ([deployment.md](deployment.md) EC2 준비와 동일).
2. 배포 디렉토리는 SSH 계정 홈의 `monitoring/` (scp target — 자동 생성).
3. **IAM 인스턴스 역할** 에 `CloudWatchReadOnlyAccess`(또는 `cloudwatch:GetMetricData`,
   `cloudwatch:GetMetricStatistics`, `cloudwatch:ListMetrics`, `tag:GetResources` 스코프) attach.
4. **IMDSv2 hop limit = 2**. 기본값 1 이면 컨테이너(cloudwatch-exporter)가 인스턴스 메타데이터로
   IAM 자격증명을 받지 못한다.
   ```bash
   aws ec2 modify-instance-metadata-options \
     --instance-id <monitoring-ec2-id> \
     --http-put-response-hop-limit 2 --http-tokens required
   ```

### 보안그룹
- 앱 EC2 SG 인바운드: **모니터링 EC2 SG** 출처로 `8083`, `9100` 허용.
- 모니터링 EC2 SG 인바운드: 관리자 IP 출처로 `3000`(Grafana), `22`(SSH). `9090`/`9106` 은 외부 비노출.
- 두 EC2 는 동일 VPC 가정 → Prometheus 는 앱 EC2 **프라이빗 IP** 로 스크랩.

### 환경별 식별자
- `monitoring/cloudwatch-exporter/config.yml` 의 차원은 식별자를 지정하지 않아 계정 내 전체 RDS/ElastiCache 를
  조회한다. 특정 인스턴스만 보려면 각 metric 에 `DBInstanceIdentifier`/`clusterId` 값을 추가한다.

## 대시보드

Grafana(`http://<monitoring-ec2>:3000`) 에 자동 프로비저닝(파일 기반):

| UID | 제목 | 내용 |
|-----|------|------|
| `app-service` | App Service Metrics | STT/WS/LLM/Debate 서비스 지표 |
| `jvm-app` | JVM / Tomcat / HikariCP / ThreadPools | 메모리·GC·HikariCP·executor·Tomcat·HTTP |
| `ec2-node` | EC2 Host (node_exporter) | CPU/Memory/Disk/Network |
| `aws-rds-elasticache` | AWS RDS / ElastiCache | CloudWatch 인프라 지표 |

## 롤백

이전 커밋 sha 태그로 이미지를 되돌린다.
```bash
cd ~/monitoring
sed -i 's#:.*#:<previous-sha>#' .env   # 또는 .env 의 *_IMAGE 3줄을 직접 수정
docker compose -f docker-compose.monitoring.yml pull
docker compose -f docker-compose.monitoring.yml up -d
```
데이터(시계열·대시보드 상태)는 named volume 에 있어 롤백·재배포로 사라지지 않는다.

## 로컬 점검

[`monitoring/README.md`](../monitoring/README.md) 참고.
```bash
docker build -t mon-prometheus monitoring/prometheus
docker build -t mon-grafana monitoring/grafana
docker build -t mon-cw-exporter monitoring/cloudwatch-exporter
PROMETHEUS_IMAGE=mon-prometheus GRAFANA_IMAGE=mon-grafana CLOUDWATCH_EXPORTER_IMAGE=mon-cw-exporter \
APP_DEBATE_TARGET=host.docker.internal:8083 NODE_EXPORTER_TARGET=host.docker.internal:9100 \
docker compose -f monitoring/docker-compose.monitoring.yml up -d
```
