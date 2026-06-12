#!/bin/bash
# Tick app server bootstrap.
# Docker + compose 플러그인 설치, /opt/tick 디렉터리 + Caddyfile + compose.prod.yaml 배치.
# 시크릿 (.env) 은 첫 부팅 후 SSM Session Manager 로 들어와서 사용자가 직접 채움.

set -euxo pipefail

dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user

mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL \
  "https://github.com/docker/compose/releases/download/v2.30.0/docker-compose-linux-aarch64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

mkdir -p /opt/tick/caddy
chown -R ec2-user:ec2-user /opt/tick

cat > /opt/tick/caddy/Caddyfile <<'CADDY'
# 도메인 붙이기 전: HTTP. 도메인 + Route53 A 레코드 붙이면 :80 을 도메인명으로 바꾸면
# Caddy 가 자동으로 Let's Encrypt cert 발급 + HTTPS 강제.
#
# 무중단 배포: backend-a / backend-b 두 컨테이너를 round-robin 으로 라우팅.
# active health check (5초 주기) + passive failure (1회 실패 시 10초 격리) 로
# 배포 중 한 쪽이 죽어도 다른 쪽으로 즉시 페일오버.
(backend_upstream) {
    reverse_proxy backend-a:8080 backend-b:8080 {
        lb_policy round_robin
        health_uri /actuator/health
        health_interval 5s
        health_timeout 3s
        health_status 2xx
        fail_duration 10s
        max_fails 1
        unhealthy_status 5xx
    }
}

:80 {
    encode gzip

    handle /api/* {
        import backend_upstream
    }
    handle /oauth2/* {
        import backend_upstream
    }
    handle /login/* {
        import backend_upstream
    }
    handle /actuator/health {
        import backend_upstream
    }
    handle {
        reverse_proxy frontend:3000
    }
}
CADDY

cat > /opt/tick/compose.prod.yaml <<COMPOSE
# backend 는 a/b 두 컨테이너로 띄워서 Caddy 가 round-robin + active health check.
# CD 가 한 번에 한 쪽씩 force-recreate 하면 다른 쪽이 트래픽 받아서 무중단.
# Flyway 마이그레이션은 동시 부팅해도 lock 으로 serialise 되니 안전.
# 단, 스키마 변경은 backward-compatible 한 변경만 (NOT NULL 추가 X).

x-backend: &backend
  image: ${backend_image}:latest
  restart: unless-stopped
  depends_on:
    postgres:
      condition: service_healthy
  mem_limit: 768m
  environment:
    SPRING_PROFILES_ACTIVE: prod
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tick
    SPRING_DATASOURCE_USERNAME: tick
    SPRING_DATASOURCE_PASSWORD: \$${POSTGRES_PASSWORD}
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID: \$${KAKAO_CLIENT_ID}
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET: \$${KAKAO_CLIENT_SECRET}
    TICK_JWT_SECRET: \$${TICK_JWT_SECRET}
    TICK_CORS_ALLOWED_ORIGINS: \$${TICK_PUBLIC_URL}
    TICK_AUTH_FRONTEND_CALLBACK_URL: \$${TICK_PUBLIC_URL}/auth/callback
    TICK_AUTH_FRONTEND_LOGIN_URL: \$${TICK_PUBLIC_URL}/login

services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: tick
      POSTGRES_USER: tick
      POSTGRES_PASSWORD: \$${POSTGRES_PASSWORD}
      TZ: Asia/Seoul
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U tick -d tick"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend-a:
    <<: *backend
    container_name: tick-backend-a

  backend-b:
    <<: *backend
    container_name: tick-backend-b

  frontend:
    image: ${frontend_image}:latest
    restart: unless-stopped
    environment:
      NEXT_PUBLIC_API_URL: \$${TICK_PUBLIC_URL}

  caddy:
    image: caddy:2-alpine
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./caddy/Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    depends_on:
      - frontend
      - backend-a
      - backend-b

volumes:
  pgdata:
  caddy_data:
  caddy_config:
COMPOSE

if [ ! -f /opt/tick/.env ]; then
cat > /opt/tick/.env <<'ENVFILE'
# Tick production secrets — SSM Session Manager 로 들어와서 직접 수정하세요.
POSTGRES_PASSWORD=change_me_strong_password
KAKAO_CLIENT_ID=change_me
KAKAO_CLIENT_SECRET=change_me
TICK_JWT_SECRET=change_me_64char_hex
TICK_PUBLIC_URL=http://CHANGE_ME_TO_EIP_OR_DOMAIN
ENVFILE
chmod 600 /opt/tick/.env
fi

# ===== Postgres daily backup → S3 =====
mkdir -p /opt/tick/scripts /var/log/tick
cat > /opt/tick/scripts/pg-backup.sh <<BACKUP
#!/bin/bash
set -euo pipefail

BACKUP_BUCKET="${backup_bucket}"
AWS_REGION="${aws_region}"
TS=\$(date -u +%Y%m%dT%H%M%SZ)
TARGET=/tmp/tick-pgdump-\$TS.sql.gz
LOG=/var/log/tick/pg-backup.log

cd /opt/tick

if ! docker compose -f compose.prod.yaml --env-file .env exec -T postgres \
    pg_dump -U tick -d tick --no-owner --clean --if-exists \
  | gzip > "\$TARGET"; then
  echo "[\$TS] pg_dump 실패" | tee -a "\$LOG" >&2
  exit 1
fi

# 빈 dump 방지 (gzip 헤더 + 본문이 거의 100 bytes 미만이면 의심)
if [ "\$(wc -c < "\$TARGET")" -lt 100 ]; then
  echo "[\$TS] dump 크기가 비정상 (< 100 bytes)" | tee -a "\$LOG" >&2
  rm -f "\$TARGET"
  exit 1
fi

aws s3 cp "\$TARGET" "s3://\$BACKUP_BUCKET/postgres/tick-\$TS.sql.gz" \
    --region "\$AWS_REGION" --no-progress \
  | tee -a "\$LOG"

rm -f "\$TARGET"
echo "[\$TS] backup OK" | tee -a "\$LOG"
BACKUP
chmod +x /opt/tick/scripts/pg-backup.sh

# 매일 03:30 KST = 18:30 UTC. cron 은 UTC 로 해석되므로 18:30 사용.
cat > /etc/cron.d/tick-pgbackup <<'CRON'
30 18 * * * root /opt/tick/scripts/pg-backup.sh >> /var/log/tick/pg-backup.log 2>&1
CRON
chmod 644 /etc/cron.d/tick-pgbackup

# AL2023 는 cronie 가 기본 없음 → 설치 + 시작
dnf install -y cronie
systemctl enable --now crond

chown -R ec2-user:ec2-user /opt/tick

# 첫 부팅 시점엔 이미지가 ECR 에 없을 수 있어서 compose up 은 안 함.
# 첫 배포는 CD 워크플로우 또는 사용자가 SSM 으로 들어와서 수동 실행.
