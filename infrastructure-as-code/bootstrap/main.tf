# Terraform state 백엔드. envs/* 보다 먼저 1회만 apply.
#
# 사용:
#   cd bootstrap
#   terraform init
#   terraform apply
#
# 이후 envs/dev/backend.tf 가 여기서 만든 S3/DynamoDB 를 사용.

terraform {
  required_version = ">= 1.10"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
  }
}

provider "aws" {
  region = var.region
}

variable "region" {
  type    = string
  default = "ap-northeast-2"
}

variable "account_id" {
  type        = string
  description = "AWS account ID — S3 state bucket 이름 (글로벌 unique) 에 prefix 로 사용. terraform.tfvars 또는 -var 로 주입."
}

variable "project" {
  type    = string
  default = "tick"
}

variable "lock_table" {
  type    = string
  default = "tick-tflock"
}

locals {
  state_bucket = "${var.project}-tfstate-${var.account_id}"
}

resource "aws_s3_bucket" "state" {
  bucket = local.state_bucket

  tags = {
    Project = "tick"
    Purpose = "terraform-state"
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "lock" {
  name         = var.lock_table
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Project = "tick"
    Purpose = "terraform-lock"
  }
}

output "state_bucket" {
  value = aws_s3_bucket.state.bucket
}

output "lock_table" {
  value = aws_dynamodb_table.lock.name
}
