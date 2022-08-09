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

/* Wrapping `aws sqs receive-message`, this returns a JSON array of message bodies and their receipts from sqs.
 *
 * @param args a {@link Map} must include maxMessages, sqsQueueUrl, visibilityTimeoutSeconds,
 * and waitTimeSeconds.
 * <ul>
 * <li>awsRegion targeted aws region. Defaults to 'us-east-1'</li>
 * <li>maxMessages as a maximum, SQS may return fewer but no more than this figure.</li>
 * <li>sqsQueueUrl targeted sqs queue url</li>
 * <li>visibilityTimeoutSeconds indicate amount of time to prevent other clients from reading a message</li>
 * <li>waitTimeSeconds long polling up to 20 seconds. This is the maximum amount of time to poll before
 * prematurely returning with empty results.</li>
 * </ul>
 */
def receiveMessages(Map args = [:]) {
    awsRegion = args.awsRegion ?: 'us-east-1'
    maxMessages = args.maxMessages
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

/* Wrapping `aws sqs send-message`, this sends a message to a given SQS queue
 *
 * @param args a {@link Map} must include sqsQueueUrl and sqsMessage
 * <ul>
 * <li>awsRegion targeted aws region. Defaults to 'us-east-1'</li>
 * <li>sqsQueueUrl targeted sqs queue url</li>
 * <li>sqsMessage string the message to send to the SQS queue</li>
 * <li>maxRetries int number of times to attempt sending a message to SQS queue before erroring</li>
 * <li>retryInterval int interval, in seconds, to wait between attempts to send messages to SQS queue</li>
 * </ul>
 */
def sendMessage(Map args = [:]) {
    awsRegion = args.awsRegion ?: 'us-east-1'
    sqsQueueUrl = args.sqsQueueUrl
    sqsMessage = args.sqsMessage
    maxRetries = args.maxRetries ?: 15
    retryInterval = args.retryInterval ?: 2

    for (int i = 0; i < maxRetries; i++) {
        withEnv(["SQS_QUEUE_URL=${sqsQueueUrl}", "MESSAGE=${sqsMessage}"]) { 
            returnCode = sh(returnStdout: true,
                returnStatus: true,
                script: '''
aws sqs send-message \
--queue-url "$SQS_QUEUE_URL" \
--message-body "$MESSAGE"
            '''.trim())

            if (returnCode == 0) {
                return
            }

            println "[Attempt ${i + 1}/${maxRetries}] Message not sent to \"${sqsQueueUrl}\" yet, waiting ${retryInterval} seconds to try again..."
            sleep(retryInterval)
        } 
    }

    error("Unable to send message to ${sqsQueueUrl} after ${maxRetries} retries")
}

return this
