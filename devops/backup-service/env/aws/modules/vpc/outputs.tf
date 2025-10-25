output "vpc_id" {
  value = aws_vpc.this.id
}

output "sg_id" {
  value = aws_security_group.this.id
}

output "public_main_subnet_ids" {
  value = aws_subnet.public_main_subnet[*].id
}

output "private_main_subnet_ids" {
  value = aws_subnet.private_main_subnet[*].id
}

output "public_eks_subnet_ids" {
  value = aws_subnet.public_eks_subnet[*].id
}

output "private_eks_subnet_ids" {
  value = aws_subnet.private_eks_subnets[*].id
}

output "public_aerospike_subnet_id" {
  value = aws_subnet.public_aerospike_subnet.id
}

output "private_hosted_zone_id" {
  value = aws_route53_zone.this.id
}

output "private_hosted_zone_name" {
  value = aws_route53_zone.this.name
}
