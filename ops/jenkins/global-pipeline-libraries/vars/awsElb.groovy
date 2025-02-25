#!/usr/bin/env groovy
// awsElb.groovy contains methods that wrap awscli elb subcommands

// Returns the Green Elastic Load Balancer's DNSName for the given environment
// See ops/terraform/services/server/modules/bfd_server_asg/main.tf for NLB definition and naming scheme
String getGreenElbDnsName(String environment) {
    elbDnsName = sh(returnStdout: true, script: "aws elbv2 describe-load-balancers --names bfd-${environment}-fhir-nlb-green --query 'LoadBalancers[0].DNSName' --output text").trim()
    if (elbDnsName.equalsIgnoreCase("none")) {
        error("Unable to determine ELB DNSName for bfd-${environment}-fhir-nlb-green")
    }
    return elbDnsName
}
