/* Deploys regression test suite via terraform
 * @param args a {@link Map} must include `bfdEnv`; optionally `dockerImageTagOverride`
 * <ul>
 * <li>bfdEnv string represents the targeted BFD SDLC Environment
 * <li>dockerImageTagOverride string represents an override regression test suite image
 * </ul>
*/
def deployLocustRegression(Map args = [:]) {
    bfdEnv = args.bfdEnv
    dockerImageTagOverride = args.dockerImageTagOverride

    dir("${workspace}/ops/terraform/services/server/bfd-regression-suite") {
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

/* Runs envionment-specific regression test suite via SQS signal.*/
/* @param args a {@link Map} must include `bfdEnv`
 * <ul>
 * <li>bfdEnv string represents the targeted BFD SDLC Environment
 * </ul>
*/
def runRegressionSuite(Map args = [:]) {
    bfdEnv = args.bfdEnv

    locustSqsQueueName = "bfd-${bfdEnv}-locust-regression"
    locustSqsQueueUrl = sh(
        returnStdout: true,
        script: "aws sqs get-queue-url --queue-name ${locustSqsQueueName} --output text"
    ).trim()

    sqsMessage = writeJSON(returnText: true, json: [
        'host': "https://${bfdEnv}.bfd.cms.gov",
        'suite_version': 'v2',
        'spawn_rate': 10,
        'users': 10,
        'spawned_runtime': '30s'
    ])

    withEnv(["SQS_QUEUE_URL=${locustSqsQueueUrl}", "MESSAGE=${sqsMessage}"]) {
        sh(returnStdout: true,
            script: '''
aws sqs send-message \
--queue-url "$SQS_QUEUE_URL" \
--message-body "$MESSAGE"
''')}
}

return this
