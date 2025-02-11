#!/usr/bin/env groovy
// awsElb.groovy contains methods that wrap awscli elb subcommands

// Returns the Elastic Load Balancer's DNSName for the given environment
// See ops/terraform/services/server/modules/bfd_server_asg/main.tf for NLB definition and naming scheme
String getElbDnsName(String environment) {
    elbDnsName = sh(returnStdout: true, script: "aws elbv2 describe-load-balancers --names bfd-${environment}-fhir-nlb --query 'LoadBalancer[0].DNSName' --output text").trim()
    return elbDnsName
}
