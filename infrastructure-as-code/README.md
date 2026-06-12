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

AWS 계정 ID 는 git tracked 파일에 박지 않고 로컬 tfvars / backend-config 로 주입한다.

```bash
# 0) AWS 계정 ID 확인
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# 1) Terraform state 백엔드 (S3 + DynamoDB)
cd bootstrap
cp terraform.tfvars.example terraform.tfvars
sed -i '' "s|<AWS_ACCOUNT_ID>|$ACCOUNT_ID|" terraform.tfvars  # 또는 직접 편집
terraform init
terraform apply
# 출력:
#   state_bucket = "tick-tfstate-<ACCOUNT_ID>"
#   lock_table   = "tick-tflock"

# 2) dev 환경 리소스 — partial backend config 로 init
cd ../terraform/envs/dev
cp backend-config.hcl.example backend-config.hcl
sed -i '' "s|<AWS_ACCOUNT_ID>|$ACCOUNT_ID|" backend-config.hcl
terraform init -backend-config=backend-config.hcl
terraform apply
# 출력:
#   app_public_ip          = "x.x.x.x"
#   app_instance_id        = "i-xxxxxxxxx"
#   github_actions_role_arn = "arn:aws:iam::...:role/tick-dev-github-actions-ci"
#   ssm_session_command    = "aws ssm start-session --target i-xxx ..."
#   ecr_backend_url        = "...dkr.ecr.../tick-dev-backend"
#   ecr_frontend_url       = "...dkr.ecr.../tick-dev-frontend"
```

`terraform.tfvars` 와 `backend-config.hcl` 은 `.gitignore` 로 막혀 있어 실수로 커밋되지 않음.

### 이미 init 된 환경에서 partial config 로 전환

기존에 `bucket = "..."` 가 backend.tf 에 박혀 있던 상태로 init 된 환경이라면 한 번만 reconfigure:

```bash
cd terraform/envs/dev
cp backend-config.hcl.example backend-config.hcl
# 값 채움
terraform init -reconfigure -backend-config=backend-config.hcl
# state 위치는 동일 (같은 S3 bucket / key) 이라 마이그레이션 없음
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
- **백업**: 매일 03:30 KST (18:30 UTC) 에 EC2 cron 이 `pg_dump | gzip` 후 S3 (`tick-dev-pgbackup-<account-id>`) 의 `postgres/tick-<ts>.sql.gz` 로 업로드.
  - 30일 후 S3 STANDARD_IA 로 자동 전환, 90일 후 만료. versioning 활성.
  - 로그: `/var/log/tick/pg-backup.log` (SSM 으로 들어가서 `tail -f`)
  - 수동 실행: `sudo /opt/tick/scripts/pg-backup.sh`
  - 복원: 로컬에서 `aws s3 cp s3://tick-dev-pgbackup-.../postgres/tick-<ts>.sql.gz - | gunzip | psql -h ... -U tick -d tick`
- **로그**: SSM 으로 들어가서 `cd /opt/tick && docker compose logs -f` 확인. CloudWatch 통합은 나중.
- **재시작**: 인스턴스 재부팅 시 `restart: unless-stopped` 로 docker-compose 가 알아서 재시작.
- **확장**: 수직 확장 (인스턴스 타입 키움) 만 가능. 사용자 증가 시 RDS 분리 + ALB 도입 검토.
- **무중단 배포**: backend 를 `backend-a` / `backend-b` 두 컨테이너로 띄우고 Caddy 가 round-robin + active health check (`/actuator/health` 5초 주기) + passive failure (1회 실패 시 10초 격리) 로 페일오버. CD 가 한 번에 한 쪽씩 `--force-recreate` + healthy 대기 → 다른 쪽이 트래픽 받음. 단일 EC2 + docker compose 환경에서 가능한 최선. 더 견고한 진짜 무중단 (replica 자동 배치, AZ 분산) 은 ECS + ALB.
  - **주의 1**: Flyway 스키마 변경은 **backward-compatible 한 변경만** 안전. NOT NULL 컬럼 추가, 컬럼 rename/drop 같은 깨는 변경은 backend-a 가 새 스키마 + backend-b 가 옛 코드로 동시에 동작해서 한쪽이 깨질 수 있음.
  - **주의 2**: t4g.small 메모리 2 GiB 에서 backend 두 개 (각 `mem_limit: 768m`) + postgres + caddy 가 빠듯하게 들어감. OOM 보이면 `mem_limit` 낮추거나 인스턴스 타입 키움.

## 무중단 구조 적용 (기존 EC2 가 있을 때)

terraform 의 `user_data_replace_on_change = false` 라서 `user_data.sh` 만 수정해도 기존 인스턴스는 안 갈림. 둘 중 하나:

### A. SSM 으로 들어가서 수동 적용 (다운타임 ~수 초)

> ⚠️ **bind mount inode 함정 — 반드시 읽기**
>
> docker bind mount (`./caddy/Caddyfile:/etc/caddy/Caddyfile:ro` 같은 host 경로 마운트) 는 컨테이너 부팅 시 호스트 파일의 **inode** 를 잡는다. **inode 가 바뀌는 패턴** 으로 파일을 갱신하면 컨테이너 안의 마운트는 옛 inode 를 계속 가리켜서 새 내용을 못 본다 — Caddy 는 `config is unchanged` 로 응답하고 reload 가 no-op 가 된다.
>
> | 함정 (inode 변경) | 안전 (inode 보존) |
> |---|---|
> | `mv new old` | `cat new > old` |
> | `vim :w` (`backupcopy=no` 기본) | `tee old < new` |
> | `cp -f new old` 의 일부 환경 | `install -m 644 new old` (POSIX) |
> | 편집기의 atomic write (`tempfile + rename`) | 편집기에서 `set backupcopy=yes` 후 저장 |
>
> 함정에 빠진 뒤엔 `docker compose restart caddy` 로 mount 가 새 inode 를 잡게 강제할 수 있다.

```bash
aws ssm start-session --target <instance-id> --region ap-northeast-2

# EC2 안에서 — vi/mv 금지. cat heredoc 으로 in-place 덮어쓰기.
sudo tee /opt/tick/caddy/Caddyfile > /dev/null <<'EOF'
... 새 Caddyfile 내용 (user_data.sh 의 Caddyfile heredoc 참고) ...
EOF

sudo tee /opt/tick/compose.prod.yaml > /dev/null <<'EOF'
... 새 compose 내용 (${backend_image} / ${frontend_image} 는 ECR repo URL 로 치환) ...
EOF

# caddy 의 admin reload 는 'config is unchanged' 라고 거짓 보고할 수 있다 (mount 가 옛 inode 잡고 있을 때).
# 안전하게 caddy 재시작으로 mount 갱신을 강제한다.
docker compose -f /opt/tick/compose.prod.yaml --env-file /opt/tick/.env restart caddy

# 새 backend-a / backend-b 띄우고 옛 단일 backend 제거
docker compose -f /opt/tick/compose.prod.yaml --env-file /opt/tick/.env up -d --no-deps backend-a backend-b
# (healthy 대기 후)
docker container rm -f tick-backend-1 2>/dev/null || true
```

이후 `main` 브랜치에 push 하면 CD 가 rolling 으로 무중단 배포.

### B. 인스턴스 재생성 (한 번 다운타임 ~2분, 그 후 영구 무중단)

```bash
cd infrastructure-as-code/terraform/envs/dev
terraform taint aws_instance.app
terraform apply
# 새 인스턴스가 새 user_data.sh 로 부팅 → EIP 그대로 attach
# 새 EIP/IP 가 카카오 콘솔에 등록된 redirect URI 와 동일한지 확인
# .env 시크릿 한 번 다시 채워야 함 (SSM 으로)
```

## 정리 (destroy)

면접 끝나면 비용 0:
```bash
cd terraform/envs/dev
terraform destroy
# (bootstrap S3/DynamoDB 는 거의 무료라 그냥 두는 게 편함)
```
