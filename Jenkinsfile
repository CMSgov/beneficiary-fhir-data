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

		/*
		 * Prepare the Ansible execution environment. We use Docker for this, as the
		 * Jenkins system is in FIPS mode, which prevents virtualenv, pip, and
		 * Ansible from working (different symptoms, but generally they need MD5).
		 */

		// First, prepare the Ansible Docker image.
		// FIXME: Get the Docker image building in Jenkins/LSS. Until then, we're
		// using an image Karl built locally and then exported-imported to the
		// Jenkins system.
		//def ansibleRunner = docker.build('ansible_runner', './dockerfiles/ansible_runner')
		def ansibleRunner = docker.image('ansible_runner')

		// Ensure the Ansible image is ready to go.
		ansibleRunner.inside('--volume=/var/lib/jenkins/.ssh:/root/.ssh:ro') {
			sh 'pwd && ls -la'
			sh 'ansible --version'
		}
	}

//	stage('Prepare Tooling') {
//		withPythonEnv('/usr/bin/python2.7') {
//			pysh "pip install --upgrade setuptools"
//			pysh "pip install --requirement requirements.txt"
//		}
//	}
//
//	stage('Check Ansible Syntax') {
//		withPythonEnv('/usr/bin/python2.7') {
//			pysh "ansible-playbook backend.yml --inventory=hosts_development --syntax-check"
//		}
//	}
}
