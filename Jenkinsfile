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

properties([
	parameters([
		booleanParam(name: 'deploy_prod_skip_confirm', defaultValue: false, description: 'Whether to prompt for confirmation before deploying to most prod-like envs.'),
		booleanParam(name: 'use_latest_images', description: 'When true, defer to latest available AMIs. Skips App and App Image Stages.', defaultValue: false),
		booleanParam(name: 'verbose_mvn_logging', description: 'When true, `mvn` will produce verbose logs.', defaultValue: false),
		booleanParam(name: 'force_migrator_deployment', description: 'When true, force the migrator to deploy.', defaultValue: false),
		string(name: 'server_regression_image_override', description: 'Overrides the Docker image tag used when deploying the server-regression lambda', defaultValue: null)
	]),
	buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: ''))
])

// These variables are accessible throughout this file (except inside methods and classes).
def scriptForApps
def scriptForDeploys
def migratorScripts
def serverScripts
def canDeployToProdEnvs
def willDeployToProdEnvs
def appBuildResults
def amiIds
def currentStage
def gitCommitId
def gitRepoUrl
def gitBranchName
def awsRegion = 'us-east-1'
def verboseMaven = params.verbose_mvn_logging
def migratorRunbookUrl = "https://github.com/CMSgov/beneficiary-fhir-data/wiki/how-to-recover-from-migrator-failures"
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
				image: 'public.ecr.aws/c2o1d8s9/bfd-cbc-build:jdk21-mvn3-tfenv3-kt1.9-latest', // TODO: consider a smarter solution for resolving this image
				command: 'cat',
				ttyEnabled: true,
				alwaysPullImage: false, // NOTE: This implies that we observe immutable container images
				resourceRequestCpu: '8000m',
				resourceLimitCpu: '8000m',
				resourceLimitMemory: '16384Mi',
				resourceRequestMemory: '16384Mi'
			)], serviceAccount: 'bfd') {
		node(POD_LABEL) {
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

			stage('Prepare') {
				currentStage = env.STAGE_NAME
				container('bfd-cbc-build') {
					// Grab the commit that triggered the build.
					checkout scm

					// Address limitations resulting from CVE-2022-24767
					sh 'git config --global --add safe.directory "$WORKSPACE"'

					// Load the child Jenkinsfiles.
					scriptForApps = load('apps/build.groovy')
					scriptForDeploys = load('ops/deploy-ccs.groovy')

					// terraservice deployments...
					migratorScripts = load('ops/terraform/services/migrator/deploy.groovy')
					serverScripts = load('ops/terraform/services/server/deploy.groovy')

					awsAuth.assumeRole()

					// Find the most current AMI IDs (if any).
					amiIds = scriptForDeploys.findAmis(gitBranchName)

					// This variables track our decision on whether or not to deploy to prod-like envs.
					canDeployToProdEnvs = false

					if (env.TAG_NAME != null && !env.TAG_NAME.contains("-")) {
						canDeployToProdEnvs = true
						echo "Tag name matched pattern"
					}
					willDeployToProdEnvs = false

					// Get the current commit id
					gitCommitId = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

					// Get the remote repo url. This assumes we are using git+https not git+ssh.
					gitRepoUrl = sh(returnStdout: true, script: 'git config --get remote.origin.url').trim().replaceAll(/\.git$/,"")

					// Send notifications that the build has started
					sendNotifications('STARTED', currentStage, gitCommitId, gitRepoUrl)
				}
			}

			stage('Fetch Apps') {
				if (!params.use_latest_images) {
					currentStage = env.STAGE_NAME
					milestone(label: 'stage_fetch_app_start')

					container('bfd-cbc-build') {
						appFetchResults = scriptForApps.fetch()
					}
				}
			}

			stage('Build App AMIs') {
				if (!params.use_latest_images) {
					currentStage = env.STAGE_NAME
					milestone(label: 'stage_build_app_amis_test_start')

					container('bfd-cbc-build') {
						amiIds = scriptForDeploys.buildAppAmis(gitBranchName, gitCommitId, amiIds, appFetchResults)
					}
				}
			}

			bfdEnv = 'test'
			stage('Deploy Base to TEST') {
				currentStage = env.STAGE_NAME
				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_base_start')
					container('bfd-cbc-build') {
						awsAuth.assumeRole()
						terraform.deployTerraservice(
							env: bfdEnv,
							directory: "ops/terraform/services/base"
						)
					}
				}
			}

			stage('Deploy Common to TEST') {
				currentStage = env.STAGE_NAME
				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_common_start')
					container('bfd-cbc-build') {
						awsAuth.assumeRole()
						terraform.deployTerraservice(
							env: bfdEnv,
							directory: "ops/terraform/services/common"
						)
					}
				}
			}

			stage('Deploy EFT to TEST') {
				currentStage = env.STAGE_NAME
				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_eft_start')
					container('bfd-cbc-build') {
						awsAuth.assumeRole()
						terraform.deployTerraservice(
							env: bfdEnv,
							directory: "ops/terraform/services/eft"
						)
					}
				}
			}

			stage('Deploy Migrator to TEST') {
				currentStage = env.STAGE_NAME
				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_migration_start')
					container('bfd-cbc-build') {

						migratorDeploymentSuccessful = migratorScripts.deployMigrator(
							amiId: amiIds.bfdMigratorAmiId,
							bfdEnv: bfdEnv,
							heartbeatInterval: 30, // TODO: Consider implementing a backoff functionality in the future
							awsRegion: awsRegion,
							forceDeployment: params.force_migrator_deployment
						)
						if (migratorDeploymentSuccessful) {
							println "Proceeding to Stage: 'Deploy Pipeline to ${bfdEnv.toUpperCase()}'"
						} else {
							println "See ${migratorRunbookUrl} for troubleshooting resources."
							error('Migrator deployment failed')
						}
					}
				}
			}

			stage('Deploy Pipeline to TEST') {
				currentStage = env.STAGE_NAME
				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_pipeline_start')
					container('bfd-cbc-build') {
						awsAuth.assumeRole()
						terraform.deployTerraservice(
							env: bfdEnv,
							directory: "ops/terraform/services/pipeline",
							tfVars: [
								ami_id_override: amiIds.bfdPipelineAmiId
							]
						)
					}
				}
			}

			stage('Deploy Server to TEST') {
				currentStage = env.STAGE_NAME
				lock(resource: 'env_test') {
					milestone(label: 'stage_deploy_test_start')

					container('bfd-cbc-build') {
						awsAuth.assumeRole()
						terraform.deployTerraservice(
							env: bfdEnv,
							directory: "ops/terraform/services/server",
							tfVars: [
								ami_id_override: amiIds.bfdServerAmiId
							]
						)

						awsAuth.assumeRole()
						terraform.deployTerraservice(
							env: bfdEnv,
							directory: "ops/terraform/services/server/server-regression",
							tfVars: [
								docker_image_tag_override: params.server_regression_image_override
							]
						)

						// Deploy the API requests Insights Lambda
						awsAuth.assumeRole()
						terraform.deployTerraservice(
							env: bfdEnv,
							directory: "ops/terraform/services/server/insights/api-requests"
						)

						awsAuth.assumeRole()
						hasRegressionRunSucceeded = serverScripts.runServerRegression(
							bfdEnv: bfdEnv,
							gitBranchName: gitBranchName,
							isRelease: canDeployToProdEnvs
						)

						if (hasRegressionRunSucceeded) {
							println 'Regression suite passed, proceeding to next stage...'
						} else {
							try {
								input 'Regression suite failed, check the CloudWatch logs above for more details. Should deployment proceed?'
								echo "Regression suite failure in '${bfdEnv}' has been accepted by operator. Proceeding to next stage..."
							} catch(err) {
								error "Operator opted to fail deployment due to regression suite failure in '${bfdEnv}'"
							}
						}
					}
				}
			}

			stage('Manual Approval') {
				currentStage = env.STAGE_NAME
				// tag name must follow pattern to enable deploy to prod environments
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
					echo "Tag Name did not match pattern"
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Manual Approval')
				}
			}

			bfdEnv = 'prod-sbx'
			stage('Deploy Base to PROD-SBX') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod_sbx') {
						milestone(label: 'stage_deploy_prod_sbx_base_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/base"
							)
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-sbx')
				}
			}

			stage('Deploy Common to PROD-SBX') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod_sbx') {
						milestone(label: 'stage_deploy_prod_sbx_common_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/common"
							)
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-sbx')
				}
			}

			stage('Deploy Migrator to PROD-SBX') {
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
								forceDeployment: params.force_migrator_deployment
							)

							if (migratorDeploymentSuccessful) {
								println "Proceeding to Stage: 'Deploy Pipeline to ${bfdEnv.toUpperCase()}'"
							} else {
								println "See ${migratorRunbookUrl} for troubleshooting resources."
								error('Migrator deployment failed')
							}
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-sbx')
				}
			}

			stage('Deploy Pipeline to PROD-SBX') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod_sbx') {
						milestone(label: 'stage_deploy_prod_sbx_pipeline_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/pipeline",
								tfVars: [
									ami_id_override: amiIds.bfdPipelineAmiId
								]
							)
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-sbx')
				}
			}

			stage('Deploy Server to PROD-SBX') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod_sbx') {
						milestone(label: 'stage_deploy_prod_sbx_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/server",
								tfVars: [
									ami_id_override: amiIds.bfdServerAmiId
								]
							)

							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/server/server-regression",
								tfVars: [
									docker_image_tag_override: params.server_regression_image_override
								]
							)

							// Deploy the API requests Insights Lambda
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/server/insights/api-requests"
							)

							awsAuth.assumeRole()
							hasRegressionRunSucceeded = serverScripts.runServerRegression(
								bfdEnv: bfdEnv,
								gitBranchName: gitBranchName,
								isRelease: canDeployToProdEnvs
							)

							if (hasRegressionRunSucceeded) {
								println 'Regression suite passed, proceeding to next stage...'
							} else {
								try {
									input 'Regression suite failed, check the CloudWatch logs above for more details. Should deployment proceed?'
									echo "Regression suite failure in '${bfdEnv}' has been accepted by operator. Proceeding to next stage..."
								} catch(err) {
									error "Operator opted to fail deployment due to regression suite failure in '${bfdEnv}'"
								}
							}
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-sbx')
				}
			}


			bfdEnv = 'prod'
			stage('Deploy Base to PROD') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod') {
						milestone(label: 'stage_deploy_prod_base_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/base"
							)
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod')
				}
			}

			stage('Deploy Common to PROD') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod') {
						milestone(label: 'stage_deploy_prod_common_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/common"
							)
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod')
				}
			}

			stage('Deploy EFT to PROD') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod') {
						milestone(label: 'stage_deploy_prod_eft_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/eft"
							)
						}
					}
				}
			}

			stage('Deploy Migrator to PROD') {
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
								forceDeployment: params.force_migrator_deployment
							)

							if (migratorDeploymentSuccessful) {
								println "Proceeding to Stage: 'Deploy Pipeline to ${bfdEnv.toUpperCase()}'"
							} else {
								println "See ${migratorRunbookUrl} for troubleshooting resources."
								error('Migrator deployment failed')
							}
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod')
				}
			}

			stage('Deploy Pipeline to PROD') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod') {
						milestone(label: 'stage_deploy_prod_pipeline_start')
						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/pipeline",
								tfVars: [
									ami_id_override: amiIds.bfdPipelineAmiId
								]
							)
						}
					}
				} else {
					org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod')
				}
			}


			stage('Deploy Server to PROD') {
				currentStage = env.STAGE_NAME
				if (willDeployToProdEnvs) {
					lock(resource: 'env_prod') {
						milestone(label: 'stage_deploy_prod_start')

						container('bfd-cbc-build') {
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/server",
								tfVars: [
									ami_id_override: amiIds.bfdServerAmiId
								]
							)

							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/server/server-regression",
								tfVars: [
									docker_image_tag_override: params.server_regression_image_override
								]
							)

							// Deploy the API requests Insights Lambda
							awsAuth.assumeRole()
							terraform.deployTerraservice(
								env: bfdEnv,
								directory: "ops/terraform/services/server/insights/api-requests"
							)

							awsAuth.assumeRole()
							hasRegressionRunSucceeded = serverScripts.runServerRegression(
								bfdEnv: bfdEnv,
								gitBranchName: gitBranchName,
								isRelease: canDeployToProdEnvs
							)

							if (hasRegressionRunSucceeded) {
								println 'Regression suite passed, proceeding to next stage...'
							} else {
								try {
									input 'Regression suite failed, check the CloudWatch logs above for more details. Should deployment proceed?'
									echo "Regression suite failure in '${bfdEnv}' has been accepted by operator. Proceeding to next stage..."
								} catch(err) {
									error "Operator opted to fail deployment due to regression suite failure in '${bfdEnv}'"
								}
							}
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
