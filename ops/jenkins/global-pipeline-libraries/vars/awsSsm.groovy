#!/usr/bin/env groovy
// awsSsm.groovy contains methods that wrap awscli ssm subcommands

// TODO
String getParameter(Map args = [:]) {
    name = args.parameterName
    awsRegion = args.awsRegion ?: 'us-east-1'

    value = sh(returnStdout: true, script: "aws ssm get-parameter --name ${name} --region ${awsRegion} --query 'Parameter.Value' --output text").trim()

    return value
}

String putParameter(Map args = [:]) {
    name = args.parameterName
    value = args.parameterValue
    shouldOverwrite = args.overwrite ? true : false
    awsRegion = args.awsRegion ?: 'us-east-1'

    if (shouldOverwrite) {
        overwrite = '--overwrite'
    }
    else {
        overwrite = ''
    }

    // TODO this is very naive and there are a crazy number of cases that this does not support. Beware.
    output = sh(returnStdout: true, script: "aws ssm put-parameter --name ${name} --value '${value}' --region ${awsRegion} ${overwrite}").trim()
    return output
}
