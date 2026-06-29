FROM grafana/grafana:11.5.0

# datasource·대시보드 프로비저닝을 이미지에 베이크. admin 비밀번호는 런타임 env 로 주입.
COPY provisioning/ /etc/grafana/provisioning/
COPY dashboards/ /var/lib/grafana/dashboards/

ENV GF_USERS_ALLOW_SIGN_UP=false
