#!/usr/bin/env groovy

/**
 * <p>
 * This is the script that will be run by Jenkins to deploy the Blue Button API
 * data/backend systems. This drives our project's continuous deployment. It
 * uses the Ansible plays in this project.
 * </p>
 * <p>
 * This script is run by Jenkins' Pipeline feature. A good intro tutorial for
 * working with Jenkins Pipelines can be found here:
 * <a href="https://jenkins.io/doc/book/pipeline/">Jenkins > Pipeline</a>.
 * </p>
 * <p>
 * The canonical Jenkins server job for this project is located here:
 * <a href="https://builds.ls.r53.cmsfhir.systems/jenkins/job/bluebutton-ansible-playbooks-data">bluebutton-ansible-playbooks-data</a>.
 * </p>
 */

properties([
	pipelineTriggers([
		triggers: [[
			$class: 'jenkins.triggers.ReverseBuildTrigger',
			upstreamProjects: "bluebutton-data-server/master,bluebutton-data-pipeline/master", threshold: hudson.model.Result.SUCCESS
		]]
	]),
	parameters([
		booleanParam(name: 'deploy_from_non_master', description: 'Whether to run the Ansible plays for builds of this project\'s non-master branches.', defaultValue: false),
		booleanParam(name: 'bootstrap_jenkins', description: 'Whether to run the Ansible plays to bootstrap some pre-req Jenkins config.', defaultValue: false),
		booleanParam(name: 'deploy_to_lss', description: 'Whether to run the Ansible plays for LSS systems (e.g. Jenkins itself).', defaultValue: false),
		booleanParam(name: 'deploy_to_prod', description: 'Whether to run the Ansible plays for PROD systems (without prompting first, which is the default behavior).', defaultValue: false)
	]),
	buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: ''))
])

stage('Prepare') {
	node {
		// Grab the commit that triggered the build.
		checkout scm

		// Ensure the Ansible image is ready to go.
		insideAnsibleContainer {
			// Just some general "what does the container look like" debug
			// logging. Useful for if/when things go sideways.
			sh 'cat /etc/passwd'
			sh 'echo $USER && echo $UID && echo $HOME && whoami'
			sh 'pwd && ls -la'
			sh 'ansible --version'

			// Verify the play's syntax before we run it.
			sh './ansible-playbook-wrapper backend.yml --inventory=hosts_test --syntax-check'
		}
	}
}

def shouldDeploy = params.deploy_from_non_master || env.BRANCH_NAME == "master"

def dataPipelineVersion = '0.1.0-SNAPSHOT'
def dataServerVersion = '1.0.0-SNAPSHOT'

stage('Bootstrap Jenkins') {
	if (shouldDeploy && params.bootstrap_jenkins) {
		lock(resource: 'env_lss', inversePrecendence: true) {
			milestone(label: 'stage_bootstrap_start')

			node {
				def jenkinsUid = sh(script: 'id --user', returnStdout: true).trim()
				def jenkinsGid = sh(script: 'id --group', returnStdout: true).trim()
				insideAnsibleContainer {
					/*
					 * Bootstrap this system: SSH known_hosts, config, etc. Note
					 * that the `.ssh/config` path is customized to ensure that the
					 * 'real' version of the file for the Jenkin's user is created/
					 * updated, rather than the copy of it that is used inside the
					 * Docker container. (If the file is created/modified here, you
					 * will likely the job a second time after it goes boom.)
					 */
					sh "./ansible-playbook-wrapper bootstrap.yml --extra-vars 'ssh_config_dest=/root/.ssh_jenkins/config ssh_config_uid=${jenkinsUid} ssh_config_gid=${jenkinsGid}'"
				}
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Bootstrap Jenkins')
	}
}

stage('Deploy to LSS') {
	if (shouldDeploy && params.deploy_to_lss) {
		lock(resource: 'env_lss', inversePrecendence: true) {
			milestone(label: 'stage_deploy_lss_start')

			node {
				insideAnsibleContainer {
					// Run the play against the LSS environment.
					writeFile file: 'extra_vars.json', encoding: 'UTF-8', text: """\
					{
						"limit_envs": [
							"ls"
						]
					}
					""".stripIndent()
					sh './ansible-playbook-wrapper backend.yml --limit=localhost:env_lss --extra-vars "@extra_vars.json"'
				}
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to LSS')
	}
}

stage('Deploy to TEST') {
	if (shouldDeploy) {
		lock(resource: 'env_test', inversePrecendence: true) {
			milestone(label: 'stage_deploy_test_start')

			node {
				insideAnsibleContainer {
					// Run the play against the test environment.
					writeFile file: 'extra_vars.json', encoding: 'UTF-8', text: """\
					{
						"limit_envs": [
							"ts"
						],
						"data_pipeline_version": "${dataPipelineVersion}",
						"data_server_version": "${dataServerVersion}"
					}
					""".stripIndent()
					sh './ansible-playbook-wrapper backend.yml --limit=localhost:env_test --extra-vars "@extra_vars.json"'
				}
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to TEST')
	}
}

stage('Manual Approval') {
	if (shouldDeploy) {
		/*
		 * Unless it was explicitly requested at the start of the build, prompt for confirmation before
		 * deploying to production environments.
		 */
		if (!params.deploy_to_prod) {
			/*
			 * The Jenkins UI will prompt with "Proceed" and "Abort" options. If "Proceed" is
			 * chosen, this build will continue merrily on as normal. If "Abort" is chosen,
			 * the build will be aborted.
			 */
			input 'Deploy to PROD?'
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Manual Approval')
	}
}

stage('Deploy to DPR') {
	if (shouldDeploy) {
		lock(resource: 'env_dpr', inversePrecendence: true) {
			milestone(label: 'stage_deploy_dpr_start')

			node {
				insideAnsibleContainer {
					// Run the play against the prod environment.
					writeFile file: 'extra_vars.json', encoding: 'UTF-8', text: """\
					{
						"limit_envs": [
							"dp"
						],
						"data_pipeline_version": "${dataPipelineVersion}",
						"data_server_version": "${dataServerVersion}"
					}
					""".stripIndent()
					sh './ansible-playbook-wrapper backend.yml --limit=localhost:env_dpr --extra-vars "@extra_vars.json"'
				}
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to DPR')
	}
}

stage('Deploy to PROD') {
	if (shouldDeploy) {
		lock(resource: 'env_prod', inversePrecendence: true) {
			milestone(label: 'stage_deploy_prod_start')

			node {
				insideAnsibleContainer {
					// Run the play against the prod environment.
					writeFile file: 'extra_vars.json', encoding: 'UTF-8', text: """\
					{
						"limit_envs": [
							"pd"
						],
						"data_pipeline_version": "${dataPipelineVersion}",
						"data_server_version": "${dataServerVersion}"
					}
					""".stripIndent()
					sh './ansible-playbook-wrapper backend.yml --limit=localhost:env_prod --extra-vars "@extra_vars.json"'
				}
			}
		}
	} else {
		org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional('Deploy to PROD')
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
		dockerArgs += " --volume=${vaultPasswordFile}:${env.WORKSPACE}/vault.password:ro"

		// Ensure that Ansible uses Jenkins' Maven repo.
		dockerArgs += ' --volume=/u01/jenkins/.m2:/root/.m2:ro'

		// Prepend the specified closure with some needed in-container setup.
		def bodyWithSetup = {
			// Copy the SSH config and keys and fix permissions.
			sh 'cp --recursive --no-target-directory /root/.ssh_jenkins /root/.ssh'
			sh 'chmod -R u=rw,g=,o= /root/.ssh'

			// Link the project's Ansible roles to where they're expected.
			sh 'ln -s /etc/ansible/roles roles_external'
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
