#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, it
 * contains methods that will handle deploying the applications to the environment, such
 * as {@link #deploy(String,String,BuildResult)}.
 * </p>
 */


/**
 * Not used in the HealthAPT environment; just stubbed out to prevent errors.
 */
class AmiIds implements Serializable {
}

/*
 * Note: due to JENKINS-33510, the 'dir' step is not yet supported inside Docker. As a workaround, all 'sh'
 * steps inside docker blocks should be prefixed with "cd <work_dir> && ...", if a different working
 * directory is needed.
 */
@groovy.transform.Field
def deployWorkingDir = 'ops/ansible/playbooks-healthapt'

/**
 * Deploys/redeploys Jenkins and related systems to the LSS environment.
 *
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that should be used
 * @throws RuntimeException An exception will be bubbled up if the deploy tooling returns a non-zero exit code.
 */
def deployManagement(AmiIds amiIds) {
	echo('Deploying to the HealthAPT LSS environment is not implemented yet.')
}

/**
 * Deploys to the specified environment.
 *
 * @param envId the ID of the environment to deploy to
 * @param gitBranchName the name of the Git branch this build is for
 * @param gitCommitId the hash/ID of the Git commit that this build is for
 * @param amiIds an {@link AmiIds} instance detailing the IDs of the AMIs that should be used
 * @param appBuildResults the {@link AppBuildResults} containing the paths to the app binaries that were built
 * @throws RuntimeException An exception will be bubbled up if the deploy tooling returns a non-zero exit code.
 */
def deploy(String envId, String gitBranchName, String gitCommitId, AmiIds amiIds, AppBuildResults appBuildResults) {

	// Ensure the Ansible image is ready to go.
	insideAnsibleContainer {
		// Just some general "what does the container look like" debug
		// logging. Useful for if/when things go sideways.
		sh 'cat /etc/passwd'
		sh 'echo $USER && echo $UID && echo $HOME && whoami'
		sh 'pwd && ls -la'
		sh 'ls -la ops/ansible/roles'
		sh 'ansible --version'

		// Verify the play's syntax before we run it.
		sh "cd ${deployWorkingDir} && ./ansible-playbook-wrapper backend.yml --inventory=hosts_test --syntax-check"
	}

	// Define env-specific variables.
	def envGroupName;
	def envLimitName;
	def extraVarsText;
	if (envId == "test" ) {
		envGroupName = 'env_test'
		envLimitName = 'ts'
	} else if (envId == "prod-stg") {
		envGroupName = 'env_dpr'
		envLimitName = 'dp'
	} else if (envId == "prod") {
		envGroupName = 'env_prod'
		envLimitName = 'pd'
	} else {
		throw new IllegalArgumentException("Unsupported environment ID: '${envId}'.")
	}

	// Run the deploy to the specified environment.
	insideAnsibleContainer {
		writeFile file: "${deployWorkingDir}/extra_vars.json", encoding: 'UTF-8', text: """\
		{
			"limit_envs": [
				"${envLimitName}"
			],
			"data_pipeline_jar": "../../../${appBuildResults.dataPipelineUberJar}",
			"data_server_launcher": "../../../${appBuildResults.dataServerLauncher}",
			"data_server_war": "../../../${appBuildResults.dataServerWar}"
		}
		""".stripIndent()
		sh "cd ${deployWorkingDir} && ./ansible-playbook-wrapper backend.yml --limit=localhost:${envGroupName} --extra-vars \"@extra_vars.json\""
	}
}

/**
 * @return the result returned from running the specified `Closure` inside the
 *         `ansible_runner` container
 */
public <V> V insideAnsibleContainer(Closure<V> body) {
	/*
	 * Prepare the Ansible execution environment. We use Docker for this, as the
	 * Jenkins system is in FIPS mode, which prevents virtualenv, pip, and
	 * Ansible from working (different symptoms, but generally they need MD5).
	 *
	 * Jenkins Docker references:
	 * * <https://builds.ls.r53.cmsfhir.systems/jenkins/job/bluebutton-ansible-playbooks-data/pipeline-syntax/globals#docker>
	 * * <https://github.com/jenkinsci/docker-workflow-plugin/blob/master/src/main/resources/org/jenkinsci/plugins/docker/workflow/Docker.groovy>
	 */

	// FIXME: Get the Docker image building in Jenkins/LSS. Until then, we're
	// using an image Karl built locally and then exported-imported to the
	// Jenkins system.
	//def ansibleRunner = docker.build('ansible_runner', './dockerfiles/ansible_runner')
	def ansibleRunner = docker.image('ansible_runner')

	// Ensure that the Ansible container can open the Ansible Vault files
	// for this project.
	withCredentials([file(credentialsId: 'bluebutton-ansible-playbooks-data-ansible-vault-password', variable: 'vaultPasswordFile')]) {

		// Run the container as root, not as the random unbound user that
		// Jenkins defaults to (as that will cause Python/Ansible errors).
		def dockerArgs = '--user root:root'

		// Ensure that Ansible uses Jenkins' SSH config and keys.
		dockerArgs += ' --volume=/var/lib/jenkins/.ssh:/root/.ssh_jenkins:rw'
		dockerArgs += ' --volume=/etc/ssh:/etc/ssh:rw'

		// Bind mount the `vault.password` file where it's needed.
		dockerArgs += " --volume=${vaultPasswordFile}:${env.WORKSPACE}/ops/ansible/playbooks-healthapt/vault.password:ro"

		// Ensure that Ansible uses Jenkins' Maven repo.
		dockerArgs += ' --volume=/u01/jenkins/.m2:/root/.m2:ro'

		// Prepend the specified closure with some needed in-container setup.
		def bodyWithSetup = {
			// Copy the SSH config and keys and fix permissions.
			sh 'cp --recursive --no-target-directory /root/.ssh_jenkins /root/.ssh'
			sh 'chmod -R u=rw,g=,o= /root/.ssh'

			// Link the project's Ansible roles to where they're expected.
			sh "cd ${deployWorkingDir} && ln -s /etc/ansible/roles roles_external"
			body.call()
		}

		// Now start the container with the above args and run the specified
		// closure in it, returning the result from that.
		try {
			return ansibleRunner.inside(dockerArgs, bodyWithSetup)
		} finally {
			// We're running the containerized process as root, so it's going to leave
			// around files owned by root. That's not great anyways, but it gets
			// particularly annoying because Jenkins can't move/cleanup those files
			// when it needs to later. So: we fix the ownership here and problem solved.
			def jenkinsUid = sh(script: 'id --user', returnStdout: true).trim()
			def jenkinsGid = sh(script: 'id --group', returnStdout: true).trim()
			ansibleRunner.inside(dockerArgs, {
				sh "find . -writable -exec chown ${jenkinsUid}:${jenkinsGid} '{}' \\;"
			})
		}
	}
}

return this
