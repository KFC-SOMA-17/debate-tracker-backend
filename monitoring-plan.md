# 모니터링 배포 체크리스트 (사용자 작업)

코드/설정 작업은 모두 끝났다(`monitoring/`, `docker-compose.dev.yml`, `.github/workflows/Monitoring_CD.yml`,
`docs/monitoring.md`). 남은 것은 **AWS 인프라 준비 + GitHub Secrets 등록**으로, 콘솔/CLI 권한이 필요해 직접 해야 한다.
아래 순서대로 진행하면 develop 에 `monitoring/**` push 시 자동 배포된다.

상세 설명은 [docs/monitoring.md](docs/monitoring.md) 참고. 이 문서는 **할 일 체크리스트**다.

---

## 0. 사전 정보 수집

- [ ] 앱 EC2 의 **프라이빗 IP** 확인 (Prometheus 스크랩 타깃용)
- [ ] 앱 EC2 의 **보안그룹 ID** 확인
- [ ] RDS **DBInstanceIdentifier**, ElastiCache 캐시(클러스터) 이름 확인 (대시보드 필터링용, 선택)
- [ ] DockerHub `debatetracker` 네임스페이스 push 권한 확인

---

## 1. 전용 모니터링 EC2 생성

- [x] EC2 인스턴스 생성 — 리전 **ap-northeast-2**, 앱 EC2 와 **같은 VPC** (프라이빗 IP 스크랩을 위해)
  - 권장 사양: t3.small 이상 (Prometheus TSDB + Grafana). 스토리지 20GB+ (TSDB 보존)
- [x] Docker + compose plugin 설치:
  ```bash
  sudo apt-get update && sudo apt-get install -y docker.io docker-compose-plugin
  sudo usermod -aG docker $USER   # 재로그인 필요
  ```
- [ ] (배포 디렉토리 `~/monitoring/` 는 CD 의 scp 가 자동 생성 — 수동 생성 불필요)

## 2. IAM 인스턴스 역할 (CloudWatch 읽기)

- [x] IAM 역할 생성 후 **모니터링 EC2 에 attach**
  - 정책: `CloudWatchReadOnlyAccess` (또는 최소 권한: `cloudwatch:GetMetricData`,
    `cloudwatch:GetMetricStatistics`, `cloudwatch:ListMetrics`, `tag:GetResources`)
- [x] **IMDSv2 hop limit = 2** 설정 (1이면 컨테이너가 IAM 자격증명을 못 받음):
  ```bash
  aws ec2 modify-instance-metadata-options \
    --instance-id <monitoring-ec2-id> \
    --http-put-response-hop-limit 2 --http-tokens required
  ```

## 3. 보안그룹

- [x] **앱 EC2 SG 인바운드**: 출처 = 모니터링 EC2 SG, 포트 `8083`(actuator), `9100`(node_exporter) 허용
- [x] **모니터링 EC2 SG 인바운드**: 출처 = 관리자 IP, 포트 `3000`(Grafana), `22`(SSH)
  - `9090`(Prometheus), `9106`(cloudwatch-exporter) 는 외부 비노출 유지
- [x] (확인) RDS/ElastiCache SG 가 앱 EC2 접근을 이미 허용 중인지 — 모니터링은 CloudWatch 경유라 직접 접근 불필요

## 4. DockerHub 리포지토리 (public으로 두고 CD 실행시 자동으로 실행하는 걸 기대중)

- [x] `debatetracker/debate-monitoring-prometheus`
- [x] `debatetracker/debate-monitoring-grafana`
- [x] `debatetracker/debate-monitoring-cloudwatch-exporter`

## 5. GitHub Secrets 등록

Settings → Secrets and variables → Actions:

- [x] `MONITORING_EC2_HOST` — 모니터링 EC2 퍼블릭 IP/도메인
- [x] `MONITORING_EC2_USER` — SSH 계정 (`ubuntu` 등)
- [x] `MONITORING_EC2_SSH_KEY` — SSH 개인키(PEM 전문)
- [x] `APP_EC2_PRIVATE_IP` — 앱 EC2 프라이빗 IP (`:8083`/`:9100` 타깃 조립용)
- [x] `GRAFANA_ADMIN_PASSWORD` — Grafana admin 비밀번호
- [x] (기존 재사용 확인) `DEV_DOCKERHUB_USERNAME`, `DEV_DOCKERHUB_TOKEN`

## 6. (선택) 환경별 식별자 좁히기

- [ ] 특정 RDS/ElastiCache 만 보려면 `monitoring/cloudwatch-exporter/config.yml` 의 각 metric 에
  `DBInstanceIdentifier` / `clusterId` 값 추가 (미지정 시 계정 내 전체 조회)

## 7. node_exporter 가 앱 EC2 에 반영되도록 배포

- [ ] `docker-compose.dev.yml`(node_exporter 추가본)이 develop 에 머지되면 **앱 CD(`Dev CD`)** 가
  새 compose 를 앱 EC2 로 전달 → node_exporter 컨테이너 기동 -> 현재 브린치 기준으로 한번 배포할 예정
- [ ] 앱 EC2 에서 `curl localhost:9100/metrics` 로 노출 확인

## 8. 모니터링 스택 배포 & 검증

- [ ] develop 에 `monitoring/**` 변경 push (또는 Actions 에서 `Monitoring CD` 수동 실행)
- [ ] GitHub Actions `Monitoring CD` 성공 + 헬스체크 통과 확인
- [ ] Prometheus `http://<monitoring-ec2>:9090/targets` — `app-debate`·`node-exporter`·`cloudwatch-exporter` 모두 **UP**
- [ ] Grafana `http://<monitoring-ec2>:3000` 로그인 → 대시보드 4종 데이터 표시
  - `app-service` / `jvm-app` / `ec2-node` / `aws-rds-elasticache`
- [ ] (데이터 유지 검증) `docker compose -f docker-compose.monitoring.yml down && up -d` 후 기존 시계열·대시보드 보존
- [ ] (분리 검증) 모니터링만 변경 push → `Monitoring CD` 만 동작, `Dev CD` skip 확인

---

## 트러블슈팅 빠른 참조

| 증상 | 원인/조치 |
|------|-----------|
| Grafana 의 RDS/ElastiCache 패널이 빈 값 | IAM 역할 미attach 또는 **IMDSv2 hop limit=1**. 2단계 재확인 |
| `app-debate`/`node-exporter` 타깃 DOWN | 앱 EC2 SG 인바운드(8083/9100) 미허용 또는 `APP_EC2_PRIVATE_IP` 오타 |
| `cloudwatch-exporter` 타깃 DOWN | 컨테이너 로그 확인(`docker logs monitoring-cloudwatch-exporter`), 리전·권한 확인 |
| 재배포 후 데이터 사라짐 | `down -v` 로 볼륨 삭제했는지 확인. named volume 유지 시 보존됨 |
| 이미지 pull 실패 | DockerHub 로그인/리포 존재/태그(sha) 확인 |
