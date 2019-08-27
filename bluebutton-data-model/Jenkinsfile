#!/usr/bin/env groovy

/**
 * <p>
 * This is the script that will be run by Jenkins to build and test this 
 * project. This drives the project's continuous integration and delivery.
 * </p>
 * <p>
 * This script is run by Jenkins' Pipeline feature. A good intro tutorial for
 * working with Jenkins Pipelines can be found here:
 * <a href="https://jenkins.io/doc/book/pipeline/">Jenkins > Pipeline</a>.
 * </p>
 * <p>
 * The canonical Jenkins server job for this project is located here: 
 * <a href="https://builds.ls.r53.cmsfhir.systems/jenkins/job/bluebutton-data-model">bluebutton-data-model</a>.
 * </p>
 */

properties([
	pipelineTriggers([
		triggers: [[
			$class: 'jenkins.triggers.ReverseBuildTrigger',
			upstreamProjects: "bluebutton-parent-pom/master", threshold: hudson.model.Result.SUCCESS
		]]
	]),
	buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: ''))
])

stage('Checkout') {
	node {
		// Grab the commit that triggered the build.
		checkout scm

		// Update the POM version so that the artifacts produced by this build
		// are distinguishable from other builds.
		setPomVersionUsingBuildId()
	}
}

stage('Build') {
	node {
		milestone(label: 'stage_build_start')

		mvn "--update-snapshots -Dmaven.test.failure.ignore clean install"
	}
}

stage('Archive') {
	node {
		// Fingerprint the output artifacts and archive the test results.
		// (Archiving the artifacts here would waste space, as the build
		// deploys them to the local Maven repository.)
		fingerprint '**/target/*.jar'
		junit testResults: '**/target/*-reports/TEST-*.xml', keepLongStdio: true
		archiveArtifacts artifacts: '**/target/*-reports/*.txt', allowEmptyArchive: true
	}
}


/**
 * Runs Maven with the specified arguments.
 *
 * @param args the arguments to pass to <code>mvn</code>
 */
def mvn(args) {
	// This tool must be setup and named correctly in the Jenkins config.
	def mvnHome = tool 'maven-3'

	// Run the build, using Maven, with the appropriate config.
	configFileProvider(
			[
				configFile(fileId: 'bluebutton:settings.xml', variable: 'MAVEN_SETTINGS'),
				configFile(fileId: 'bluebutton:toolchains.xml', variable: 'MAVEN_TOOLCHAINS')
			]
	) {
		sh "${mvnHome}/bin/mvn --settings $MAVEN_SETTINGS --toolchains $MAVEN_TOOLCHAINS ${args}"
	}
}

/**
 * @return the <version /> from the POM of the project in the current working
 *         directory.
 */
def readPomVersion() {
	// Reference: http://stackoverflow.com/a/26514030/1851299
	mvn "--quiet --non-recursive -Dexec.executable='echo' -Dexec.args='\${project.version}' org.codehaus.mojo:exec-maven-plugin:1.3.1:exec > pom.project.version.txt"
	pomProjectVersion = readFile('pom.project.version.txt').trim()
	sh "rm -f pom.project.version.txt"

	echo "Current POM version: ${pomProjectVersion}"
	return pomProjectVersion
}

/**
 * @return an ID for the current build, in the form of
 *         "<code>${env.BRANCH_NAME}-${env.BUILD_NUMBER}</code>"
 */
def calculateBuildId() {
	gitBranchName = "${env.BRANCH_NAME}".toString()
	gitCommitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

	buildId = "${gitBranchName}-${gitCommitId}"

	echo "Build ID: ${buildId} (for branch ${gitBranchName})"
	return buildId
}

/**
 * <p>
 * The same artifact repository will store builds produced from all source code
 * branches. Unfortunately, Maven's versioning scheme doesn't really account
 * for branches: unless the version numbers are manually changed for each
 * branch (and there isn't any graceful scheme that would accommodate that),
 * the "latest <code>-SNAPSHOT</code>" artifact included in builds could be
 * from any branch.
 * </p>
 * <p>
 * This method is used to workaround that problem. For non-<code>master</code>
 * branch builds, the "<code>-SNAPSHOT</code> version qualifier is replaced
 * with "<code>-&lt;branchName&gt;-&lt;commitSha1&gt;</code>, ensuring that
 * those builds don't "pollute" the <code>master</code> branch's artifact
 * history. Version numbers for <code>master</code> branch builds are left
 * alone.
 * </p>
 */
def setPomVersionUsingBuildId() {
	gitBranchName = "${env.BRANCH_NAME}".toString()
	if (!"master".equals(gitBranchName)) {
		pomProjectVersionWithBuildId = readPomVersion().replaceAll("SNAPSHOT", calculateBuildId())

		// Update the POM version to include the build ID.
		// Reference: https://maven.apache.org/maven-release/maven-release-plugin/examples/update-versions.html
		mvn "--batch-mode --quiet org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion='${pomProjectVersionWithBuildId}' -DgenerateBackupPoms=false"
		echo "Updated POM version: ${pomProjectVersionWithBuildId}"
	}
}
