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
 * - Deploy Env:
 *     - HealthAPT or CCS (exclusive)
 *     - hhsdevcloud (included in CCS deploys)
 *     - deploy Jenkins (optional, both envs)
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
		booleanParam(name: 'deploy_management', description: 'Whether to deploy/redeploy the management environment, which includes Jenkins. May cause the job to end early, if Jenkins is restarted.', defaultValue: false),
		booleanParam(name: 'deploy_prod_skip_confirm', defaultValue: false, description: 'Whether to prompt for confirmation before deploying to most prod-like envs.'),
		booleanParam(name: 'deploy_hhsdevcloud', description: 'Whether to deploy to the hhsdevcloud/"old sandbox" environment.', defaultValue: false),
		booleanParam(name: 'build_platinum', description: 'Whether to build/update the "platinum" base AMI.', defaultValue: false)
		//booleanParam(name: 'deploy_to_lss', description: 'Whether to run the Ansible plays for LSS systems (e.g. Jenkins itself).', defaultValue: false),
		//booleanParam(name: 'deploy_to_prod', description: 'Whether to run the Ansible plays for PROD systems (without prompting first, which is the default behavior).', defaultValue: false)
	]),
	buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: ''))
])

// These variables are accessible throughout this file (except inside methods and classes).
def deployEnvironment
def scriptForApps
def scriptForDeploys
def scriptForDeploysHhsdevcloud
def canDeployToProdEnvs
def willDeployToProdEnvs
def appBuildResults
def amiIds

stage('Prepare') {
	node {
		// Grab the commit that triggered the build.
		checkout scm

		// Deployment varies a bit by environment: are we running in the CCS?
		if (env.JENKINS_URL.contains('cmscloud')) {
			deployEnvironment = 'ccs'
		} else {
			deployEnvironment = 'healthapt'
		}

		// Load the child Jenkinsfiles.
		scriptForApps = load('apps/build.groovy')
		if (deployEnvironment == 'healthapt') {
			scriptForDeploys = load('ops/deploy-healthapt.groovy')
		} else if (deployEnvironment == 'ccs') {
			scriptForDeploys = load('ops/deploy-ccs.groovy')
		}
		scriptForDeploysHhsdevcloud = load('ops/deploy-hhsdevcloud.groovy')

		// Find the most current AMI IDs (if any).
		amiIds = null
		if (deployEnvironment == 'ccs') {
			amiIds = scriptForDeploys.findAmis()
		}

		// These variables track our decision on whether or not to deploy to prod-like envs.
		canDeployToProdEnvs = env.BRANCH_NAME == "master" || params.deploy_prod_from_non_master
		willDeployToProdEnvs = false
	}
}

if (deployEnvironment == 'ccs') {
	stage('Build Platinum AMI') {
		if (params.build_platinum || amiIds.platinumAmiId == null) {
			milestone(label: 'stage_build_platinum_ami_start')

			node {
				amiIds = scriptForDeploys.buildPlatinumAmi(amiIds)
			}
		} else {
			org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Build Platinum AMI')
		}
	}
}

stage('Deploy mgmt') {
	if (canDeployToProdEnvs && params.deploy_management) {
		lock(resource: 'env_management', inversePrecendence: true) {
			milestone(label: 'stage_management_jenkins_start')

			node {
				scriptForDeploy.deployManagement(amiIds)
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy mgmt')
	}
}

stage('Build Apps') {
	milestone(label: 'stage_build_apps_start')

	node {
		build_env = deployEnvironment
		appBuildResults = scriptForApps.build(build_env)
	}
}


if (deployEnvironment == 'ccs') {
	stage('Build App AMIs for TEST') {
		milestone(label: 'stage_build_app_amis_test_start')

		node {
			amiIds = scriptForDeploys.buildAppAmis('test', amiIds, appBuildResults)
		}
	}
}

stage('Deploy to TEST') {
	milestone(label: 'stage_deploy_test_start')

	node {
		gitBranchName = env.BRANCH_NAME
		gitCommitId = env.GIT_COMMIT
		scriptForDeploys.deploy('test', gitBranchName, gitCommitId, amiIds, appBuildResults)
	}

}

stage('Manual Approval') {
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
				input 'Deploy to production environments (prod, prod-stg, dpr, and hhsdevcloud)?'
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

stage('Deploy to hhsdevcloud') {
	if (willDeployToProdEnvs && params.deploy_hhsdevcloud) {
		lock(resource: 'env_hhsdevcloud', inversePrecendence: true) {
			milestone(label: 'stage_deploy_hhsdevcloud')

			node {
				scriptForDeploysHhsdevcloud.deploy(appBuildResults)
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to hhsdevcloud')
	}
}

if (deployEnvironment == 'ccs') {
	if (willDeployToProdEnvs) {
		stage('Build App AMIs for PROD-SBX') {
			milestone(label: 'stage_build_app_amis_prod-sbx_start')

			node {
				amiIds = scriptForDeploys.buildAppAmis('prod-stg', amiIds, appBuildResults)
			}
		}
	}
}

stage('Deploy to prod-stg') {
	if (willDeployToProdEnvs) {
		lock(resource: 'env_prod_stg', inversePrecendence: true) {
			milestone(label: 'stage_deploy_prod_stg_start')

			node {
				scriptForDeploys.deploy('prod-stg', amiIds, appBuildResults)
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod-stg')
	}
}

if (deployEnvironment == 'ccs') {
	if (willDeployToProdEnvs) {
		stage('Build App AMIs for PROD') {
			milestone(label: 'stage_build_app_amis_prod_start')

			node {
				amiIds = scriptForDeploys.buildAppAmis('prod', amiIds, appBuildResults)
			}
		}
	}
}

stage('Deploy to prod') {
	if (willDeployToProdEnvs && deployEnvironment != 'ccs') {
		lock(resource: 'env_prod', inversePrecendence: true) {
			milestone(label: 'stage_deploy_prod_start')

			node {
				scriptForDeploys.deploy('prod', amiIds, appBuildResults)
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to prod')
	}
}
