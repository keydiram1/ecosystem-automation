# Prerequisite
* [Install Task](https://taskfile.dev/installation)
* [Install Ansible](https://docs.ansible.com/ansible/latest/installation_guide/index.html)
* Install AWS plugin: `cd ansible && ansible-galaxy collection install -r requirements.yml`


# Create builder 
```shell
task create-builder
```

# Build Base Image
```shell
DOCKER_USERNAME="<username>" DOCKER_PASSWORD="<token>" LATEST="<true/false>" TAG="<tag>" task build-base
```

# Build Image
```shell
DOCKER_USERNAME="<username>" DOCKER_PASSWORD="<token>" LATEST="<true/false>" TAG="<tag>" BRANCH="<branch>" task build-image
```

# Destroy builder
```shell
task destroy-builder  
```