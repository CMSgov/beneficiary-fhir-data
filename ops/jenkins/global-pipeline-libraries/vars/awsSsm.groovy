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
    typeOpt = "--type ${args.parameterType ?: 'String'}"
    tags = "--tags ${args.parameterTags ?: "Key=Source,Value=${JOB_NAME} Key=Environment,Value=mgmt Key=stack,Value=mgmt Key=Terraform,Value=False Key=application,Value=bfd Key=business,Value=oeda"}"
    overwriteOpt = args.shouldOverwrite ? '--overwrite' : ''
    awsRegionOpt = "--region ${args.awsRegion ?: 'us-east-1'}"

    rTypeOpt = "--resource-type ${args.resourceType ?: 'Parameter'}"
    rId = "--resource-id ${args.resourceId ?: name}"

    // TODO this is very naive and there are a crazy number of cases that this does not support. Beware.
    parameterOutput = sh(returnStdout: true, script: "aws ssm put-parameter --name ${name} --value '${value}' ${typeOpt} ${awsRegionOpt} ${overwriteOpt}").trim()
    tagResource()
    return parameterOutput
}

// Adds or overwrites one or more tags for the specified resource
String tagResource(Map args = [:]) {
    //If Parameter, this should be called by putParameter()
    if (env.rTypeOpt == '--resource-type Parameter') {
        tagParameter = sh(returnStdout: true, script: "aws ssm add-tags-to-resource ${rTypeOpt} ${rId} ${tags}").trim()
        return tagParameter
    }

    type = "--resource-type ${args.resourceType}"
    id = "--resource-id ${args.resourceId}"
    tags = "--tags ${args.resourceTags ?: "Key=Source,Value=${JOB_NAME} Key=Environment,Value=mgmt Key=stack,Value=mgmt Key=Terraform,Value=False Key=application,Value=bfd Key=business,Value=oeda"}"

    tagOutput = sh(returnStdout: true, script: "aws ssm add-tags-to-resource ${type} ${id} ${tags}").trim()
    return tagOutput
}
