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
	String dbMigratorZip
	String dataPipelineZip
	String dataServerLauncher
	String dataServerWar
}

/**
 * Builds the Java applications and utilities in this directory: Data Pipeline, Data Server, etc.
 *
 * @param verboseMaven when `false`, maven runs with `--quiet` and `--batch-mode` flags
 * @return An {@link AppBuildResults} instance containing the paths to the artifacts that were built.
 * @throws Exception An exception will be bubbled up if the Maven build fails.
 */
def build(boolean verboseMaven) {
	dir ('apps') {
		quietFlags = verboseMaven ? '' : '--quiet --batch-mode'

		sh "mvn ${quietFlags} --threads 1C --update-snapshots -DskipITs -DskipTests -Dmaven.javadoc.skip=true clean verify"
	}

	return new AppBuildResults(
		dbMigratorZip: 'apps/bfd-db-migrator/target/bfd-db-migrator-1.0.0-SNAPSHOT.zip',
		dataPipelineZip: 'apps/bfd-pipeline/bfd-pipeline-app/target/bfd-pipeline-app-1.0.0-SNAPSHOT.zip',
		dataServerLauncher: 'apps/bfd-server/bfd-server-launcher/target/bfd-server-launcher-1.0.0-SNAPSHOT.zip',
		dataServerWar: 'apps/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war'
	)
}

return this
