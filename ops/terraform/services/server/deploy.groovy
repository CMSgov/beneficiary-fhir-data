#!/usr/bin/env groovy

// Load the sqs-specific methods
sqs = load('ops/terraform/services/common/sqs.groovy')

/* Deploys regression test suite via terraform
 * @param args a {@link Map} must include `bfdEnv`; optionally `dockerImageTagOverride`
 * <ul>
 * <li>bfdEnv string represents the targeted BFD SDLC Environment
 * <li>dockerImageTagOverride string represents an override regression test suite image
 * </ul>
*/
def deployServerRegression(Map args = [:]) {
    bfdEnv = args.bfdEnv
    dockerImageTagOverride = args.dockerImageTagOverride

    dir("${workspace}/ops/terraform/services/server/server-regression") {
        // Debug output terraform version
        sh "terraform --version"

        // Initilize terraform
        sh "terraform init -no-color"

        // - Attempt to create the desired workspace
        // - Select the desired workspace
        // NOTE: this is the terraform concept of workspace **NOT** Jenkins
        sh """
terraform workspace new "$bfdEnv" 2> /dev/null || true &&\
terraform workspace select "$bfdEnv" -no-color
"""
        // Gathering terraform plan
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
        if (dockerImageTagOverride != null) {
            sh """
terraform plan \
-var='docker_image_tag_override=${dockerImageTagOverride}' \
-no-color -out=tfplan
"""
        } else {
            sh "terraform plan -no-color -out=tfplan"
        }

        // Apply Terraform plan
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
        sh '''
terraform apply \
-no-color -input=false tfplan
'''
        echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
    }
}

/* Runs envionment-specific regression test suite via SQS signal and returns if the regression suite was successful
 * @param args a {@link Map} must include `bfdEnv`
 * <ul>
 * <li>awsRegion string the targeted AWS AZ region
 * <li>bfdEnv string represents the targeted BFD SDLC Environment
 * <li>gitBranchName string the name of the current git branch being ran
 * <li>heartbeatInterval int the interval of time, in seconds, to heartbeat the signal SQS queue
 * </ul>
*/
def runServerRegression(Map args = [:], authFunction) {
    awsRegion = args.awsRegion ?: 'us-east-1'
    bfdEnv = args.bfdEnv
    gitBranchName = args.gitBranchName
    heartbeatInterval = args.heartbeatInterval ?: 15

    // This queue is used to trigger the lambda
    lambdaSqsQueueName = "bfd-${bfdEnv}-server-regression"
    lamdaSqsQueueUrl = sqs.getQueueUrl(lambdaSqsQueueName)

    // This queue is posted to by the lambda to signal to the pipeline
    // the result of the test run
    signalSqsQueueName = "bfd-${bfdEnv}-server-regression-signal"

    currentBuildId = currentBuild.id
    lastSuccessfulBuildID = getLastSuccessfulBuildNum()
    // Athena only accepts partitions (which the compare/store tags, which are constructed
    // from the branch name, are) with alphanumeric characters and the '_' character. We need
    // to sanitize the branch name in case it contains invalid characters
    sanitizedBranchName = gitBranchName.replaceAll(/[^0-9a-zA-Z_]/, '_')
                                       .replaceAll(/[\_]+/, '_')
                                       .toLowerCase()

    if (canServerRegressionRunProceed(awsRegion, signalSqsQueueName, bfdEnv)) {
        println "Proceeding to run bfd-${bfdEnv}-server-regression lambda..."
    } else {
        println "Halting bfd-${bfdEnv}-server-regression lambda run. Check the SQS Queue ${signalSqsQueueName}"
        return false
    }

    sqsMessage = writeJSON(returnText: true, json: [
        'host': "https://${bfdEnv}.bfd.cms.gov",
        'suite_version': 'v2',
        'spawn_rate': 10,
        'users': 10,
        'spawned_runtime': '30s',
        'compare_tag': "build${lastSuccessfulBuildID}__${sanitizedBranchName}",
        'store_tag': "build${currentBuildId}__${sanitizedBranchName}"
    ])
    sqs.sendMessage(
        sqsQueueUrl: lamdaSqsQueueUrl,
        sqsMessage: sqsMessage
    )

    hasRegressionSucceeded = monitorServerRegression(
        sqsQueueName: signalSqsQueueName, 
        heartbeatInterval: heartbeatInterval,
        authFunction
    )

    sqs.purgeQueue(sqsQueueName)
    return hasRegressionSucceeded
}

/* Monitors the server-regression-signal SQS queue for when the server-regression lambda finishes */
boolean monitorServerRegression(Map args = [:], authFunction) {
    sqsQueueName = args.sqsQueueName
    awsRegion = args.awsRegion ?: 'us-east-1'
    heartbeatInterval = args.heartbeatInterval
    maxRetries = args.maxRetries ?: 15

    sqsQueueUrl = sqs.getQueueUrl(sqsQueueName)
    for (int i = 0; i < maxRetries; i++) {
        authFunction()
        messages = sqs.receiveMessages(
            sqsQueueUrl: sqsQueueUrl,
            awsRegion: awsRegion,
            visibilityTimeoutSeconds: 30,
            waitTimeSeconds: 20,
            maxMessages: 1
        )

        // The server-regression lambda will post only one message once finished,
        // so we just need to ensure that the number of messages returned is greater
        // than 0 and then capture the first message
        if (!messages.isEmpty()) {
            msg = messages[0]

            printServerRegressionMessage(msg)
            sqs.deleteMessage(msg.receipt, sqsQueueUrl)

            testRunResult = msg.body.result
            return testRunResult == 'SUCCESS' ? true : false
        } else {
            println "[Attempt ${i + 1}/${maxRetries}] No messages posted to ${sqsQueueName} yet, waiting ${heartbeatInterval} seconds to check again..."
        }

        sleep(heartbeatInterval)
    }
}

// checks for indications of a running server-regression lambda by looking for unconsumed SQS messages
boolean canServerRegressionRunProceed(String awsRegion, String sqsQueueName, String bfdEnv) {
    println "Checking server-regression ${sqsQueueName} state..."

    if (sqs.queueExists(sqsQueueName)) {
        sqsQueueUrl = sqs.getQueueUrl(sqsQueueName)
        println "Queue ${sqsQueueName} exists. Checking for messages in ${sqsQueueUrl}..."
        serverRegressionSignalMsgs = sqs.receiveMessages(
                sqsQueueUrl: sqsQueueUrl,
                awsRegion: awsRegion,
                maxMessages: 10,
                visibilityTimeoutSeconds: 0,
                waitTimeSeconds: 0)
        if (serverRegressionSignalMsgs?.isEmpty()) {
            println "Queue ${sqsQueueName} is empty. bfd-${bfdEnv}-server-regression run can proceed"
            return true
        } else {
            println "Queue ${sqsQueueName} has messages. Is there an ongoing run of the server-regression lambda for the ${bfdEnv} environment?"
            return false
        }
    } else {
        println "Queue ${sqsQueueName} can not be found. Was the ${sqsQueueName} destroyed?"
        return false
    }
}

// print formatted migrator messages
def printServerRegressionMessage(Map message) {
    body = message.body

    cloudWatchUrl = getCloudWatchLogUrl(
        logGroupName: body.log_group_name,
        logStreamName: body.log_stream_name
    )
    println "Timestamp: ${java.time.LocalDateTime.now().toString()}"
    println """${body.function_name} finished running with a ${body.result} result, more information:
                |   Lambda Request ID: ${body.request_id}
                |   CloudWatch log stream name: ${body.log_stream_name}
                |   CloudWatch log group name: ${body.log_group_name}
                |   CloudWatch URL: ${cloudWatchUrl}""".stripMargin()
}

/* Gets the build ID of the last successful Jenkins job build of the current branch */
def getLastSuccessfulBuildNum() {
    def lastSuccessfulBuildID = 0
    def build = currentBuild.previousBuild
    while (build != null) {
        if (build.result == "SUCCESS")
        {
            lastSuccessfulBuildID = build.id as Integer
            break
        }
        build = build.previousBuild
    }

    return lastSuccessfulBuildID
}

/* Builds a CloudWatch log URL given a Log Group and a Log Stream */
String getCloudWatchLogUrl(Map args = [:]) {
    awsRegion = args.awsRegion ?: 'us-east-1'
    logGroupName = args.logGroupName
    logStreamName = args.logStreamName

    cloudWatchUrl = [
        "https://${awsRegion}.console.aws.amazon.com/cloudwatch/home?region=${awsRegion}",
        "#logsV2:log-groups/log-group/",
        URLEncoder.encode(logGroupName, "UTF-8"),
        '/log-events/',
        URLEncoder.encode(logStreamName, "UTF-8")
    ].join("")

    return cloudWatchUrl
}

return this
