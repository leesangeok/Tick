# Postgres 일일 pg_dump 백업 → S3.
# - 매일 03:30 KST (= 18:30 UTC) 에 EC2 cron 실행.
# - 30일 후 STANDARD_IA 로 전환, 90일 후 만료.
# - EC2 instance profile 에 BACKUP_BUCKET PutObject 권한 추가.

resource "aws_s3_bucket" "backup" {
  bucket = "${local.name_prefix}-pgbackup-${data.aws_caller_identity.current.account_id}"

  tags = { Name = "${local.name_prefix}-pgbackup" }
}

resource "aws_s3_bucket_versioning" "backup" {
  bucket = aws_s3_bucket.backup.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backup" {
  bucket = aws_s3_bucket.backup.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "backup" {
  bucket                  = aws_s3_bucket.backup.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "backup" {
  bucket = aws_s3_bucket.backup.id

  rule {
    id     = "postgres-backup-lifecycle"
    status = "Enabled"

    filter {
      prefix = "postgres/"
    }

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    expiration {
      days = 90
    }

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

# EC2 가 backup bucket 에 PutObject / ListBucket
resource "aws_iam_role_policy" "ec2_backup" {
  name = "${local.name_prefix}-ec2-backup"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:PutObjectAcl"]
        Resource = "${aws_s3_bucket.backup.arn}/postgres/*"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = aws_s3_bucket.backup.arn
      }
    ]
  })
}

output "backup_bucket" {
  value       = aws_s3_bucket.backup.bucket
  description = "Postgres pg_dump 백업 S3 bucket"
}
