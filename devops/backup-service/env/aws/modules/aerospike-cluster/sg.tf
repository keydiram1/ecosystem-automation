#resource "aws_security_group" "this" {
#  name   = "${local.cluster_name}-sg"
#  vpc_id = data.aws_vpc.this.id
#
##  ingress {
##    protocol    = "tcp"
##    self        = true
##    from_port   = 22
##    to_port     = 22
##    cidr_blocks = ["0.0.0.0/0"]
##  }
##  ingress {
##    protocol    = "tcp"
##    self        = true
##    from_port   = 3000
##    to_port     = 3000
##    cidr_blocks = ["0.0.0.0/0"]
##  }
##  ingress {
##    protocol    = "tcp"
##    self        = true
##    from_port   = 3000
##    to_port     = 3004
##    cidr_blocks = [data.aws_vpc.this.cidr_block]
##  }
##  ingress {
##    from_port        = 80
##    to_port          = 80
##    protocol         = "tcp"
##    cidr_blocks      = ["0.0.0.0/0"]
##  }
##  egress {
##    from_port   = 0
##    to_port     = 0
##    protocol    = "-1"
##    cidr_blocks = ["0.0.0.0/0"]
##  }
#  ingress {
#    protocol    = -1
#    self        = true
#    from_port   = 0
#    to_port     = 0
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#  egress {
#    from_port   = 0
#    to_port     = 0
#    protocol    = "-1"
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#  tags = {
#    Name = "${local.cluster_name}-sg"
#  }
#}
