#!/usr/bin/env groovy

String getFormattedMonitorMsg(String msg) {
    return "[Migrator monitor]: ${msg}"
}

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

    sqsQueueName = awsSsm.getParameter(
        parameterName: "/bfd/${bfdEnv}/migrator/nonsensitive/sqs_queue_name",
        awsRegion: awsRegion
    )

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

    /** App has finished processing with no errors. See gov.cms.bfd.migrator.app.MigratorProgress */
    if (finalMigratorStatus[0] == "Finished") {
        // set return value for final disposition
        migratorDeployedSuccessfully = true
        awsSsm.putParameter(
            parameterName: "/bfd/${bfdEnv}/common/nonsensitive/database_schema_version",
            parameterValue: finalMigratorStatus[1],
            parameterType: "String",
            parameterTags: ["Environment": bfdEnv, "stack": bfdEnv],
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
    migratorStartTimestamp = java.time.LocalDateTime.now().toString()
    println getFormattedMonitorMsg("Begin Migrator monitoring - ${migratorStartTimestamp}")
    println getFormattedMonitorMsg("NOTE - Messages may appear out of order. Refer to the messageId for the true " +
        "order when debugging.")

    sqsQueueName = args.sqsQueueName
    awsRegion = args.awsRegion
    heartbeatInterval = args.heartbeatInterval
    maxMessages = args.maxMessages
    sqsQueueUrl = awsSqs.getQueueUrl(sqsQueueName)
    latestSchemaVersion = sh(returnStdout: true, script: "./ops/jenkins/scripts/getLatestSchemaMigrationScriptVersion.sh") as Integer
    while(true) {
        awsAuth.assumeRole()
        hasMessages = true;
        while(hasMessages) {
            messages = awsSqs.receiveMessages(
                sqsQueueUrl: sqsQueueUrl,
                awsRegion: awsRegion,
                visibilityTimeoutSeconds: 30,
                waitTimeSeconds: 20,
                maxMessages: maxMessages
            )

            // 1. "handle" (capture status, print, delete) each message
            // 2. if the message body contains a non-running value, return it
            for (msg in messages) {
                body = msg.body
                println getFormattedMonitorMsg(getMigratorStatus(body))
                awsSqs.deleteMessage(msg.receipt, sqsQueueUrl)
                /** App has finished processing with no errors. See gov.cms.bfd.migrator.app.MigratorProgress */
                if (body.appStage == "Finished") {
                    return [body.appStage, latestSchemaVersion]
                }
            }
            hasMessages = messages.size() > 0
        }
        sleep(heartbeatInterval)
    }
    println getFormattedMonitorMsg("Migrator started at ${migratorStartTimestamp} is no longer running with a final status of '${migratorStatus}' at ${java.time.LocalDateTime.now().toString()}")
}

// return migrator status as a formatted string
String getMigratorStatus(Map body = [:]) {
    migratorStatus = body.appStage
    migrationStage = body?.migrationStage
    msgIdPrefix = "[${body.messageId}]"
    if (migrationStage != null) {
        statusStage = "${migratorStatus} (${migrationStage.stage})"
        if (migrationStage?.migrationFile != null) {
            return "${msgIdPrefix} ${statusStage}: ${migrationStage.migrationFile} (schema version ${migrationStage.version}) ..."
        } else {
            return "${msgIdPrefix} ${statusStage}"
        }
    }
    return "${msgIdPrefix} ${migratorStatus}"
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
            waitTimeSeconds: 0
        )
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
        echo "Exception has been encountered getting the parameter /bfd/${bfdEnv}/common/nonsensitive/database_schema_version from the aws ssm, missing ssm parameter for stored schema version."
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
