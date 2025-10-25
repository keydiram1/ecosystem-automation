#!/usr/bin/env groovy
import jenkins.model.Jenkins

def job1 = Jenkins.instance.getItemByFullName('devops/rp-token-renewal')
job1.scheduleBuild2(0)

def job2 = Jenkins.instance.getItemByFullName('devops/asdb-load-data')
job2.scheduleBuild2(0)
