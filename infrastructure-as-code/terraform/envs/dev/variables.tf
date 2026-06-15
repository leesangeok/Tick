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

variable "root_domain" {
  type        = string
  description = "Root domain. 파생: api.<root_domain> = backend, <root_domain> = frontend, .<root_domain> = cookie domain"
  default     = "tickk.dev"
}
