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

		def ansibleRunner = docker.build('ansible-runner', './dockerfiles/ansible-runner')
		ansibleRunner.inside {
			sh 'echo "Hello World!"'
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
