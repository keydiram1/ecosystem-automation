packer {
  required_plugins {
    googlecompute = {
      version = ">= 1.1.1"
      source = "github.com/hashicorp/googlecompute"
    }
    ansible = {
      version = "~> 1"
      source = "github.com/hashicorp/ansible"
    }
  }
}

variable "project_id" {
  type = string
}

variable "zone" {
  type = string
}

variable "image_name" {
  type = string
}

variable "machine_type" {
  type = string
}

variable "source_image_family" {
  type = string
}



# variable "builder_sa" {
#     type = string
# }

source "googlecompute" "image" {

  project_id                  = var.project_id
  zone                        = var.zone
  source_image_family         = var.source_image_family
  instance_name = var.image_name
  machine_type = var.machine_type
  image_name = var.image_name
#   skip_create_image = true
  image_description = "Aerospike Image Builder"
  disk_name = "${var.image_name}-disk"
  disk_size = 20
  disk_type = "pd-standard"
  ssh_username                = "ubuntu"

  #     use_os_login = true
}

build {
  sources = ["sources.googlecompute.image"]
  provisioner "ansible" {
    playbook_file = "${path.root}/setup.yaml"
    use_proxy = false
  }
}
