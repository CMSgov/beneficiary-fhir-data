#!/usr/bin/env groovy
// awsSsm.groovy contains methods that wrap awscli ssm subcommands

// Gets value from SSM parameter store based on parameter name.
// If a SSM parameter does not exist, it will result in an exception.
String getParameter(Map args = [:]) {
    name = args.parameterName
    awsRegion = args.awsRegion ?: 'us-east-1'
    isSensitive = args.isSensitive ?: false

    echo """Sensitive Flag: ${isSensitive}"""

    // Construct the AWS CLI command dynamically based on isSensitive
    def awsCmd = isSensitive
            ? "aws ssm get-parameter --name ${name} --region ${awsRegion} --with-decryption --query 'Parameter.Value' --output text"
            : "aws ssm get-parameter --name ${name} --region ${awsRegion} --query 'Parameter.Value' --output text"

    // Execute the AWS CLI command using a single sh step
    return sh(returnStdout: true, script: awsCmd).trim()
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
    output = sh(returnStdout: true, script: "aws ssm put-parameter --name ${name} --value '${value}' ${includeType} --region ${awsRegion} ${overwrite}").trim()
    return output
}
