data "aws_secretsmanager_secret" "regcred" {
  name = "adr/jfrog/dev/creds"
}

data "aws_secretsmanager_secret_version" "regcred" {
  secret_id = data.aws_secretsmanager_secret.regcred.id
}