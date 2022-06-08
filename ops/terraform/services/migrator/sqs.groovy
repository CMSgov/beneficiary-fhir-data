#!/usr/bin/env groovy
// sqs.groovy contains methods that wrap awscli sqs subcommands

// returns the url of the given `sqsQueueName`
String getQueueUrl(String sqsQueueName, String awsRegion = 'us-east-1') {
    url = sh(returnStdout: true, script: "aws sqs get-queue-url --queue-name ${sqsQueueName} --region ${awsRegion} --output text").trim()
    return url
}

// deletes a message by the given `receipt`
// this indicates the associated message has been proceesed
String deleteMessage(String receipt, String sqsQueueUrl, String awsRegion = 'us-east-1') {
    result = sh(returnStdout: true, script: "aws sqs delete-message --queue-url ${sqsQueueUrl} --region ${awsRegion} --receipt-handle ${receipt}")
    return result
}

// returns true when if the given `sqsQueueName` exists
boolean queueExists(String sqsQueueName, String awsRegion = 'us-east-1') {
    // For whatever reason, the ternary expression doesn't work here. I still don't understand Groovy.
    exists = sh(returnStatus: true, script: "aws sqs get-queue-url --queue-name ${sqsQueueName} --region ${awsRegion} --output text")
    if (exists == 0) {
        return true
    } else {
        return false
    }
}

// purge all messages from the given
def purgeQueue(String sqsQueueName, String awsRegion = 'us-east-1') {
    sqsQueueUrl = getQueueUrl(sqsQueueName, awsRegion)
    result = sh(returnStdout: true, script: "aws sqs purge-queue --queue-url ${sqsQueueUrl} --region ${awsRegion}")
    return result
}

// wraps the aws cli's receive-message subcommand to produce JSON objects
// that represent the deployed migrator's state. Contains the following
// fields mapped to string values: pid, start_time, stop_time, status, code
def receiveMessages(Map args = [:]) {
    maxMessages = args.maxMessages
    awsRegion = args.awsRegion ?: 'us-east-1'
    sqsQueueUrl = args.sqsQueueUrl
    visibilityTimeoutSeconds = args.visibilityTimeoutSeconds
    waitTimeSeconds = args.waitTimeSeconds

    withEnv(["url=${sqsQueueUrl}",
             "region=${awsRegion}",
             "maxMessages=${maxMessages}",
             "waitTimeSeconds=${waitTimeSeconds}",
             "visibilityTimeoutSeconds=${visibilityTimeoutSeconds}"]) {
        rawMessages = sh(
            returnStdout: true,
            script: '''
#!/usr/bin/env bash
aws sqs receive-message \
  --region "$region" \
  --queue-url "$url" \
  --max-number-of-messages "$maxMessages" \
  --wait-time-seconds "$waitTimeSeconds" \
  --visibility-timeout "$visibilityTimeoutSeconds" \
  --output json \
  --query Messages |\
jq '.? | map({receipt: .ReceiptHandle, body: .Body | fromjson}) | unique'
'''
        ).trim()
    }
    try {
        jsonMessages = readJSON text: rawMessages
    } catch(err) {
        jsonMessages = readJSON text: '[]'
    }
    return jsonMessages
}

return this
