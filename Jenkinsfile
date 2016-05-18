node {
	stage 'Checkout'
	checkout scm
	
	// Calculate the build ID.
	sh "git branch | sed -n -e 's/^\\* \\(.*\\)/\\1/p' | tee git-branch-name.txt"
	gitBranchName = readFile('git-branch-name.txt').trim()
	def buildId = ""
	if("master".equals(gitBranchName)) {
		buildId = "${env.BUILD_NUMBER}"
	} else {
		buildId = "${gitBranchName}-${env.BUILD_NUMBER}"
	}
	
	stage 'Build'
	
	// This tool must be setup and named correctly in the Jenkins config.
	def mvnHome = tool 'maven-3'
	
	// Run the build, using Maven.
	sh "${mvnHome}/bin/mvn -Dmaven.test.failure.ignore clean install -DbuildId=${buildId}"
	
	
	//stage 'Archive'
	//step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
	step([$class: 'ArtifactArchiver', artifacts: '**/target/*.war', fingerprint: true])
	//step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
	step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml'])
	
	
	stage 'Trigger Downstream'
	//build job: '../some-other-project/master', wait: false
}
