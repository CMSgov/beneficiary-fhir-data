#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. As this project is a
 * "monorepo", this Jenkinsfile is a bit more complicated than others. It's intended to:
 * </p>
 * <ol>
 * <li>Run a local-to-Jenkins integration build on all of the subprojects/modules here,
 * ensuring that all the pieces fit together as expected.</li>
 * <li>Run a deploy of the assembled artifacts into a lower AWS environment, to A) verify
 * that they can be deployed, B) verify that our smoke tests pass within that
 * environment, and C) to allow for further manual testing of the integrated systems
 * in/using that environment.</li>
 * <li>For builds of the master branch, it will also deploy the assembled artifacts into
 * our production environments.</li>
 * <li>Allow Jenkins users to easily run some of the tools contained within the monorepo,
 * when desired. For example, users can set XXX = XXX to run our performance tests in an environment of their choosing.</li>
 * </ol>
 * <p>
 * This top-level Jenkinsfile just handles the top-level concerns for the build: it loads
 * and calls other Jenkinsfile in the various subprojects/modules to handle concerns
 * specific to them.
 * </p>
 */

/*
 * Optionality:
 * - Performance Tests:
 *     - Default: 2 workers, 60 seconds
 *     - Extended: 4 workers, 300 seconds
 *     - Stress: 100 workers, 1800 seconds
 * - Misc. Tasks:
 *     - Build Platinum AMI (optional)
 */

properties([
	parameters([
		booleanParam(name: 'deploy_prod_from_non_master', defaultValue: false, description: 'Whether to deploy to prod-like envs for builds of this project\'s non-master branches.'),
		booleanParam(name: 'deploy_prod_skip_confirm', defaultValue: false, description: 'Whether to prompt for confirmation before deploying to most prod-like envs.'),
		booleanParam(name: 'build_platinum', description: 'Whether to build/update the "platinum" base AMI.', defaultValue: false),
		booleanParam(name: 'use_latest_images', description: 'When true, defer to latest available AMIs. Skips App and App Image Stages.', defaultValue: false),
		booleanParam(name: 'verbose_mvn_logging', description: 'When true, `mvn` will produce verbose logs.', defaultValue: false),
		string(name: 'locust_regression_image_override', description: 'Overrides the Docker image tag used when deploy the Locust regression lambda', defaultValue: null)
	]),
	buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: ''))
])

// These variables are accessible throughout this file (except inside methods and classes).
def awsCredentials
def scriptForApps
def scriptForDeploys
def migratorScripts
def locustRegressionScripts
def canDeployToProdEnvs
def willDeployToProdEnvs
def appBuildResults
def amiIds
def currentStage
def gitCommitId
def gitRepoUrl
def awsRegion = 'us-east-1'
def verboseMaven = params.verbose_mvn_logging

// send notifications to slack, email, etc
def sendNotifications(String buildStatus = '', String stageName = '', String gitCommitId = '', String gitRepoUrl = ''){
	// we will use this to display a link to diffs in the message. This assumes we are using git+https not git+ssh
	def diffsUrl = "${gitRepoUrl}/commit/"

	// buildStatus of NULL means success
	if (!buildStatus) {
		buildStatus = 'SUCCESS'
	}

	// build colors
	def colorMap = [:]
	colorMap['STARTED']  = '#0000FF'
	colorMap['SUCCESS']  = '#00FF00'
	colorMap['ABORTED']  = '#6A0DAD'
	colorMap['UNSTABLE'] = '#FFFF00'
	colorMap['FAILED']   = '#FF0000'
	def buildColor = colorMap[buildStatus]
	buildColor = buildColor ?: '#FF0000' // default to red

	// prettyfi messages
	def msg = ''
	switch (buildStatus){
		case 'UNSTABLE':
		case 'SUCCESS':
			msg = 'COMPLETED SUCCESSFULLY'
			break
		case 'FAILED':
		case 'FAILURE':
			msg = "FAILED ON ${stageName.toUpperCase()} STAGE"
			break
		case 'STARTED':
			msg = 'HAS STARTED'
			break
		case 'ABORTED':
			msg = 'WAS ABORTED'
			break
		default:
			msg = "${buildStatus.toUpperCase()}"
			break
	}

	// who launched the build
	def specificCause = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
	def startedBy = "${specificCause.shortDescription}".replaceAll("[\\[\\](){}]","")

	// build slack message
	def slackMsg = ''
	if (buildStatus == 'UNSTABLE'){
		slackMsg = "UNSTABLE BFD BUILD <${env.BUILD_URL}|#${env.BUILD_NUMBER}> ${msg} \n"
	} else {
		slackMsg = "BFD BUILD <${env.BUILD_URL}|#${env.BUILD_NUMBER}> ${msg} \n"
	}
	slackMsg+="\tJob '${env.JOB_NAME}' (${startedBy.toLowerCase()}) \n"
	slackMsg+="\tBranch: ${env.BRANCH_NAME == 'main' ? 'master' : env.BRANCH_NAME} \n"
	if (gitCommitId){
		// we will only have a gitCommitId if we've checked out the repo
		slackMsg+="\tView changes <${diffsUrl + gitCommitId}|here> \n"
	}

	// send Slack messages
	slackSend(color: buildColor, message: slackMsg)

	// future notifications can go here. (email, other channels, etc)
}

// begin pipeline
try {
	// See ops/jenkins/cbc-build-push.sh for this image's definition.
	podTemplate(
		containers: [
			containerTemplate(
				name: 'bfd-cbc-build',
				image: 'public.ecr.aws/c2o1d8s9/bfd-cbc-build:jdk11-mvn3-an29-tfenv',
				command: 'cat',
				ttyEnabled: true,
				alwaysPullImage: false, // NOTE: This implies that we observe immutable container images
				resourceRequestCpu: '8000m',
				resourceLimitCpu: '8000m',
				resourceLimitMemory: '16384Mi',
				resourceRequestMemory: '16384Mi'
			)], serviceAccount: 'bfd') {
		node(POD_LABEL) {
			stage('Prepare') {
				currentStage = env.STAGE_NAME
				container('bfd-cbc-build') {
					// Grab the commit that triggered the build.
					checkout scm

					// Load the child Jenkinsfiles.
					scriptForApps = load('apps/build.groovy')
					scriptForDeploys = load('ops/deploy-ccs.groovy')
					migratorScripts = load('ops/terraform/services/migrator/Jenkinsfile')
					locustRegressionScripts = load('ops/terraform/services/server/deploy.groovy')

					awsAssumeRole()

					// Find the most current AMI IDs (if any).
					amiIds = null
					amiIds = scriptForDeploys.findAmis()

					// These variables track our decision on whether or not to deploy to prod-like envs.
					canDeployToProdEnvs = env.BRANCH_NAME == "master" || params.deploy_prod_from_non_master
					willDeployToProdEnvs = false

					// Get the current commit id
					gitCommitId = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

					// Get the remote repo url. This assumes we are using git+https not git+ssh.
					gitRepoUrl = sh(returnStdout: true, script: 'git config --get remote.origin.url').trim().replaceAll(/\.git$/,"")

					// Send notifications that the build has started
					sendNotifications('STARTED', currentStage, gitCommitId, gitRepoUrl)
				}
			}

			/* This stage switches the gitBranchName (needed for our CCS downsream stages) 
			value if the build is a PR as the BRANCH_NAME var is populated with the build 
			name during PR builds. 
			*/
			stage('Set Branch Name') {
				currentStage = env.STAGE_NAME
				script {
					if (env.BRANCH_NAME.startsWith('PR')) {
						gitBranchName = env.CHANGE_BRANCH
					} else {
						gitBranchName = env.BRANCH_NAME
					}
				}
			}

			stage('Build Platinum AMI') {
				currentStage = env.STAGE_NAME
				if (params.build_platinum || amiIds.platinumAmiId == null) {
					milestone(label: 'stage_build_platinum_ami_start')

					container('bfd-cbc-build') {
						amiIds = scriptForDeploys.buildPlatinumAmi(amiIds)
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Build Platinum AMI')
				}
			}

			stage('Build Apps') {
				if (!params.use_latest_images) {
					currentStage = env.STAGE_NAME
					milestone(label: 'stage_build_apps_start')

					container('bfd-cbc-build') {
						appBuildResults = scriptForApps.build(verboseMaven)
					}
				}
			}

			stage('Build App AMIs') {
				if (!params.use_latest_images) {
					currentStage = env.STAGE_NAME
					milestone(label: 'stage_build_app_amis_test_start')

					container('bfd-cbc-build') {
						amiIds = scriptForDeploys.buildAppAmis(gitBranchName, gitCommitId, amiIds, appBuildResults)
					}
				}
			}

			stage('Deploy Migrator to TEST') {
				bfdEnv = 'test'
				currentStage = env.STAGE_NAME

				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_migration_start')
					container('bfd-cbc-build') {

						migratorDeploymentSuccessful = migratorScripts.deployMigrator(
							amiId: amiIds.bfdMigratorAmiId,
							bfdEnv: bfdEnv,
							heartbeatInterval: 30, // TODO: Consider implementing a backoff functionality in the future
							awsRegion: awsRegion,
							gitBranchName: gitBranchName,
							this.&awsAssumeRole
						)

						if (migratorDeploymentSuccessful) {
							println "Proceeding to Stage: 'Deploy to ${bfdEnv.toUpperCase()}'"
						} else {
							error('Migrator deployment failed')
						}
					}
				}
			}

			stage('Deploy to TEST') {
				currentStage = env.STAGE_NAME
				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_start')

					container('bfd-cbc-build') {
						awsAssumeRole()
						scriptForDeploys.deploy('test', gitBranchName, gitCommitId, amiIds)
					}

					locustRegressionScripts.deployLocustRegression(
						bfdEnv: bfdEnv,
						dockerImageTagOverride: params.locust_regression_image_override
					)

					// TODO: Run the regression suite once deployed
				}
			}

			stage('Manual Approval') {
				currentStage = env.STAGE_NAME
				if (canDeployToProdEnvs) {
					/*
					* Unless it was explicitly requested at the start of the build, prompt for confirmation before
					* deploying to production environments.
					*/
					if (!params.deploy_prod_skip_confirm) {
						/*
						* The Jenkins UI will prompt with "Proceed" and "Abort" options. If "Proceed" is
						* chosen, this build will continue merrily on as normal. If "Abort" is chosen,
						* an exception will be thrown.
						*/
						try {
							input 'Deploy to production environments (prod-sbx, prod)?'
							willDeployToProdEnvs = true
						} catch(err) {
							willDeployToProdEnvs = false
							echo 'User opted not to deploy to prod-like envs.'
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Manual Approval')
				}
			}

			stage('Deploy Migrator to PROD-SBX') {
				bfdEnv = 'prod-sbx'
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod_sbx') {
						milestone(label: 'stage_deploy_prod_sbx_migration_start')
						container('bfd-cbc-build') {

							migratorDeploymentSuccessful = migratorScripts.deployMigrator(
								amiId: amiIds.bfdMigratorAmiId,
								bfdEnv: bfdEnv,
								heartbeatInterval: 30, // TODO: Consider implementing a backoff functionality in the future
								awsRegion: awsRegion,
								gitBranchName: gitBranchName,
								this.&awsAssumeRole
							)

							if (migratorDeploymentSuccessful) {
								println "Proceeding to Stage: 'Deploy to ${bfdEnv.toUpperCase()}'"
							} else {
								error('Migrator deployment failed')
							}
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-sbx')
				}
			}

			stage('Deploy to PROD-SBX') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod_sbx') {
						milestone(label: 'stage_deploy_prod_sbx_start')
						container('bfd-cbc-build') {
							awsAssumeRole()
							scriptForDeploys.deploy('prod-sbx', gitBranchName, gitCommitId, amiIds)
						}

						locustRegressionScripts.deployLocustRegression(
							bfdEnv: bfdEnv,
							dockerImageTagOverride: params.locust_regression_image_override
						)

						// TODO: Run the regression suite once deployed
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-sbx')
				}
			}

			stage('Deploy Migrator to PROD') {
				bfdEnv = 'prod'
				currentStage = env.STAGE_NAME

				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod') {
						milestone(label: 'stage_deploy_prod_migration_start')
						container('bfd-cbc-build') {

							migratorDeploymentSuccessful = migratorScripts.deployMigrator(
								amiId: amiIds.bfdMigratorAmiId,
								bfdEnv: bfdEnv,
								heartbeatInterval: 30, // TODO: Consider implementing a backoff functionality in the future
								awsRegion: awsRegion,
								gitBranchName: gitBranchName,
								this.&awsAssumeRole
							)

							if (migratorDeploymentSuccessful) {
								println "Proceeding to Stage: 'Deploy to ${bfdEnv.toUpperCase()}'"
							} else {
								error('Migrator deployment failed')
							}
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod')
				}
			}

			stage('Deploy to PROD') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod') {
						milestone(label: 'stage_deploy_prod_start')

						container('bfd-cbc-build') {
							awsAssumeRole()
							scriptForDeploys.deploy('prod', gitBranchName, gitCommitId, amiIds)
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod')
				}
			}
		}
	}
} catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e){
	currentBuild.result = "ABORTED"
	throw e
} catch (ex) {
	currentBuild.result = "FAILURE"
	throw ex
} finally {
	sendNotifications(currentBuild.currentResult, currentStage, gitCommitId, gitRepoUrl)
}

/**
 * Wrap the awscli to authenticate with AWS by assuming a specific, fully qualified role. The role
 * must be the id of an available Jenkins secret.
 *
 * In addition to the setting credentials as environment variables for AWS requests, this also
 * sets AWS_REGION, DEFAULT_AWS_REGION, and a special NEXT_AWS_AUTH. NEXT_AWS_AUTH contains the
 * the time at which ~50% of the requested session has elapsed. If this environment variable is
 * populated, the script will only re-authenticate if there is less than half the time left in the
 * current session. By default, this requests a 60 minute session and subsequent calls of this
 * method will only reauthenticate if there are fewer than 30 minutes remaining on the session. This
 * keeps both the Jenkins logs tidier and avoids excessive role assumption as recorded in cloudwatch
 * logs. This is an especially important consideration in tight loops that seek to ensure valid AWS
 * credentials before or after each iteration.
 *
 * TODO: consider transitioning away from wrapped awscli role assumption to something in groovy/java
 * TODO: implement validation safety surrounding e.g. credentialsId, sessionName
 *
 * @param args {@link Map} that optionally includes sessionDurationSeconds, sessionName, awsRegion,
 * and credentialsId
 */
void awsAssumeRole(Map args = [:]) {
	sessionDurationSeconds = args.sessionDurationSeconds ?: 3600
	// default `sessionName` as `env.JOB_NAME` is vaguely sanitized to replace non-alphanumeric
	// chars with '-' AND deduplicates consecutive '-'. This is somewhat more restrictive than the
	// regex used in the STS API for RoleSessionName, i.e. `/[\w+=,.@:\/-]*/`
	// See https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html
	sessionName = args.sessionName ?: env.JOB_NAME.replaceAll(/[^a-zA-Z\d]/, '-').replaceAll(/[\-]+/, '-')
	credentialsId = args.credentialsId ?: 'bfd-aws-assume-role'
	awsRegion = args.awsRegion ?: 'us-east-1'

	if (env.NEXT_AWS_AUTH == null || java.time.Instant.now() > java.time.Instant.parse(env.NEXT_AWS_AUTH)) {
		echo "Authenticating..."
		withEnv(["DURATION=${sessionDurationSeconds}",
				 "SESSION_NAME=${sessionName}",
				 'AWS_ACCESS_KEY_ID=',
				 'AWS_SECRET_ACCESS_KEY=',
				 'AWS_SESSION_TOKEN=']) {
			withCredentials([string(credentialsId: credentialsId, variable: 'JENKINS_ROLE')]) {
				awsCredentials = sh(
					returnStdout: true,
					script: '''
aws sts assume-role \
  --duration-seconds "$DURATION" \
  --role-arn "$JENKINS_ROLE" \
  --role-session-name "$SESSION_NAME" \
  --output text --query Credentials
'''
				).trim().split(/\s+/)
				// Set nextAuthSeconds to renew through ~50% of original session's duration
				nextAuthSeconds = (sessionDurationSeconds / 2).longValue()
				env.NEXT_AWS_AUTH = java.time.Instant.now().plus(java.time.Duration.ofSeconds(nextAuthSeconds))
				env.AWS_REGION = awsRegion
				env.AWS_DEFAULT_REGION = awsRegion
				env.AWS_ACCESS_KEY_ID = awsCredentials[0]
				env.AWS_SECRET_ACCESS_KEY = awsCredentials[2]
				env.AWS_SESSION_TOKEN = awsCredentials[3]
			}
		}
	}
}
