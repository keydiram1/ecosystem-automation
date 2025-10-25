# Update Jenkins plugins

```
docker run -p 8080:8080 -p 50000:50000 --restart=on-failure jenkins/jenkins:lts-jdk17
```
* Copy access token from the terminal
* Navigate to http://127.0.0.1:8080
* Click on Install Suggested plugins 
* Click on skip and continue as an admin
* Navigate to http://127.0.0.1:8080/script
* Copy paste the following snippet
```
Jenkins.instance.pluginManager.plugins.each{
  plugin -> 
    println ("${plugin.getDisplayName()} (${plugin.getShortName()}): ${plugin.getVersion()}")
}
```
* Copy paste the result to the `plugins.txt` file

# Run Jenkins Locally
aerospike.jfrog.io/ecosystem-container-dev-local/abs-jenkins-server
