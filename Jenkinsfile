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

node {
	stage('Prepare') {
		// Grab the commit that triggered the build.
		checkout scm

		// Ensure the Ansible image is ready to go.
		insideAnsibleContainer {
			sh 'ln -s /etc/ansible/roles roles_external'
			sh 'cat /etc/passwd'
			sh 'echo $USER && echo $UID && whoami'
			sh 'pwd && ls -la'
			sh 'ansible --version'
			sh './ansible-playbook-wrapper backend.yml --inventory=hosts_test --syntax-check'
		}
	}

	stage('Deploy to Test') {
		insideAnsibleContainer {
			sh 'ln -s /etc/ansible/roles roles_external'
			sh 'pwd && ls -la'
			sh './ansible-playbook-wrapper backend.yml --inventory=hosts_test --extra-vars "data_pipeline_version=0.1.0-SNAPSHOT data_server_version=1.0.0-SNAPSHOT"'
		}
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
		def dockerArgs = '-u root:root'

		// Ensure that Ansible uses Jenkins' SSH config and keys.
		dockerArgs += ' --volume=/var/lib/jenkins/.ssh:/root/.ssh:ro'

		// Bind mount the `vault.password` file where it's needed.
		dockerArgs += " --volume=${vaultPasswordFile}:${env.WORKSPACE}/vault.password:ro"

		// Now start the container with the above args and run the specified
		// closure in it, returning the result from that.
		return ansibleRunner.inside(dockerArgs, body)
	}
}
