#!/usr/bin/env groovy
// awsSsm.groovy contains methods that wrap awscli ssm subcommands

// Gets value from SSM parameter store based on parameter name.
// If a SSM parameter does not exist, it will result in an exception.
String getParameter(Map args = [:]) {
    name = args.parameterName
    awsRegion = args.awsRegion ?: 'us-east-1'

    return sh(returnStdout: true, script: "aws ssm get-parameter --name ${name} --region ${awsRegion} --with-decryption --query 'Parameter.Value' --output text").trim()
}

// Stores a value in SSM parameter store based on the parameter name.  If a parameter doesn't exist in
// parameter store, this function will store the value under the parameter name passed in and will
// default the type stored as a 'String' unless otherwise passed in as a parameter type in the arguments.
String putParameter(Map args = [:]) {
    name = args.parameterName
    value = args.parameterValue
    type = args.parameterType ?: "String"
    applyOverwrite = args.shouldOverwrite ? "--overwrite" : ""
    awsRegion = args.awsRegion ?: "us-east-1"

    // TODO this is very naive and there are a crazy number of cases that this does not support. Beware.
    parameterOutput = sh(returnStdout: true, script: "aws ssm put-parameter --name ${name} --value \"${value}\" --type ${type} --region ${awsRegion} ${applyOverwrite}").trim()
    putParameterTags(args.parameterTags, args.resourceId ?: name, args.resourceType)
    return parameterOutput
}

String putParameterTags(tagsMap, resourceId, resourceType) {
    defaultTags = ["Source": "${JOB_NAME}", "Environment": "mgmt", "stack": "mgmt", "Terraform": "false", "application": "bfd", "business": "oeda"]
    tags = (defaultTags + tagsMap).collect{ key, value -> "\"Key=${key},Value=${value}\"" }.join(" ")
    tagParameter = sh(returnStdout: true, script: "aws ssm add-tags-to-resource --resource-id ${resourceId} --resource-type ${resourceType ?: 'Parameter'} --tags ${tags}").trim()
    return tagParameter
}
