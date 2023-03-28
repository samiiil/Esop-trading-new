resource "aws_s3_bucket" "gurukul-samiksha" {
  bucket = "gurukul-samiksha"
}

resource "aws_s3_bucket_versioning" "enabled" {
  bucket = aws_s3_bucket.gurukul-samiksha.id
  versioning_configuration {
    status = "Enabled"
  }
}
resource "aws_s3_bucket_server_side_encryption_configuration" "default" {
  bucket = aws_s3_bucket.gurukul-samiksha.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
