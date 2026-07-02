#!/bin/sh
set -e

# 환경별 스크랩 타깃을 런타임에 주입한다. prom/prometheus 이미지엔 envsubst 가 없어 sed 로 치환한다.
ENVIRONMENT="${ENVIRONMENT:-dev}"
# 필수 scrape target 은 기본값으로 숨기지 않는다. 비어 있으면 잘못된 타깃으로 조용히 배포돼
# prometheus 프로세스만 살아 있고 실제 수집은 실패하는 상태가 되므로, 여기서 바로 실패시킨다.
: "${APP_DEBATE_TARGET:?APP_DEBATE_TARGET is required}"
: "${NODE_EXPORTER_TARGET:?NODE_EXPORTER_TARGET is required}"

sed \
  -e "s|\${ENVIRONMENT}|${ENVIRONMENT}|g" \
  -e "s|\${APP_DEBATE_TARGET}|${APP_DEBATE_TARGET}|g" \
  -e "s|\${NODE_EXPORTER_TARGET}|${NODE_EXPORTER_TARGET}|g" \
  /etc/prometheus/prometheus.yml.tmpl \
  > /etc/prometheus/prometheus.yml

exec /bin/prometheus \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --web.console.libraries=/usr/share/prometheus/console_libraries \
  --web.console.templates=/usr/share/prometheus/consoles
