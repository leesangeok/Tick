# =============================================================================
# RDS Postgres (pgvector) — 코드만 준비. apply 는 별도.
# =============================================================================
# 현재 EC2 docker postgres 를 RDS 로 이관하기 위한 정의. plan 만 확인하고 실제 apply 시
# 다음 순서 필요:
#   1. RDS 인스턴스 생성 (`terraform apply -target=aws_db_instance.main` 등)
#   2. pg_dump 로 EC2 postgres 스냅샷 → RDS 로 restore (`psql -f dump.sql`)
#   3. CREATE EXTENSION vector; (pgvector — RDS Postgres 16.x 지원)
#   4. backend/ai-server env (POSTGRES_DSN 또는 SPRING_DATASOURCE_URL) 를 RDS endpoint 로 교체
#   5. 배포 → 정상 확인 후 EC2 postgres 컨테이너 정리 + backup.tf lifecycle 유지
# 위 순서 없이 apply 만 하면 backend 가 여전히 EC2 postgres 를 바라봐서 이관 무의미.
#
# 비용: db.t4g.micro (~$12/월) + gp3 20GB (~$2/월). backup 유지비는 backup.tf 와 별개.
# 프로덕션이 아닌 dev 용이라 Multi-AZ off, publicly_accessible=false, deletion_protection=false.
# =============================================================================

# RDS 는 subnet group 에 최소 2 AZ 필요. 기존 public 서브넷 (10.0.1.0/24) 옆에 두 개 추가.
# RDS 는 private 이므로 이 서브넷들은 IGW 라우트 없이 SG 로만 EC2 → RDS 통신 허용.
resource "aws_subnet" "db_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.10.0/24"
  availability_zone = "${var.region}a"

  tags = { Name = "${local.name_prefix}-db-a" }
}

resource "aws_subnet" "db_c" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.11.0/24"
  availability_zone = "${var.region}c"

  tags = { Name = "${local.name_prefix}-db-c" }
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db"
  subnet_ids = [aws_subnet.db_a.id, aws_subnet.db_c.id]

  tags = { Name = "${local.name_prefix}-db" }
}

resource "aws_security_group" "db" {
  name        = "${local.name_prefix}-db"
  description = "RDS Postgres — EC2 app 만 5432 접속"
  vpc_id      = aws_vpc.main.id

  # RDS 는 EC2 app SG 로부터만 5432 접속 허용. Korean 문자열은 AWS Description 규격 밖.
  ingress {
    description     = "Postgres from app EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    description = "all outbound (RDS itself does not initiate outbound)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${local.name_prefix}-db" }
}

# 비밀번호는 Secrets Manager. Terraform 최초 apply 시 자동 생성, 이후 rotation 가능.
resource "aws_secretsmanager_secret" "db_password" {
  name        = "${local.name_prefix}/db/password"
  description = "RDS postgres tick user password"
}

resource "random_password" "db" {
  length  = 32
  special = false # RDS 는 일부 특수문자 URL-encode 필요 — 단순화 위해 배제.
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db.result
}

resource "aws_db_instance" "main" {
  identifier                   = "${local.name_prefix}-main"
  engine                       = "postgres"
  engine_version               = "16.4"
  instance_class               = "db.t4g.micro"
  allocated_storage            = 20
  storage_type                 = "gp3"
  storage_encrypted            = true
  db_name                      = "tick"
  username                     = "tick"
  password                     = random_password.db.result
  db_subnet_group_name         = aws_db_subnet_group.main.name
  vpc_security_group_ids       = [aws_security_group.db.id]
  publicly_accessible          = false
  backup_retention_period      = 7
  backup_window                = "18:00-19:00" # 03:00-04:00 KST
  maintenance_window           = "sat:19:00-sat:20:00"
  auto_minor_version_upgrade   = true
  deletion_protection          = false # dev
  skip_final_snapshot          = true  # dev
  performance_insights_enabled = false

  # pgvector 는 Postgres 16 표준 확장. shared_preload_libraries 는 불필요, 그냥 CREATE EXTENSION 필요.
  # 관측: CREATE EXTENSION vector; 를 restore SQL 상단에 넣거나 Flyway V1 이전에 수동 실행.

  tags = { Name = "${local.name_prefix}-db" }
}

# EC2 가 db password secret 을 읽어서 backend/ai-server 컨테이너에 env 로 주입.
resource "aws_iam_role_policy" "ec2_db_secret" {
  name = "${local.name_prefix}-ec2-db-secret"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = aws_secretsmanager_secret.db_password.arn
    }]
  })
}

output "db_endpoint" {
  value       = aws_db_instance.main.address
  description = "RDS endpoint — backend SPRING_DATASOURCE_URL 로 사용."
}

output "db_password_secret_arn" {
  value       = aws_secretsmanager_secret.db_password.arn
  description = "RDS password 를 담은 Secrets Manager ARN."
}
