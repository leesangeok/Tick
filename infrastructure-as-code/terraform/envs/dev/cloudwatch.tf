# =============================================================================
# CloudWatch — 앱 로그/메트릭 중앙집중. 코드만 준비 (docker log driver 변경 필요).
# =============================================================================
# 활성 순서:
#   1. `terraform apply` — 아래 log group + IAM 생성
#   2. docker-compose 에서 각 서비스에 `logging.driver=awslogs` 설정
#      logging:
#        driver: awslogs
#        options:
#          awslogs-region: ap-northeast-2
#          awslogs-group: /tick/dev/app
#          awslogs-stream: backend / frontend / ai-server / caddy
#   3. 재배포 후 CloudWatch → Log groups 에서 확인
# =============================================================================

resource "aws_cloudwatch_log_group" "app" {
  name              = "/${var.project}/${var.environment}/app"
  retention_in_days = 30
  tags              = { Name = "${local.name_prefix}-logs" }
}

# EC2 가 CloudWatch Logs 로 push 할 수 있게 권한. awslogs driver 는 이 권한 사용.
resource "aws_iam_role_policy" "ec2_cloudwatch_logs" {
  name = "${local.name_prefix}-ec2-cwlogs"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogStreams",
        "logs:CreateLogGroup",
      ]
      Resource = "${aws_cloudwatch_log_group.app.arn}:*"
    }]
  })
}

# 알람 예시 — 에러 로그가 5분에 10건 넘으면 SNS 알림. SNS topic 은 별도 정의 필요.
# 현재는 metric filter 만 정의 (알람은 사용자 필요 시 추가).
resource "aws_cloudwatch_log_metric_filter" "backend_errors" {
  name           = "${local.name_prefix}-backend-errors"
  log_group_name = aws_cloudwatch_log_group.app.name
  pattern        = "\"ERROR\""

  metric_transformation {
    name      = "BackendErrorCount"
    namespace = "Tick/${var.environment}"
    value     = "1"
    unit      = "Count"
  }
}

output "cloudwatch_log_group" {
  value       = aws_cloudwatch_log_group.app.name
  description = "docker-compose logging.options.awslogs-group 에 넣을 값."
}
