# =============================================================================
# 뉴스 원본 HTML 아카이빙 S3 bucket. backend `tick.news.archive.enabled=true` 시 사용.
# =============================================================================

resource "aws_s3_bucket" "news_archive" {
  bucket = "${local.name_prefix}-news-archive-${data.aws_caller_identity.current.account_id}"
  tags   = { Name = "${local.name_prefix}-news-archive" }
}

resource "aws_s3_bucket_versioning" "news_archive" {
  bucket = aws_s3_bucket.news_archive.id
  versioning_configuration {
    status = "Suspended" # 뉴스 원본은 immutable — versioning 비용 절약.
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "news_archive" {
  bucket = aws_s3_bucket.news_archive.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "news_archive" {
  bucket                  = aws_s3_bucket.news_archive.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 오래된 뉴스는 STANDARD_IA 로 이관 후 1년 뒤 만료. 감사/원문 재확인 목적이라 장기 보관 불필요.
resource "aws_s3_bucket_lifecycle_configuration" "news_archive" {
  bucket = aws_s3_bucket.news_archive.id

  rule {
    id     = "news-archive-lifecycle"
    status = "Enabled"
    filter { prefix = "news/" }

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    expiration {
      days = 365
    }
  }
}

# EC2 backend 컨테이너가 뉴스 body 를 이 bucket 에 PutObject.
resource "aws_iam_role_policy" "ec2_news_archive" {
  name = "${local.name_prefix}-ec2-news-archive"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:PutObjectAcl"]
      Resource = "${aws_s3_bucket.news_archive.arn}/news/*"
    }]
  })
}

output "news_archive_bucket" {
  value       = aws_s3_bucket.news_archive.bucket
  description = "backend TICK_NEWS_ARCHIVE_S3_BUCKET 환경변수로 주입."
}
