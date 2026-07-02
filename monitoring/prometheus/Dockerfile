FROM prom/prometheus:v3.2.0

# config 를 이미지에 베이크. 환경별로 바뀌는 스크랩 타깃만 런타임 envsubst 로 주입한다.
COPY prometheus.yml.tmpl /etc/prometheus/prometheus.yml.tmpl
COPY rules/ /etc/prometheus/rules/
COPY entrypoint.sh /bin/entrypoint.sh

ENTRYPOINT ["/bin/sh", "/bin/entrypoint.sh"]
