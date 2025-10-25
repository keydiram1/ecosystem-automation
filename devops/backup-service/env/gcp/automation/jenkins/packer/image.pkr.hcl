packer {
  required_plugins {
    googlecompute = {
      version = ">= 1.1.1"
      source  = "github.com/hashicorp/googlecompute"
    }
    ansible = {
      version = "~> 1"
      source  = "github.com/hashicorp/ansible"
    }
  }
}

variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "zone" {
  type = string
}

variable "image_name" {
  type = string
  default = "jenkins-multi-node-local-tests-worker"
}

variable "machine_type" {
  type = string
}

variable "source_image_family" {
  type = string
}

variable "github_token" {
  type = string
}

variable "local_storage" {
  type = bool
}

variable "disk_size" {
  type = number
}


# variable "startup_script_file" {
#   type = string
# }

source "googlecompute" "image" {

  project_id          = var.project_id
  zone                = var.zone
  source_image_family = var.source_image_family
  instance_name       = "packer-${var.image_name}-builder"
  machine_type        = var.machine_type
  image_name          = var.image_name
  image_description   = "EcoEng ${var.image_name} packer image"
  image_labels = {
    "resource" : "jenkins"
  }
  image_storage_locations = [var.region]
  disk_name           = "${var.image_name}-disk"
  disk_size           = var.disk_size
  disk_type           = "pd-standard"
  ssh_username        = "ubuntu"
  # skip_create_image = true
  preemptible = true
  use_os_login = true

  dynamic "disk_attachment" {
    for_each = var.local_storage ? [1] : []
    content {
      volume_type = "scratch"
      volume_size = 375
      interface_type  = "NVME"
    }
  }


  # startup_script_file = var.startup_script_file
}

build {
  sources = ["sources.googlecompute.image"]
  provisioner "ansible" {
    playbook_file = "${path.root}/playbooks/${var.image_name}.yaml"
    use_proxy     = false
    extra_arguments = ["--extra-vars", "github_token=${var.github_token}"]
  }
}
