#!/usr/bin/env groovy
// awsEc2.groovy contains methods that wrap awscli ec2 subcommands

// Returns the AmiId for the given AMI and (Optional) git branch
String getAmiId(String gitBranch, String amiName) {
    command = "aws ec2 describe-images --owners self --filters \
        ${gitBranch != "" ? "'Name=tag:Branch,Values=${gitBranch}'" : ""} \
        'Name=name,Values=${amiName}' \
        'Name=state,Values=available' --region us-east-1 --output json | \
        jq -r '.Images | sort_by(.CreationDate) | last(.[]).ImageId'"
        amiId = sh(returnStdout: true, script: command).trim()
    return amiId
}
