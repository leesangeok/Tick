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
:80 {
    encode gzip

    handle /api/* {
        reverse_proxy backend:8080
    }
    handle /oauth2/* {
        reverse_proxy backend:8080
    }
    handle /login/* {
        reverse_proxy backend:8080
    }
    handle /actuator/health {
        reverse_proxy backend:8080
    }
    handle {
        reverse_proxy frontend:3000
    }
}
CADDY

cat > /opt/tick/compose.prod.yaml <<COMPOSE
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

  backend:
    image: ${backend_image}:latest
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
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
      - backend

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

chown -R ec2-user:ec2-user /opt/tick

# 첫 부팅 시점엔 이미지가 ECR 에 없을 수 있어서 compose up 은 안 함.
# 첫 배포는 CD 워크플로우 또는 사용자가 SSM 으로 들어와서 수동 실행.
