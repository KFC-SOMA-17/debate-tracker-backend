# Docker 환경 구성

이 디렉토리는 로컬 개발 및 테스트를 위한 Docker Compose 설정과 모니터링 스택 구성을 포함합니다.

## 디렉토리 구조

```
docker/
├── README.md                                    # 이 파일
└── monitoring/                                  # 모니터링 설정
    ├── prometheus.yml                           # Prometheus 수집 설정
    └── grafana/
        └── provisioning/
            ├── datasources/
            │   └── prometheus.yml               # Prometheus 데이터 소스
            └── dashboards/
                └── dashboard.yml                # 대시보드 프로비저닝
```

---

## 1. 기본 인프라 실행

### MySQL + Redis 실행

```bash
docker-compose up -d mysql redis
```

### 앱 실행 (로컬 또는 Docker)

**로컬 실행**:
```bash
./gradlew :app-debate:bootRun
```

**Docker 실행**:
```bash
docker-compose --profile app up -d
```

---

## 2. 모니터링 스택 실행

### 모니터링 스택 시작

```bash
docker-compose --profile monitoring up -d
```

### 접속 정보

| 서비스 | URL | 계정 |
|--------|-----|------|
| **App-Debate** | http://localhost:8080 | - |
| **Actuator** | http://localhost:8083/monitoring | - |
| **Prometheus** | http://localhost:19090 | - |
| **Grafana** | http://localhost:13000 | admin/admin |

---

## 3. 환경 변수

### 인프라 설정

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `MYSQL_HOST_PORT` | 13306 | 호스트에서 MySQL 접속 포트 |
| `MYSQL_DATABASE` | debate | 데이터베이스 이름 |
| `MYSQL_USER` | debate | MySQL 사용자 |
| `MYSQL_PASSWORD` | debate | MySQL 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | root | MySQL root 비밀번호 |
| `REDIS_HOST_PORT` | 16379 | 호스트에서 Redis 접속 포트 |

### 모니터링 설정

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `PROMETHEUS_HOST_PORT` | 19090 | Prometheus 포트 |
| `GRAFANA_HOST_PORT` | 13000 | Grafana 포트 |
| `GRAFANA_ADMIN_USER` | admin | Grafana 관리자 계정 |
| `GRAFANA_ADMIN_PASSWORD` | admin | Grafana 관리자 비밀번호 |

### 사용 예시

**포트 충돌 해결**:
```bash
GRAFANA_HOST_PORT=3001 docker-compose --profile monitoring up -d
```

**Grafana 계정 변경**:
```bash
GRAFANA_ADMIN_USER=myuser GRAFANA_ADMIN_PASSWORD=mypass docker-compose --profile monitoring up -d
```

**전체 환경 변수 설정**:
```bash
MYSQL_HOST_PORT=3307 \
REDIS_HOST_PORT=6380 \
PROMETHEUS_HOST_PORT=9091 \
GRAFANA_HOST_PORT=3001 \
GRAFANA_ADMIN_USER=admin \
GRAFANA_ADMIN_PASSWORD=secret123 \
docker-compose up -d
```

---

## 4. 주요 Actuator 엔드포인트

### Health Check
```bash
curl http://localhost:8083/monitoring/health
```

**응답 예시**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP", "details": {...}},
    "redis": {"status": "UP", "details": {...}},
    "diskSpace": {"status": "UP", "details": {...}}
  }
}
```

### Prometheus Metrics
```bash
curl http://localhost:8083/monitoring/prometheus
```

**주요 메트릭**:
- `hikaricp_connections_active`: DB 활성 연결 수
- `jvm_memory_used_bytes`: JVM 메모리 사용량
- `http_server_requests_seconds`: HTTP 요청 지연시간
- `system_cpu_usage`: 시스템 CPU 사용률

### 전체 메트릭 목록
```bash
curl http://localhost:8083/monitoring/metrics
```

### 런타임 로그 레벨 변경 (Dev/Local만)
```bash
# 현재 로그 레벨 확인
curl http://localhost:8083/monitoring/loggers/com.debatetracker.debate

# 로그 레벨 변경
curl -X POST http://localhost:8083/monitoring/loggers/com.debatetracker.debate \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

### Thread Dump (Dev/Local만)
```bash
curl http://localhost:8083/monitoring/threaddump > threads.json
```

---

## 5. Prometheus 쿼리 예시

Prometheus UI (http://localhost:19090)에서 다음 쿼리 실행:

### DB Connection Pool 사용률
```promql
hikaricp_connections_active{application="app-debate"} / hikaricp_connections_max{application="app-debate"} * 100
```

### JVM Heap 사용률
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### HTTP 요청 P95 응답시간 (초 단위)
```promql
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket{application="app-debate"}[5m])
)
```

### 시스템 CPU 사용률
```promql
system_cpu_usage{application="app-debate"} * 100
```

---

## 6. Grafana 대시보드 구성

### 기본 패널 추천

**1. JVM 메모리**
- Query: `jvm_memory_used_bytes{application="app-debate", area="heap"}`
- Visualization: Time series (line chart)

**2. DB Connection Pool**
- Query: `hikaricp_connections_active{application="app-debate"}`
- Visualization: Gauge (현재 값 + 최대값)

**3. HTTP Request Rate**
- Query: `rate(http_server_requests_seconds_count{application="app-debate"}[1m])`
- Visualization: Graph (요청/초)

**4. GC Count**
- Query: `rate(jvm_gc_pause_seconds_count{application="app-debate"}[1m])`
- Visualization: Time series (GC 빈도)

### 커뮤니티 대시보드 추천

Grafana에서 즉시 사용 가능한 검증된 대시보드:

1. **JVM (Micrometer)** - ID: 4701
   - Grafana → Dashboards → Import → 4701 입력
   - JVM 메모리, GC, 스레드 등 종합 모니터링

2. **Spring Boot 2.1 Statistics** - ID: 10280
   - Grafana → Dashboards → Import → 10280 입력
   - Spring Boot Actuator 메트릭 기반 대시보드

3. **Spring Boot APM Dashboard** - ID: 12900
   - Grafana → Dashboards → Import → 12900 입력
   - HTTP 요청, DB Pool, Redis 상태 통합 뷰

**Import 방법**:
1. Grafana 접속 (http://localhost:13000)
2. 좌측 메뉴 → Dashboards → Import
3. "Import via grafana.com" 입력란에 Dashboard ID 입력
4. Prometheus 데이터 소스 선택
5. Import 클릭

---

## 7. 환경별 설정 차이

| 항목 | Local/Dev | Prod |
|------|-----------|------|
| 설정 파일 | `config-monitoring-dev.yml` | `config-monitoring-prod.yml` |
| 노출 엔드포인트 | health, prometheus, metrics, info, **loggers**, **threaddump** | health, prometheus, metrics, info |
| 로그 레벨 변경 | ✅ 가능 | ❌ 불가 (재배포 필요) |
| Thread Dump | ✅ 가능 | ❌ 불가 (jstack 수동) |

---

## 8. 서비스 중지

### 모니터링 스택만 중지
```bash
docker-compose --profile monitoring down
```

### 전체 중지
```bash
docker-compose down
```

### 볼륨까지 삭제 (데이터 초기화)
```bash
docker-compose down -v
```

---

## 9. 트러블슈팅

### Prometheus가 메트릭을 수집하지 못할 때

1. 앱이 실행 중인지 확인:
```bash
curl http://localhost:8083/monitoring/health
```

2. Prometheus 타겟 상태 확인:
   - http://localhost:19090/targets 접속
   - `app-debate` 타겟이 UP 상태여야 함

3. Docker 네트워크 문제 (Linux):
```bash
# host.docker.internal이 작동하지 않으면 docker-compose.yml에 추가
prometheus:
  extra_hosts:
    - "host.docker.internal:host-gateway"
```

### Grafana에서 Prometheus 연결 실패

1. Datasource 상태 확인:
   - Grafana → Configuration → Data Sources → Prometheus
   - "Save & Test" 버튼 클릭

2. 네트워크 확인:
```bash
docker exec debate-grafana ping prometheus
```

### 포트 충돌

포트가 이미 사용 중일 때:
```bash
# 사용 중인 포트 확인
lsof -i :13306  # MySQL
lsof -i :16379  # Redis
lsof -i :19090  # Prometheus
lsof -i :13000  # Grafana

# 환경 변수로 포트 변경
MYSQL_HOST_PORT=3307 GRAFANA_HOST_PORT=3001 docker-compose up -d
```

---

## 참고 자료

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/3.5.14/reference/html/actuator.html)
- [Prometheus Documentation](https://prometheus.io/docs/introduction/overview/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Grafana Community Dashboards](https://grafana.com/grafana/dashboards/)
