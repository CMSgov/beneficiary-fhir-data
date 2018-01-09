node {
	stage 'Checkout'
	checkout scm
	
	
	stage 'Build'
	
	// This tool must be setup and named correctly in the Jenkins config.
	def mvnHome = tool 'maven-3'
	
	// Run the build, using Maven.
	configFileProvider(
			[
				configFile(fileId: 'bluebutton:settings.xml', variable: 'MAVEN_SETTINGS'),
				configFile(fileId: 'bluebutton:toolchains.xml', variable: 'MAVEN_TOOLCHAINS')
			]
	) {
		sh "${mvnHome}/bin/mvn --settings $MAVEN_SETTINGS --toolchains $MAVEN_TOOLCHAINS clean install"
	}
	
	
	//stage 'Archive'
	//step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
	
	
	stage 'Trigger Downstream'
	//build job: '../some-other-project/master', wait: false
}
