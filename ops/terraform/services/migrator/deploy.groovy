#!/usr/bin/env groovy

// entrypoint to migrator deployment, requires mapped arguments and an aws authentication closure
// attempts to deploy and monitor and return `true` when the migrator signals a zero exit status
boolean deployMigrator(Map args = [:]) {
    amiId = args.amiId
    bfdEnv = args.bfdEnv
    heartbeatInterval = args.heartbeatInterval ?: 30
    awsRegion = args.awsRegion ?: 'us-east-1'
    forceDeployment = args.forceDeployment ?: false

    // authenticate
    awsAuth.assumeRole()

    // set sqsQueueName
    sqsQueueName = "bfd-${bfdEnv}-migrator"

    // prechecks
    if (isMigratorDeploymentRequired(bfdEnv, awsRegion) || forceDeployment) {
        println "Migrator deployment is required"
    } else {
        println "Migrator deployment is NOT required. Skipping migrator deployment."
        return true
    }

    if (canMigratorDeploymentProceed(sqsQueueName, awsRegion)) {
        println "Proceeding to Migrator Deployment"
    } else {
        println "Halting Migrator Deployment. Check the SQS Queue ${sqsQueueName}."
        return false
    }

    // plan/apply terraform
    terraform.deployTerraservice(
	    env: bfdEnv,
	    directory: "ops/terraform/services/migrator",
	    tfVars: [
                ami_id_override: amiId,
                create_migrator_instance: true,
                migrator_monitor_heartbeat_interval_seconds_override: heartbeatInterval
	    ]
    )

    // monitor migrator deployment
    finalMigratorStatus = monitorMigrator(
        sqsQueueName: sqsQueueName,
        awsRegion: awsRegion,
        heartbeatInterval: heartbeatInterval,
        maxMessages: 10
    )

    // re-authenticate
    awsAuth.assumeRole()

    // set return value for final disposition
    if (finalMigratorStatus[0] == '0') {
        migratorDeployedSuccessfully = true
        awsSsm.putParameter(
            parameterName: "/bfd/${bfdEnv}/common/nonsensitive/database_schema_version",
            parameterValue: finalMigratorStatus[1],
            parameterType: "String",
            shouldOverwrite: true
        )

        // Teardown when there is a healthy exit status
        terraform.deployTerraservice(
            env: bfdEnv,
            directory: "ops/terraform/services/migrator",
            tfVars: [
                ami_id_override: amiId,
                create_migrator_instance: false
            ]
        )
    } else {
        migratorDeployedSuccessfully = false
    }

    awsSqs.purgeQueue(sqsQueueName)

    println "Migrator completed with exit status ${finalMigratorStatus[0]}"
    return migratorDeployedSuccessfully
}


// polls the given AWS SQS Queue `sqsQueueName` for migrator messages for
// 20s at the `heartbeatInterval`
def monitorMigrator(Map args = [:]) {
    sqsQueueName = args.sqsQueueName
    awsRegion = args.awsRegion
    heartbeatInterval = args.heartbeatInterval
    maxMessages = args.maxMessages

    sqsQueueUrl = awsSqs.getQueueUrl(sqsQueueName)
    while (true) {
        awsAuth.assumeRole()
        messages = awsSqs.receiveMessages(
            sqsQueueUrl: sqsQueueUrl,
            awsRegion: awsRegion,
            visibilityTimeoutSeconds: 30,
            waitTimeSeconds: 20,
            maxMessages: maxMessages
        )

        // 1. "handle" (capture status, print, delete) each message
        // 2. if the message body contains a non "0/0" (running) value, return it
        for (msg in messages) {
            migratorStatus = msg.body.status
            println "Migrator schema version is at ${msg.body.schema_version}"
            schemaVersion = msg.body.schema_version
            printMigratorMessage(msg)
            awsSqs.deleteMessage(msg.receipt, sqsQueueUrl)
            if (migratorStatus =='0') {
                def resultsList = [migratorStatus, schemaVersion]
                return resultsList
            }
        }
        sleep(heartbeatInterval)
    }
}

// print formatted migrator messages
void printMigratorMessage(message) {
    body = message.body
    println "Timestamp: ${java.time.LocalDateTime.now().toString()}"

    if (body.stop_time == "n/a") {
        println "Migrator ${body.pid} started at ${body.start_time} is running"
    } else {
        println "Migrator ${body.pid} started at ${body.start_time} is no longer running: '${body.code}' '${body.status}' as of ${body.stop_time}"
    }
}

// checks for indications of a running migrator deployment by looking for unconsumed SQS messages
boolean canMigratorDeploymentProceed(String sqsQueueName, String awsRegion) {
    println "Checking Migrator Queue ${sqsQueueName} State..."

    if (awsSqs.queueExists(sqsQueueName)) {
        sqsQueueUrl = awsSqs.getQueueUrl(sqsQueueName)
        println "Queue ${sqsQueueName} exists. Checking for messages in ${sqsQueueUrl} ..."
        migratorMessages = awsSqs.receiveMessages(
                sqsQueueUrl: sqsQueueUrl,
                awsRegion: awsRegion,
                maxMessages: 10,
                visibilityTimeoutSeconds: 0,
                waitTimeSeconds: 0)
        if (migratorMessages?.isEmpty()) {
            println "Queue ${sqsQueueName} is empty. Migrator deployment can proceed!"
            return true
        } else {
            println "Queue ${sqsQueueName} has messages. Is there an old bfd-db-migrator instance running? Migrator deployment cannot proceed."
            return false
        }
    } else {
        println "Queue ${sqsQueueName} can not be found. Migrator deployment can proceed!"
        return true
    }
}

// checks to determine whether the migrator deployment is required
// returns true when the latest versioned migration script is newer
// than the database's stored schema version
// otherwise false
boolean isMigratorDeploymentRequired(String bfdEnv, String awsRegion) {
    println "Comparing schema migration versions..."
    // Initialize stored schema version
    int storedSchemaVersion = 0
    // check SSM Parameter Store
    try {
        storedSchemaVersion = awsSsm.getParameter(
                parameterName: "/bfd/${bfdEnv}/common/nonsensitive/database_schema_version",
                awsRegion: awsRegion
        ) as Integer
    } catch(Exception ex) {
        echo "Exception has been encountered getting the storedSchemaVersion from aws ssm, missing ssm parameter for stored schema version."
        return true
    }
    echo "Stored Schema Version : ${storedSchemaVersion}"

    // check latest available versioned migration
    latestAvailableMigrationVersion = sh(returnStdout: true, script: "./ops/jenkins/scripts/getLatestSchemaMigrationScriptVersion.sh") as Integer
    echo "Latest Available Migration Version: ${latestAvailableMigrationVersion}"

    // compare and determine
    return latestAvailableMigrationVersion > storedSchemaVersion
}

return this
