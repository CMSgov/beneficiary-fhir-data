#!/usr/bin/env groovy
// awsSsm.groovy contains methods that wrap awscli ssm subcommands

// Gets value from SSM parameter store based on parameter name.
// If a SSM parameter does not exist, it will result in an exception.
String getParameter(Map args = [:]) {
    name = args.parameterName
    awsRegion = args.awsRegion ?: 'us-east-1'

    return sh(returnStdout: true, script: "aws ssm get-parameter --name ${name} --region ${awsRegion} --query 'Parameter.Value' --output text").trim()
}

// Stores a value in SSM parameter store based on the parameter name.  If a parameter doesn't exist in
// parameter store, this function will store the value under the parameter name passed in and will
// default the type stored as a 'String' unless otherwise passed in as a parameter type in the arguments.
String putParameter(Map args = [:]) {
    name = args.parameterName
    value = args.parameterValue
    type = args.parameterType ?: 'String'
    overwrite = args.shouldOverwrite ? '--overwrite' : ''
    awsRegion = args.awsRegion ?: 'us-east-1'
    includeType = "--type ${type}"

    // TODO this is very naive and there are a crazy number of cases that this does not support. Beware.
    output = sh(returnStdout: true, script: "aws ssm put-parameter --name ${name} --value '${value}' ${includeType} --tags ${tags} --region ${awsRegion} ${overwrite}").trim()
    return output
}

// Adds or overwrites one or more tags for the specified resource
String tagResource(Map args = [:]) {
    type = args.resourceType ?: 'Parameter'
    id = args.resourceId
    tags = args.resourceTags

    output = sh(returnStdout: true, script: "aws ssm add-tags-to-resource --resource-type '${type}' --resource-id '${id}' --tags '${tags}'").trim()
    return output
}
