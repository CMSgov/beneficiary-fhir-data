#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, calling
 * the <code>build()</code> method will build the Java applications: Data Pipeline, Data
 * Server, etc.
 * </p>
 */


/**
 * Runs Maven with the specified arguments.
 *
 * @param args the arguments to pass to <code>mvn</code>
 * @throws RuntimeException An exception will be bubbled up if the Maven build returns a non-zero exit code.
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
 * Models the results of a call to {@link #build}: contains the paths to the artifacts that were built.
 */
class AppBuildResults implements Serializable {
	String dataPipelineUberJar
	String dataServerLauncher
	String dataServerWar
}

/**
 * Builds the Java applications and utilities in this directory: Data Pipeline, Data Server, etc.
 *
 * @return An {@link AppBuildResults} instance containing the paths to the artifacts that were built.
 * @throws Exception An exception will be bubbled up if the Maven build fails.
 */
 
def build(String build_env) {
	dir ('apps') {

		mvn "--update-snapshots -Dmaven.test.failure.ignore clean verify"

	
		/*
		 * Fingerprint the output artifacts and archive the test results.
		 *
		 * Archiving the artifacts here would waste space, as the build deploys them to the local Maven repository.
		 */
		fingerprint '**/target/*.jar,**/target/*.war,**/target/*.zip'
		junit testResults: '**/target/*-reports/TEST-*.xml', keepLongStdio: true
		archiveArtifacts artifacts: '**/target/*.jar,**/target/*.war,**/target/*.zip,**/target/*-reports/*.txt', allowEmptyArchive: true
	}

	return new AppBuildResults(
		dataPipelineUberJar: 'apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar',
		dataServerLauncher: 'apps/bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT-capsule-fat.jar',
		dataServerWar: 'apps/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war'
	)
}

return this
