# Tick Infrastructure

AWS 배포 / IaC. **리전: `ap-northeast-2` (Seoul). 단일 EC2 + Caddy + docker-compose 구성.**

## 아키텍처

```
Internet
   │
   ▼
┌──────────────────────────────────────┐
│  EC2 t4g.small  (public subnet)      │
│  ┌────────────────────────────────┐  │
│  │  Caddy :80 / :443 (TLS 자동)   │  │
│  │   ├ /api/*       → backend     │  │
│  │   ├ /oauth2/*    → backend     │  │
│  │   ├ /login/*     → backend     │  │
│  │   └ /*           → frontend    │  │
│  │                                │  │
│  │  backend  (Spring Boot, :8080) │  │
│  │  frontend (Next.js, :3000)     │  │
│  │  postgres (16-alpine, :5432)   │  │
│  └────────────────────────────────┘  │
│  Elastic IP attached                  │
└──────────────────────────────────────┘
        │
        │ pull on deploy (SSM Run Command)
        ▼
   ECR (backend / frontend 이미지)

   SSM Session Manager → EC2 (SSH 키 없음)
   IAM OIDC → GitHub Actions (장기 access key 없음)
```

## 비용 (월, USD 대략)

| 항목 | 비용 |
|---|---|
| EC2 t4g.small (24/7) | ~$12 |
| EBS gp3 30GB | ~$2.4 |
| Elastic IP (attached) | $0 |
| ECR (<500MB 무료) | $0 |
| Data transfer out (<100GB) | $0 |
| SSM | $0 |
| **합계** | **~$15** |

도메인 붙이면:
- Route 53 hosted zone: +$0.50
- TLS cert (Caddy auto via Let's Encrypt): $0
- 도메인 자체 (.com 등): 별도 (~$10/년)

띄워둔 시간만큼 청구되니까 **면접 직전에 띄우고 끝나면 `terraform destroy`** 전략이면 한 번에 $1 미만.

## 디렉터리

```
infrastructure-as-code/
├── README.md
├── compose.full.yaml           로컬 풀스택 (postgres + backend + frontend, Caddy 없음)
├── bootstrap/                  Terraform state 백엔드 (S3 + DynamoDB) — 1회만 apply
│   └── main.tf
└── terraform/envs/dev/         실제 dev 환경 리소스
    ├── backend.tf              S3 backend 설정
    ├── variables.tf
    ├── vpc.tf                  VPC + IGW + public subnet
    ├── ec2.tf                  AL2023 ARM64, t4g.small, EIP, IMDSv2 강제
    ├── user_data.sh            Docker 설치 + /opt/tick 부트스트랩
    ├── ecr.tf                  backend/frontend repo + 20개 보존
    └── iam_github_oidc.tf      GitHub Actions OIDC role (ECR push + SSM SendCommand)
```

## 배포 순서

### 1회 셋업

```bash
# 1) Terraform state 백엔드 (S3 + DynamoDB)
cd bootstrap
terraform init && terraform apply

# 2) dev 환경 리소스
cd ../terraform/envs/dev
terraform init && terraform apply
# 출력:
#   app_public_ip          = "x.x.x.x"
#   app_instance_id        = "i-xxxxxxxxx"
#   github_actions_role_arn = "arn:aws:iam::...:role/tick-dev-github-actions-ci"
#   ssm_session_command    = "aws ssm start-session --target i-xxx ..."
#   ecr_backend_url        = "...dkr.ecr.../tick-dev-backend"
#   ecr_frontend_url       = "...dkr.ecr.../tick-dev-frontend"
```

### GitHub Secrets 등록

| Secret name | 값 |
|---|---|
| `AWS_OIDC_ROLE_ARN` | `github_actions_role_arn` 출력값 |
| `EC2_INSTANCE_ID` | `app_instance_id` 출력값 |

### EC2 시크릿 채우기 (한 번만)

EC2 부팅 시 `/opt/tick/.env` 가 placeholder 로 만들어짐. SSM 으로 들어가서 실제 값으로 교체.

```bash
# 로컬에서
aws ssm start-session --target <instance-id> --region ap-northeast-2

# EC2 안에서
sudo vi /opt/tick/.env
# POSTGRES_PASSWORD, KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET,
# TICK_JWT_SECRET, TICK_PUBLIC_URL 채움
# 예: TICK_PUBLIC_URL=http://<eip>  (도메인 없을 때)
#     TICK_PUBLIC_URL=https://tick.example.com  (도메인 있을 때)
```

### 카카오 콘솔 설정

EC2 EIP / 도메인 기준으로 **Redirect URI** 추가:
- `http://<eip>/login/oauth2/code/kakao`
- 또는 `https://<domain>/login/oauth2/code/kakao`

### 첫 배포

`main` 브랜치에 푸시 → CD 워크플로우가:
1. 변경된 쪽 (backend/frontend) Docker 이미지 빌드 + ECR push
2. SSM Run Command 로 EC2 에 `docker compose pull && up -d` 실행

수동 배포 (테스트용):
```bash
gh workflow run CD
```

### 도메인 + HTTPS 붙이기 (선택)

1. Route 53 hosted zone 생성 (또는 외부 DNS 에 A 레코드 추가) → A 레코드 → EIP
2. `/opt/tick/caddy/Caddyfile` 수정: `:80` → 도메인명 (예: `tick.example.com`)
3. `/opt/tick/.env` 의 `TICK_PUBLIC_URL` 도 `https://tick.example.com` 으로
4. `docker compose -f /opt/tick/compose.prod.yaml --env-file /opt/tick/.env up -d`
5. Caddy 가 자동으로 Let's Encrypt cert 발급 + HTTPS 강제

## 운영 메모

- **데이터**: Postgres 데이터는 EC2 EBS volume 의 docker volume (`pgdata`) 에 저장.
- **백업**: 일정 시점에 `pg_dump` → S3 업로드 cron 추가 가능 (지금은 없음).
- **로그**: SSM 으로 들어가서 `cd /opt/tick && docker compose logs -f` 확인. CloudWatch 통합은 나중.
- **재시작**: 인스턴스 재부팅 시 `restart: unless-stopped` 로 docker-compose 가 알아서 재시작.
- **확장**: 수직 확장 (인스턴스 타입 키움) 만 가능. 사용자 증가 시 RDS 분리 + ALB 도입 검토.
- **무중단 배포 한계**: 이 구성은 docker compose up -d 시 컨테이너 교체에 몇 초 다운타임 있음. 진짜 무중단은 ECS + ALB 로 가야 함.

## 정리 (destroy)

면접 끝나면 비용 0:
```bash
cd terraform/envs/dev
terraform destroy
# (bootstrap S3/DynamoDB 는 거의 무료라 그냥 두는 게 편함)
```
