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

# Caddyfile: api.<root_domain> 만 받음. frontend 는 Vercel 이 처리.
# 도메인 명시 시 Caddy 가 자동으로 Let's Encrypt cert 발급 + HTTPS 강제.
# 무중단 배포: backend-a / backend-b round_robin + active health check.
cat > /opt/tick/caddy/Caddyfile <<CADDY
${caddy_host} {
    encode gzip

    @backend path /api/* /oauth2/* /login/* /actuator/health
    handle @backend {
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

    handle {
        respond "Tick API server. Frontend is hosted on Vercel." 200
    }
}
CADDY

# backend 는 a/b 두 컨테이너로 띄워서 Caddy 가 round_robin + active health check.
# CD 가 한 번에 한 쪽씩 force-recreate 하면 다른 쪽이 트래픽 받아서 무중단.
# Flyway 마이그레이션은 동시 부팅해도 lock 으로 serialise.
# 단, 스키마 변경은 backward-compatible 한 변경만 (NOT NULL 추가 X).
cat > /opt/tick/compose.prod.yaml <<COMPOSE
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
    SPRING_DATASOURCE_PASSWORD: $${POSTGRES_PASSWORD}
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID: $${KAKAO_CLIENT_ID}
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET: $${KAKAO_CLIENT_SECRET}
    TICK_JWT_SECRET: $${TICK_JWT_SECRET}
    TICK_CORS_ALLOWED_ORIGINS: $${TICK_CORS_ALLOWED_ORIGINS}
    TICK_AUTH_FRONTEND_CALLBACK_URL: $${TICK_FRONTEND_URL}/auth/callback
    TICK_AUTH_FRONTEND_LOGIN_URL: $${TICK_FRONTEND_URL}/login
    TICK_AUTH_COOKIE_SAME_SITE: None
    TICK_AUTH_COOKIE_SECURE: "true"
    TICK_AUTH_COOKIE_DOMAIN: $${TICK_AUTH_COOKIE_DOMAIN}
    SERVER_FORWARD_HEADERS_STRATEGY: $${SERVER_FORWARD_HEADERS_STRATEGY}
    NAVER_CLIENT_ID: $${NAVER_CLIENT_ID}
    NAVER_CLIENT_SECRET: $${NAVER_CLIENT_SECRET}
    TICK_AI_SERVER_URL: http://ai-server:8000

services:
  postgres:
    image: pgvector/pgvector:pg16
    restart: unless-stopped
    environment:
      POSTGRES_DB: tick
      POSTGRES_USER: tick
      POSTGRES_PASSWORD: $${POSTGRES_PASSWORD}
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

  # Python FastAPI RAG server. 외부 노출 X (Caddy 라우팅 없음).
  # backend-a/b 만 docker network 안에서 http://ai-server:8000 으로 호출.
  ai-server:
    image: ${ai_server_image}:latest
    container_name: tick-ai-server
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    mem_limit: 512m
    environment:
      POSTGRES_DSN: postgresql://tick:$${POSTGRES_PASSWORD}@postgres:5432/tick
      OPENAI_API_KEY: $${OPENAI_API_KEY}
      ANTHROPIC_API_KEY: $${ANTHROPIC_API_KEY}
      LOG_LEVEL: INFO

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
      - backend-a
      - backend-b

volumes:
  pgdata:
  caddy_data:
  caddy_config:
COMPOSE

# .env 템플릿: 도메인/CORS/쿠키 도메인은 terraform 변수로 박힘.
# 시크릿은 placeholder. 첫 부팅 후 SSM Session Manager 로 들어와서 수정.
if [ ! -f /opt/tick/.env ]; then
cat > /opt/tick/.env <<ENVFILE
POSTGRES_PASSWORD=change_me_strong_password
KAKAO_CLIENT_ID=change_me
KAKAO_CLIENT_SECRET=change_me
TICK_JWT_SECRET=change_me_64char_hex
TICK_BACKEND_URL=${backend_url}
TICK_FRONTEND_URL=${frontend_url}
TICK_CORS_ALLOWED_ORIGINS=${cors_origins}
TICK_AUTH_COOKIE_DOMAIN=${cookie_domain}
SERVER_FORWARD_HEADERS_STRATEGY=native
NAVER_CLIENT_ID=change_me
NAVER_CLIENT_SECRET=change_me
OPENAI_API_KEY=change_me
ANTHROPIC_API_KEY=change_me
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
