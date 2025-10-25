locals {
  module_name = "jenkins"
  members = toset(jsondecode(var.members))
  instances = element([for instance in data.google_compute_instance_group.group.instances: reverse(split("/", instance))[0]], 0)
  jenkins_server = "jenkins-server"
  jenkins_gcp_env_test_provisioner = "jenkins-gcp-env-provisioner"
  jenkins_artifact_builder = "jenkins-artifact-builder"
  jenkins_gcp_test_worker = "jenkins-gcp-test-worker"
  jenkins_local_test_worker = "jenkins-local-test-worker"
  jenkins_muli_node_local_test_worker = "jenkins-multi-node-local-tests-worker"
}
