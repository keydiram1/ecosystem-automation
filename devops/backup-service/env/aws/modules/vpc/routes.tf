resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }
  tags = {
    Name = "${var.prefix}-public"
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this.id
  }

  tags = {
    Name = "${var.prefix}-private"
  }
}

resource "aws_route_table_association" "public_main" {
  subnet_id      = aws_subnet.public_main_subnet.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private_main" {
  subnet_id      = aws_subnet.private_main_subnet.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "public_eks" {
  count = length(var.eks_public_subnets)
  subnet_id      = aws_subnet.public_eks_subnet[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private_eks" {
  count = length(var.eks_private_subnets)
  subnet_id      = aws_subnet.private_eks_subnets[count.index].id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "public_backup_cluster" {
  count = length(var.aerospike_cluster_public_subnets)
  subnet_id      = aws_subnet.public_aerospike_subnet.id
  route_table_id = aws_route_table.public.id
}
