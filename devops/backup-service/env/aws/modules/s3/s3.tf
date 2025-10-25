resource "aws_s3_bucket" "this" {
  count = var.buckets
  bucket        = "${var.prefix}-${var.bucket_name}-${count.index}"
  force_destroy = true

  tags = {
    Name = "${var.prefix}-${var.bucket_name}-${count.index}"
  }
}

resource "aws_s3_bucket_ownership_controls" "this" {
  count = var.buckets
  bucket = aws_s3_bucket.this[count.index].id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }

}

resource "aws_s3_bucket_acl" "this" {
  count = var.buckets
  bucket = aws_s3_bucket.this[count.index].id
  acl    = "private"

  depends_on = [aws_s3_bucket_ownership_controls.this]
}

resource "aws_s3_access_point" "this" {
  count = var.access_point == true ? 1 : 0
  bucket = aws_s3_bucket.this.0.id
  name   = "${var.prefix}-${var.bucket_name}-ap"

  lifecycle {
    ignore_changes = [bucket]
  }

  depends_on = [
    aws_s3_bucket_ownership_controls.this,
    aws_s3_bucket_acl.this
  ]
}
