#!/usr/bin/env groovy

import groovy.json.JsonSlurper

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, it
 * contains methods that will handle deploying the applications to the environment, such
 * as {@link #deploy(String,String,BuildResult)}.
 * </p>
 */

@Library('bfd@bfd-2886-v2-build-docker-host')


/**
 * Models the results of a call to {@link #buildAppAmis}: contains the IDs of the AMIs that were built.
 */
class AmiIds implements Serializable {
	/**
	 * <p>
	 * The ID of the base "platinum" AMI to use for new instances/AMIs, or <code>null</code> if such an AMI does not yet exist.
	 * </p>
	 * <p>
	 * Why "platinum"? Because it's essentially a respin of the CCS environment's "gold master", with
	 * updates and additional tooling and configuration applied.
	 * </p>
	 */
	String platinumAmiId

	/**
	 * The ID of the AMI that will run the BFD Pipeline service, or <code>null</code> if such an AMI does not yet exist.
	 */
	String bfdPipelineAmiId

	/**
	 * The ID of the AMI that will run the BFD Server service, or <code>null</code> if such an AMI does not yet exist.
	 */
	String bfdServerAmiId

	/**
	 * The ID of the AMI that will run the BFD DB Migrator service, or <code>null</code> if such an AMI does not yet exist.
	 */
	String bfdMigratorAmiId

	/**
	 * The ID of the AMI that will run the bfd-server-load service's controller, or <code>null</code> if such an AMI does not yet exist.
	 */
	String bfdServerLoadAmiId

	/**
	 * The ID of the AMI that will run the docker host service, or <code>null</code> if such an AMI does not yet exist.
	 */
	String bfdDockerHostAmiId
}

/**
 * Finds the IDs of the latest BFD AMIs (if any) in the environment.
 *
 * @return a new {@link AmiIds} instance detailing the already-existing, latest AMIs (if any) that are now available for use
 * @throws RuntimeException An exception will be bubbled up if the AMI-builder tooling returns a non-zero exit code.
*/
def findAmis(String branchName = 'master') {
	// Replace this lookup either with a lookup in SSM or in a build artifact.
	return new AmiIds(
		platinumAmiId: awsEc2.getAmiId("", "bfd-amzn2-jdk17-platinum-??????????????"),
		bfdPipelineAmiId: awsEc2.getAmiId(branchName, "bfd-amzn2-jdk17-etl-??????????????"),
		bfdServerAmiId: awsEc2.getAmiId(branchName, "bfd-amzn2-jdk17-fhir-??????????????"),
		bfdMigratorAmiId: awsEc2.getAmiId(branchName, "bfd-amzn2-jdk17-db-migrator-??????????????"),
		bfdServerLoadAmiId:awsEc2.getAmiId(branchName, "server-load-??????????????"),
		bfdDockerHostAmiId: awsEc2.getAmiId(branchName, "docker-host-??????????????")
	)
}

/**
 * Builds the BFD Pipeline and BFD Server AMIs.
 *
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that already exist
 * @param appBuildResults the {@link AppBuildResults} containing the paths to the app binaries that were built
 * @return a new {@link AmiIds} instance detailing the shiny new AMIs that are now available for use
 * @throws RuntimeException An exception will be bubbled up if the AMI-builder tooling returns a non-zero exit code.
 */
def buildAppAmis(String gitBranchName, String gitCommitId, AmiIds amiIds, AppBuildResults appBuildResults) {
	amiIdsWrapper = new AmiIds();

	amis = [
		'data_server_launcher': "${workspace}/${appBuildResults.dataServerLauncher}",
		'data_server_war': "${workspace}/${appBuildResults.dataServerWar}",
		'data_pipeline_zip': "${workspace}/${appBuildResults.dataPipelineZip}",
		'db_migrator_zip': "${workspace}/${appBuildResults.dbMigratorZip}"
	]

	dir('ops/ansible/playbooks-ccs'){

		writeJSON file: "${workspace}/ops/ansible/playbooks-ccs/extra_vars.json", json: amis

		withCredentials([file(credentialsId: 'bfd-vault-password', variable: 'vaultPasswordFile')]) {
			packerBuildAmis(amiIds.platinumAmiId, gitBranchName, gitCommitId,
					"../../packer/build_bfd-all.json", ["vault_password_file": "$vaultPasswordFile"] as Map)

			amiIdsWrapper.platinumAmiId = amiIds.platinumAmiId
			amiIdsWrapper.bfdPipelineAmiId = extractAmiIdFromPackerManifest(readFile(
						file: "${workspace}/ops/ansible/playbooks-ccs/manifest_data-pipeline.json"))
			amiIdsWrapper.bfdServerAmiId = extractAmiIdFromPackerManifest(readFile(
						file: "${workspace}/ops/ansible/playbooks-ccs/manifest_data-server.json"))
			amiIdsWrapper.bfdMigratorAmiId = extractAmiIdFromPackerManifest(readFile(
						file: "${workspace}/ops/ansible/playbooks-ccs/manifest_db-migrator.json"))
			amiIdsWrapper.bfdServerLoadAmiId = extractAmiIdFromPackerManifest(readFile(
						file: "${workspace}/ops/ansible/playbooks-ccs/manifest_server-load.json"))
		}
	}

	return amiIdsWrapper
}

def buildDockerHostAmi(String gitBranchName, String gitCommitId, String platinumAmiId) {
	dir('ops/packer'){
		packerBuildAmis(platinumAmiId, gitBranchName, gitCommitId, "./build_bfd-docker-host.json", [] as Map)
		return extractAmiIdFromPackerManifest(readFile(file: "${workspace}/ops/packer/manifest_docker-host.json"))
	}
}

def packerBuildAmis(String platinumAmiId, String gitBranchName, String gitCommitId, String templateFile,
   		Map additionalVariables) {

	variableOpts = ""
	additionalVariables.each { entry -> variableOpts = variableOpts + "-var $entry.key=$entry.value " }

	withEnv(["platinumAmiId=${platinumAmiId}", "gitBranchName=${gitBranchName}",
			 "gitCommitId=${gitCommitId}", "templateFile=${templateFile}"]) {
		// build ami
		sh '''
packer build -color=false \
-var source_ami="$platinumAmiId" \
-var subnet_id=subnet-092c2a68bd18b34d1 \
-var git_branch="$gitBranchName" \
-var git_commit="$gitCommitId" \
"$variableOpts" \
"$templateFile"
'''
	}
}

/**
 * Deploys to the specified environment.
 *
 * @param environmentId the ID of the environment to deploy to
 * @param gitBranchName the name of the Git branch this build is for
 * @param gitCommitId the hash/ID of the Git commit that this build is for
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that should be used
 * @throws RuntimeException An exception will be bubbled up if the deploy tooling returns a non-zero exit code.
 */
def deploy(String environmentId, String gitBranchName, String gitCommitId, AmiIds amiIds) {

	dir("${workspace}/ops/terraform/env/${environmentId}/stateless") {
		// Debug output terraform version
		sh "terraform --version"

		// Initilize terraform
		sh "terraform init -no-color"

		// Gathering terraform plan
		echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
		sh "terraform plan \
		-var='fhir_ami=${amiIds.bfdServerAmiId}' \
		-var='ssh_key_name=bfd-${environmentId}' \
		-var='git_branch_name=${gitBranchName}' \
		-var='git_commit_id=${gitCommitId}' \
		-no-color -out=tfplan"

		// Apply Terraform plan
		echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
		sh "terraform apply \
		-no-color -input=false tfplan"
		echo "Timestamp: ${java.time.LocalDateTime.now().toString()}"
	}
}

def extractAmiIdFromPackerManifest(String manifest) {
	def manifestJson = new JsonSlurper().parseText(manifest)
	// artifactId will be of the form $region:$amiId
	return manifestJson.builds[manifestJson.builds.size() - 1].artifact_id.split(":")[1]
}

return this
