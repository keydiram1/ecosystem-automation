resource "aws_s3_object" "upload_remote_config" {
  bucket       = var.s3.name
  key          = local.config_path
  content      = yamlencode(local.service_config)
  content_type = "application/x-yaml"
}
