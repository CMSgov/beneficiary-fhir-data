/*
 * <p>
 * This is the script that will be run by Jenkins to build and test this 
 * project. This drives the project's continuous integration, delivery, and
 * deployment.
 * </p>
 * <p>
 * The canonical Jenkins server job for this project is located here: 
 * <a href="http://builds.hhsdevcloud.us/job/HHSIDEAlab/job/bbonfhir-server/">bbonfhir-server</a>.
 * </p>
 */

node {
	stage 'Checkout'
		checkout scm
		setPomVersionUsingBuildId()
	
	stage 'Build'
		/* Create the settings file for Maven (contains deploy credentials). 
		 * Will be automatically cleaned up at end of block.
		 */
		wrap([$class: 'ConfigFileBuildWrapper', 
			managedFiles: [[fileId: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig:cms-bluebutton-settings-xml', 
			variable: 'SETTINGS_PATH']]
		]) {
			// Run the build.
			mvn "--settings ${env.SETTINGS_PATH} -Dmaven.test.failure.ignore clean deploy scm:tag"
		}
	
	stage 'Archive'
		step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true, allowEmptyArchive: true])
		step([$class: 'ArtifactArchiver', artifacts: '**/target/*.war', fingerprint: true, allowEmptyArchive: true])
		step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true])
		step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml', allowEmptyResults: true])
		
	stage 'Trigger Downstream'
		//build job: '../some-other-project/master', wait: false
}

/**
 * Runs Maven with the specified arguments.
 * 
 * @param args the arguments to pass to <code>mvn</code>
 */
def mvn(args) {
	// This tool must be setup and named correctly in the Jenkins config.
    sh "${tool 'maven-3'}/bin/mvn ${args}"
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
	// Calculate the build ID.
	gitBranchName = "${env.BRANCH_NAME}".toString()
	buildId = ""
	if("master".equals(gitBranchName)) {
		buildId = "${env.BUILD_NUMBER}"
	} else {
		buildId = "${gitBranchName}-${env.BUILD_NUMBER}"
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
