
terraform {
  backend "s3" {
    bucket  = "gurukul-samiksha"
    key     = "terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
  }
}