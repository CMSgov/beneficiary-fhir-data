node {
	stage 'Checkout'
	checkout scm
	
	// This tool must be setup and named correctly in the Jenkins config.
	def mvnHome = tool 'maven-3'
	
	// Calculate the current POM version.
	// Reference: http://stackoverflow.com/a/26514030/1851299
	sh "${mvnHome}/bin/mvn --quiet --non-recursive -Dexec.executable='echo' -Dexec.args='\${project.version}' org.codehaus.mojo:exec-maven-plugin:1.3.1:exec > pom.project.version.txt"
	pomProjectVersion = readFile('pom.project.version.txt').trim()
	echo "Current POM version: ${pomProjectVersion}"
	
	// Calculate the build ID.
	gitBranchName = "${env.BRANCH_NAME}".toString()
	echo "Branch name: ${gitBranchName}"
	def buildId = ""
	if("master".equals(gitBranchName)) {
		buildId = "${env.BUILD_NUMBER}"
	} else {
		buildId = "${gitBranchName}-${env.BUILD_NUMBER}"
	}
	echo "Build ID: ${buildId}"
	
	// Update the POM version to include the build ID.
	// Reference: https://maven.apache.org/maven-release/maven-release-plugin/examples/update-versions.html
	def pomProjectVersionWithBuildId = pomProjectVersion.replaceAll("SNAPSHOT", buildId)
	echo "Updated POM version: ${pomProjectVersionWithBuildId}"
	sh "${mvnHome}/bin/mvn --batch-mode --quiet org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion='${pomProjectVersionWithBuildId}' -DgenerateBackupPoms=false"
	
	stage 'Build'
	
	// Create the settings file for Maven (contains deploy credentials). Will
	// be automatically cleaned up at end of block.
	wrap([$class: 'ConfigFileBuildWrapper', 
		managedFiles: [[fileId: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig:cms-bluebutton-settings-xml', 
		variable: 'SETTINGS_PATH']]
	]) {
		// Run the build, using Maven.
		sh "${mvnHome}/bin/mvn --settings ${env.SETTINGS_PATH} -Dmaven.test.failure.ignore clean deploy scm:tag"
	}
	
	
	stage 'Archive'
	//step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
	step([$class: 'ArtifactArchiver', artifacts: '**/target/*.war', fingerprint: true])
	//step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
	step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml'])
	
	
	stage 'Trigger Downstream'
	//build job: '../some-other-project/master', wait: false
}
