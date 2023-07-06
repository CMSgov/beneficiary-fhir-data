#!/usr/bin/env groovy
// awsElb.groovy contains methods that wrap awscli elb subcommands

// Returns the Elastic Load Balancer's DNSName for the given environment
String getElbDnsName(String loadBalancerName) {
    elbDnsName = sh(returnStdout: true, script: "aws elb describe-load-balancers --load-balancer-names ${loadBalancerName} --query 'LoadBalancerDescriptions[0].DNSName' --output text").trim()
    return elbDnsName
}
