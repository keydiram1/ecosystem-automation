packer {
    required_plugins {
        amazon = {
            version = ">= 1.0.0"
            source = "github.com/hashicorp/amazon"
        }
    }
}

variable "ami_name" {
    type = string
}

variable "project" {
    type = string
}

variable "subnet_id" {
    type = string
}

data "amazon-ami" "ubuntu-jammy-2204-amd64" {
    filters = {
        name                = "ubuntu/images/*ubuntu-jammy-22.04-amd64-server-*"
        root-device-type    = "ebs"
        virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["099720109477"]
}

source "amazon-ebs" "builder" {
    ami_name      = var.ami_name
    force_deregister = true
    force_delete_snapshot = true
#    skip_create_ami = true
    source_ami    = data.amazon-ami.ubuntu-jammy-2204-amd64.id
    instance_type = "t2.micro"
    subnet_id     = var.subnet_id
    ssh_username  = "ubuntu"
    tags = {
        Name = var.ami_name
        Project = var.project
    }
    snapshot_tags = {
        Name = var.ami_name
        Project = var.project
    }
}

build {
    sources = ["source.amazon-ebs.builder"]

    provisioner "shell" {
        script = "${path.root}/install-worker.sh"
        execute_command = "sudo -S bash -c '{{ .Path }}'"
    }
}
