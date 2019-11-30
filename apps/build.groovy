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
	if (build_env == 'healthapt') {
		def mvnHome = tool 'maven-3'

		// Run the build, using Maven, with the appropriate config.
		withCredentials([
				string(credentialsId: 'proxy-host', variable: 'proxyHost'),
				string(credentialsId: 'proxy-port', variable: 'proxyPort')
		]) {
		configFileProvider(
				[
					configFile(fileId: 'bluebutton:settings.xml', variable: 'MAVEN_SETTINGS'),
					configFile(fileId: 'bluebutton:toolchains.xml', variable: 'MAVEN_TOOLCHAINS')
				]
		) {
			def proxyArgs = ''
			if (proxyHost?.trim() && proxyPort.trim()) {
				proxyArgs = "-Dhttp.proxyHost=${proxyHost} -Dhttp.proxyPort=${proxyPort} -Dhttps.proxyHost=${proxyHost} -Dhttps.proxyPort=${proxyPort} -Dhttp.nonProxyHosts=localhost"
			}
			sh "${mvnHome}/bin/mvn --settings $MAVEN_SETTINGS --toolchains $MAVEN_TOOLCHAINS ${args} ${proxyArgs}"
		}
	} } else if (build_env == 'ccs') {
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
}

/**
 * Models the results of a call to {@link #build}: contains the paths to the artifacts that were built.
 */
class AppBuildResults implements Serializable {
	String dataPipelineUberJar
	String dataServerLauncher
	String dataServerWar
	String dataServerPlaidApp
}

/**
 * Installs the Rust build toolchain if it's not already present.
 *
 * @throws Exception An exception will be bubbled up if the installation fails.
 */
def installRustToolchain() {
	// FIXME This all is a hacky way of doing this; the toolchain should be added to the Jenkins setup.

	// Check to see if Cargo is already installed.
	isCargoInstalled = sh(script: 'which cargo', returnStatus: true) == 0
	if (isCargoInstalled) {
		echo 'Cargo is already installed.'
		sh 'cargo --version'
		sh 'rustc --version'
	} else {
		// It's not installed, so let's fix that.
		// Reference: <https://www.rust-lang.org/learn/get-started>
		sh "curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y"
		echo 'Installed Rust toolchain.'
		sh 'cargo --version'
		sh 'rustc --version'
	}

	sh 'rpm --quiet --query gcc || sudo yum install -y gcc'
	sh 'rpm --quiet --query gcc || sudo yum install -y openssl-devel'
	sh 'rpm --quiet --query gcc || sudo yum install -y sqlite-devel'
	sh 'rpm --quiet --query gcc || sudo yum install -y postgresql-libs'
	sh 'rpm --quiet --query gcc || sudo yum install -y postgresql-static'
}

/**
 * Builds the Java applications and utilities in this directory: Data Pipeline, Data Server, etc.
 *
 * @return An {@link AppBuildResults} instance containing the paths to the artifacts that were built.
 * @throws Exception An exception will be bubbled up if the Maven build fails.
 */
def build(String build_env) {
	dir ('apps') {
		if (build_env == 'healthapt') {
			mvn "--update-snapshots -Dmaven.test.failure.ignore clean verify -Dhttp.nonProxyHosts=localhost"
		} else if (build_env == 'ccs') {
			// Build the Plaid app.
			installRustToolchain()
			dir ('bfd-server-plaid') {
				sh 'cargo build --release'
			}

			mvn "--update-snapshots -Dmaven.test.failure.ignore clean verify"
		} else {
			UnsupportedOperationException("No Build Apps job available for ${params.dev_env} environment")
		}
	
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
		dataServerWar: 'apps/bfd-server/bfd-server-war/target/bfd-server-war-1.0.0-SNAPSHOT.war',
		dataServerPlaidApp: 'apps/bfd-server-plaid/target/release/bfd-server-plaid-app'
	)
}

return this
