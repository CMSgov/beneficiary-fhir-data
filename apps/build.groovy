#!/usr/bin/env groovy

/**
 * <p>
 * This script will be run by Jenkins when building this repository. Specifically, calling
 * the <code>build()</code> method will build the Java applications: Data Pipeline, Data
 * Server, etc.
 * </p>
 */

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

		sh "mvn --update-snapshots -Dmaven.test.failure.ignore clean verify"

		// DEBUG
		sh "ls -la bfd-server/bfd-server-war/target/server-work"

		/*
		 * Fingerprint the output artifacts and archive the test results.
		 *
		 * Archiving the artifacts here would waste space, as the build deploys them to the local Maven repository.
		 */
		fingerprint '**/target/*.jar,**/target/*.war,**/target/*.zip'
		junit testResults: '**/target/*-reports/TEST-*.xml', keepLongStdio: true
		archiveArtifacts artifacts: '**/target/*.jar,**/target/*.war,**/target/*.zip,**/target/*-reports/*.txt,**/server-work/*.properties,**/server-work/*.txt,**/server-work/*.log,**/server-work/*.json', allowEmptyArchive: true
	}

	return new AppBuildResults(
		dataPipelineUberJar: 'apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar',
		dataServerLauncher: 'apps/bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT-capsule-fat.jar',
		dataServerWar: 'apps/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war'
	)
}

return this
