#!/bin/sh
set -e

# 환경별 스크랩 타깃을 런타임에 주입한다. prom/prometheus 이미지엔 envsubst 가 없어 sed 로 치환한다.
ENVIRONMENT="${ENVIRONMENT:-dev}"
APP_DEBATE_TARGET="${APP_DEBATE_TARGET:-host.docker.internal:8083}"
NODE_EXPORTER_TARGET="${NODE_EXPORTER_TARGET:-host.docker.internal:9100}"

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
