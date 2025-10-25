resource "aws_subnet" "public_main_subnet" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.0.0.0/24"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-public-main"
  }
}

resource "aws_subnet" "private_main_subnet" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = false

  tags = {
    Name = "${var.prefix}-private-main"
  }
}

resource "aws_subnet" "public_eks_subnet" {
  count = length(var.eks_public_subnets)
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.eks_public_subnets[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index % 2]
  map_public_ip_on_launch = true

  tags = merge(
    { Name = "${var.prefix}-public-eks-${ data.aws_availability_zones.available.names[count.index % 2]}" },
    var.eks_public_subnet_tags
  )
}

resource "aws_subnet" "private_eks_subnets" {
  count = length(var.eks_private_subnets)
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.eks_private_subnets[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index % 2]
  map_public_ip_on_launch = false

  tags = merge(
    { Name = "${var.prefix}-private-eks-${data.aws_availability_zones.available.names[count.index % 2]}" },
    var.eks_private_subnet_tags
  )
}

resource "aws_subnet" "public_aerospike_subnet" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.aerospike_cluster_public_subnets.0
  availability_zone       = data.aws_availability_zones.available.names.0
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-public-backup-${data.aws_availability_zones.available.names.0}"
  }
}
