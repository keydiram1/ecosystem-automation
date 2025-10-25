resource "aws_route53_zone" "this" {
  name = local.dns_name

  vpc {
    vpc_id = aws_vpc.this.id
  }

  tags = {
    Name = local.dns_name
  }
}
