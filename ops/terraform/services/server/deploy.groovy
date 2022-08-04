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

/* Runs envionment-specific regression test suite via SQS signal.
 * @param args a {@link Map} must include `bfdEnv`
 * <ul>
 * <li>bfdEnv string represents the targeted BFD SDLC Environment
 * <li>gitBranchName string the name of the current git branch being ran
 * </ul>
*/
def runServerRegression(Map args = [:]) {
    bfdEnv = args.bfdEnv
    gitBranchName = args.gitBranchName

    locustSqsQueueName = "bfd-${bfdEnv}-server-regression"
    locustSqsQueueUrl = sh(
        returnStdout: true,
        script: "aws sqs get-queue-url --queue-name ${locustSqsQueueName} --output text"
    ).trim()

    currentBuildId = currentBuild.id
    lastSuccessfulBuildID = getLastSuccessfulBuildNum()
    // Athena only accepts partitions (which the compare/store tags, which are constructed
    // from the branch name, are) with alphanumeric characters and the '_' character. We need
    // to sanitize the branch name in case it contains invalid characters
    sanitizedBranchName = gitBranchName.replaceAll(/[^0-9a-zA-Z_]/, '_')
                                       .replaceAll(/[\_]+/, '_')
                                       .toLowerCase()

    sqsMessage = writeJSON(returnText: true, json: [
        'host': "https://${bfdEnv}.bfd.cms.gov",
        'suite_version': 'v2',
        'spawn_rate': 10,
        'users': 10,
        'spawned_runtime': '30s',
        'compare_tag': "build${lastSuccessfulBuildID}__${sanitizedBranchName}",
        'store_tag': "build${currentBuildId}__${sanitizedBranchName}"
    ])

    withEnv(["SQS_QUEUE_URL=${locustSqsQueueUrl}", "MESSAGE=${sqsMessage}"]) {
        sh(returnStdout: true,
            script: '''
aws sqs send-message \
--queue-url "$SQS_QUEUE_URL" \
--message-body "$MESSAGE"
''')}
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

return this
