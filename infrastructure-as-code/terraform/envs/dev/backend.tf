terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Partial backend config — bucket / dynamodb_table 은 git 에 박지 않고
  # `terraform init -backend-config=backend-config.hcl` 로 주입.
  # backend-config.hcl 작성법은 backend-config.hcl.example 참고.
  backend "s3" {
    key     = "envs/dev/terraform.tfstate"
    region  = "ap-northeast-2"
    encrypt = true
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "tick"
      Environment = "dev"
      ManagedBy   = "terraform"
    }
  }
}
