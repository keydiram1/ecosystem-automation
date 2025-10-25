output "bucket" {
  value = var.access_point == true ? aws_s3_access_point.this.0.arn : aws_s3_bucket.this.0.bucket
}
