/**
 * <p>
 * This is the script that will be run by Jenkins to build and test this 
 * project. This drives the project's continuous integration, delivery, and
 * deployment.
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

node {
	stage('Checkout') {
		// Grab the commit that triggered the build.
		checkout scm

		// Update the POM version so that the artifacts produced by this build
		// are distinguishable from other builds.
		setPomVersionUsingBuildId()
	}

	stage('Build') {
		mvn "--update-snapshots -Dmaven.test.failure.ignore clean deploy"
	}

	stage('Archive') {
		// Fingerprint the output artifacts and archive the test results.
		// (Archiving the artifacts here would waste space, as the build
		// deploys them to the artifact repository.)
		fingerprint '**/target/*.jar'
		archiveArtifacts artifacts: '**/target/*-reports/TEST-*.xml', allowEmptyArchive: true, fingerprint: true
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
 *         "<code>${env.BUILD_NUMBER}</code>" for builds against the SCM's 
 *         <code>master</code> branch, and 
 *         "<code>${env.BRANCH_NAME}-${env.BUILD_NUMBER}</code>" for other branches
 */
def calculateBuildId() {
	gitBranchName = "${env.BRANCH_NAME}".toString()
	gitCommitId = sh(script: "git rev-parse HEAD", returnStdout: true).trim()

	buildId = ""
	if("master".equals(gitBranchName)) {
		buildId = "${gitCommitId}"
	} else {
		buildId = "${gitBranchName}-${gitCommitId}"
	}

	echo "Build ID: ${buildId} (for branch ${gitBranchName})"
	return buildId
}

/**
 * <p>
 * Uses <code>org.codehaus.mojo:versions-maven-plugin</code> to set the version
 * of the POM in the current working directory, such that it replaces 
 * "<code>SNAPSHOT</code>" with the value of {@link #calculateBuildId()}.
 * </p>
 * <p>
 * This ensures that every CI build has a continuous-deployment-friendly, 
 * unique version number.
 * </p>
 */
def setPomVersionUsingBuildId() {
	// Update the POM version to include the build ID.
	// Reference: https://maven.apache.org/maven-release/maven-release-plugin/examples/update-versions.html
	pomProjectVersionWithBuildId = readPomVersion().replaceAll("SNAPSHOT", calculateBuildId())
	mvn "--batch-mode --quiet org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion='${pomProjectVersionWithBuildId}' -DgenerateBackupPoms=false"
	echo "Updated POM version: ${pomProjectVersionWithBuildId}"
}