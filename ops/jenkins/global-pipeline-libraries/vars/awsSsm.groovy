#!/usr/bin/env groovy
// awsSsm.groovy contains methods that wrap awscli ssm subcommands

// TODO
String getParameter(Map args = [:]) {
    name = args.parameterName
    awsRegion = args.awsRegion ?: 'us-east-1'

    value = sh(returnStdout: true, script: "aws ssm get-parameter --name ${name} --region ${awsRegion} --query 'Parameter.Value' --output text").trim()
    echo "returned value is ${value}"
    return value
}

String putParameter(Map args = [:]) {
    name = args.parameterName
    value = args.parameterValue
    type = args.parameterType ? args.parameterType : ''
    test = args.parameterTest ? args.parameterTest : ''
    shouldOverwrite = args.shouldOverwrite ? true : false
    awsRegion = args.awsRegion ?: 'us-east-1'

    echo "Seeing if test is null or not ${test}"
    if (shouldOverwrite) {
        overwrite = '--overwrite'
    }
    else {
        overwrite = ''
    }

    includeType = ''

    if(type) {
     includeType = '--type {$type}'
    }


    // TODO this is very naive and there are a crazy number of cases that this does not support. Beware.
    output = sh(returnStdout: true, script: "aws ssm put-parameter --name ${name} --value '${value}' ${type} --region ${awsRegion} ${overwrite}").trim()
    return output
}
