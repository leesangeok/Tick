variable "region" {
  type        = string
  description = "AWS region"
  default     = "ap-northeast-2"
}

variable "project" {
  type        = string
  description = "Project name (prefix for resource names)"
  default     = "tick"
}

variable "environment" {
  type        = string
  description = "Environment name"
  default     = "dev"
}
