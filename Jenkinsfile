node {
	stage 'Checkout'
	checkout scm
	
	
	stage 'Build'
	
	// This tool must be setup and named correctly in the Jenkins config.
	def mvnHome = tool 'maven-3'
	
	// Run the build, using Maven.
	sh "${mvnHome}/bin/mvn -Dmaven.test.failure.ignore clean install"
	
	
	//stage 'Archive'
	//step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
	step([$class: 'ArtifactArchiver', artifacts: '**/target/*.war', fingerprint: true])
	//step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
	//step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml'])

	
	stage 'Trigger Downstream'
	//build job: '../some-other-project/master', wait: false
}
