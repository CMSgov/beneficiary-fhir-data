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
	
	
	stage 'Trigger Downstream'
	//build job: '../some-other-project/master', wait: false
}
